package com.kieronquinn.app.darq.ui.screens.settings.developer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.databinding.FragmentSettingsDeveloperOptionsBinding
import com.kieronquinn.app.darq.ui.base.AutoExpandOnRotate
import com.kieronquinn.app.darq.ui.base.BackAvailable
import com.kieronquinn.app.darq.ui.base.BoundFragment
import com.kieronquinn.app.darq.ui.screens.container.ContainerSharedViewModel
import com.kieronquinn.app.darq.ui.utils.TransitionUtils
import com.kieronquinn.app.darq.utils.extensions.navGraphViewModel
import com.kieronquinn.monetcompat.extensions.views.applyMonetRecursively
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsDeveloperOptionsFragment :
    BoundFragment<FragmentSettingsDeveloperOptionsBinding>(FragmentSettingsDeveloperOptionsBinding::inflate),
    BackAvailable,
    AutoExpandOnRotate {

    private val viewModel by viewModel<SettingsDeveloperOptionsViewModel>()
    private val sharedViewModel by navGraphViewModel<ContainerSharedViewModel>(R.id.nav_graph_main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        enterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        returnTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
        reenterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.applyMonetRecursively()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupServiceOperations()
        loadServiceState()
    }

    private fun setupInsets() = with(binding) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bottomInset)
            insets
        }
    }

    private fun setupServiceOperations() = with(binding) {
        cardKillService.setOnClickListener {
            onKillOtherInstancesClicked()
        }
    }

    private fun loadServiceState() {
        lifecycleScope.launchWhenResumed {
            viewModel.getServiceInfo(sharedViewModel).collect {
                binding.textServiceInfoSubtitle.text = if (it.second == null) {
                    getString(it.first)
                } else {
                    getString(it.first, it.second)
                }
            }
        }
    }

    private fun onKillOtherInstancesClicked() {
        lifecycleScope.launchWhenResumed {
            if (sharedViewModel.killOtherInstances()) {
                Toast.makeText(requireContext(), R.string.item_developer_options_kill_service_toast_success, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), R.string.item_developer_options_kill_service_toast_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

}