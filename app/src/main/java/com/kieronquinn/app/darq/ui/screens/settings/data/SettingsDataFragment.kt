package com.kieronquinn.app.darq.ui.screens.settings.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.components.navigation.Navigation
import com.kieronquinn.app.darq.components.settings.DarqSharedPreferences
import com.kieronquinn.app.darq.databinding.FragmentSettingsDataBinding
import com.kieronquinn.app.darq.model.settings.SettingsBackup
import com.kieronquinn.app.darq.service.autodark.DarqAutoDarkForegroundService
import com.kieronquinn.app.darq.ui.base.AutoExpandOnRotate
import com.kieronquinn.app.darq.ui.base.BackAvailable
import com.kieronquinn.app.darq.ui.base.BoundFragment
import com.kieronquinn.app.darq.utils.extensions.gzip
import com.kieronquinn.app.darq.utils.extensions.ungzip
import com.kieronquinn.app.darq.ui.screens.container.ContainerSharedViewModel
import com.kieronquinn.app.darq.utils.extensions.navGraphViewModel
import com.kieronquinn.monetcompat.extensions.views.applyMonetRecursively
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsDataFragment :
    BoundFragment<FragmentSettingsDataBinding>(FragmentSettingsDataBinding::inflate),
    AutoExpandOnRotate, BackAvailable {

    private val settings by inject<DarqSharedPreferences>()
    private val navigation by inject<Navigation>()
    private val sharedViewModel by navGraphViewModel<ContainerSharedViewModel>(R.id.nav_graph_main)

    private val backupSelection = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri != null) {
            performBackup(uri)
        }
    }

    private val restoreSelection = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            performRestore(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.applyMonetRecursively()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupActions()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.container.updatePadding(bottom = navInsets.bottom + (24 * resources.displayMetrics.density).toInt())
            insets
        }
    }

    private fun setupActions() = with(binding) {
        cardBackup.setOnClickListener {
            val filename = String.format(
                "darq-config-%s.darqbkp",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
            )
            backupSelection.launch(filename)
        }

        cardRestore.setOnClickListener {
            restoreSelection.launch(arrayOf("*/*"))
        }

        cardClear.setOnClickListener {
            showClearConfirmationDialog()
        }
    }

    private fun performBackup(uri: Uri) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val json = Gson().toJson(settings.getSettingsBackup())
                    val file = DocumentFile.fromSingleUri(requireContext(), uri) ?: return@withContext false
                    if (!file.canWrite()) return@withContext false
                    val outputStream = requireContext().contentResolver.openOutputStream(uri) ?: return@withContext false
                    outputStream.use {
                        it.write(json.gzip())
                        it.flush()
                    }
                    true
                }.getOrDefault(false)
            }
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    if (success) R.string.item_backup_restore_backup_success
                    else R.string.item_backup_restore_backup_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun performRestore(uri: Uri) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val input = requireContext().contentResolver.openInputStream(uri) ?: return@withContext false
                    input.use {
                        val unGzipped = it.readBytes().ungzip()
                        val backup = Gson().fromJson(unGzipped, SettingsBackup::class.java)
                        settings.fromSettingsBackup(backup)
                        true
                    }
                }.getOrDefault(false)
            }
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    if (success) R.string.item_backup_restore_restore_success
                    else R.string.item_backup_restore_restore_failed,
                    Toast.LENGTH_SHORT
                ).show()
                if (success) {
                    sharedViewModel.onRestoreSuccess()
                    rescheduleService()
                }
            }
        }
    }

    private fun showClearConfirmationDialog() {
        val dialog = android.app.Dialog(requireContext())
        val dialogBinding = com.kieronquinn.app.darq.databinding.DialogMd3ConfirmBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            clearAllData()
        }
        dialog.show()
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                settings.sharedPreferences.edit().clear().apply()
                requireContext().getSharedPreferences("darq_ui_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            }
            if (isAdded) {
                Toast.makeText(requireContext(), R.string.data_settings_clear_toast_success, Toast.LENGTH_SHORT).show()
                sharedViewModel.onRestoreSuccess()
                rescheduleService()
                navigation.navigateBack()
            }
        }
    }

    private fun rescheduleService() {
        runCatching {
            requireContext().startForegroundService(
                Intent(requireContext(), DarqAutoDarkForegroundService::class.java).apply {
                    putExtra(DarqAutoDarkForegroundService.KEY_JUST_RESCHEDULE, true)
                }
            )
        }
    }

}