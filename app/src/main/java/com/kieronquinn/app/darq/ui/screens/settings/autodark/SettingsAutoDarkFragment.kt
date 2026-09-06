package com.kieronquinn.app.darq.ui.screens.settings.autodark

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.components.settings.DarqSharedPreferences
import com.kieronquinn.app.darq.databinding.FragmentSettingsAutodarkBinding
import com.kieronquinn.app.darq.service.autodark.DarqAutoDarkForegroundService
import com.kieronquinn.app.darq.ui.base.AutoExpandOnRotate
import com.kieronquinn.app.darq.ui.base.BackAvailable
import com.kieronquinn.app.darq.ui.base.BoundFragment
import com.kieronquinn.app.darq.ui.screens.container.ContainerSharedViewModel
import com.kieronquinn.app.darq.utils.extensions.applyMD3SwitchMonet
import com.kieronquinn.app.darq.utils.extensions.navGraphViewModel
import com.kieronquinn.monetcompat.extensions.views.applyMonetRecursively
import org.koin.android.ext.android.inject
import java.util.Locale

class SettingsAutoDarkFragment :
    BoundFragment<FragmentSettingsAutodarkBinding>(FragmentSettingsAutodarkBinding::inflate),
    AutoExpandOnRotate, BackAvailable {

    private val settings by inject<DarqSharedPreferences>()
    private val sharedViewModel by navGraphViewModel<ContainerSharedViewModel>(R.id.nav_graph_main)

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
        setupMasterSwitch()
        setupScheduleOptions()
        setupTimePickers()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.container.updatePadding(bottom = navInsets.bottom + (24 * resources.displayMetrics.density).toInt())
            insets
        }
    }

    private fun setupMasterSwitch() = with(binding) {
        switchMaster.applyMD3SwitchMonet()
        updateMasterSwitchUi(settings.autoDarkTheme)

        cardMasterSwitch.setOnClickListener {
            switchMaster.toggle()
        }

        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            if (settings.autoDarkTheme != isChecked) {
                sharedViewModel.setAutoDarkThemeEnabled(isChecked)
            }
            updateMasterSwitchUi(isChecked)
        }

        lifecycleScope.launchWhenResumed {
            sharedViewModel.autoDarkTheme.collect { enabled ->
                if (switchMaster.isChecked != enabled) {
                    switchMaster.isChecked = enabled
                }
                updateMasterSwitchUi(enabled)
            }
        }
    }

    private fun updateMasterSwitchUi(enabled: Boolean) = with(binding) {
        textMasterSubtitle.text = getString(
            if (enabled) R.string.auto_dark_status_enabled
            else R.string.auto_dark_status_disabled
        )
        val alpha = if (enabled) 1.0f else 0.5f
        layoutScheduleGroup.alpha = alpha
    }

    private fun setupScheduleOptions() = with(binding) {
        switchSunsetSunrise.applyMD3SwitchMonet()
        switchCustomTime.applyMD3SwitchMonet()

        updateScheduleSelection(settings.autoDarkScheduleType)

        val selectSunsetSunrise = {
            if (settings.autoDarkScheduleType != DarqSharedPreferences.SCHEDULE_TYPE_SUNSET_SUNRISE) {
                settings.autoDarkScheduleType = DarqSharedPreferences.SCHEDULE_TYPE_SUNSET_SUNRISE
                updateScheduleSelection(DarqSharedPreferences.SCHEDULE_TYPE_SUNSET_SUNRISE)
                if (!settings.autoDarkTheme) {
                    sharedViewModel.setAutoDarkThemeEnabled(true)
                } else {
                    rescheduleService()
                }
            }
        }

        val selectCustomTime = {
            if (settings.autoDarkScheduleType != DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM) {
                settings.autoDarkScheduleType = DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM
                updateScheduleSelection(DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM)
                if (!settings.autoDarkTheme) {
                    sharedViewModel.setAutoDarkThemeEnabled(true)
                } else {
                    rescheduleService()
                }
            }
        }

        cardSunsetSunrise.setOnClickListener { selectSunsetSunrise() }
        switchSunsetSunrise.setOnClickListener { selectSunsetSunrise() }

        cardCustomTime.setOnClickListener { selectCustomTime() }
        switchCustomTime.setOnClickListener { selectCustomTime() }
    }

    private fun updateScheduleSelection(type: Int) = with(binding) {
        val isSunset = type == DarqSharedPreferences.SCHEDULE_TYPE_SUNSET_SUNRISE
        switchSunsetSunrise.isChecked = isSunset
        switchCustomTime.isChecked = !isSunset

        layoutCustomTimes.isVisible = !isSunset
        cardCustomTime.background = ContextCompat.getDrawable(
            requireContext(),
            if (isSunset) R.drawable.bg_group_card_bottom else R.drawable.bg_group_card_middle
        )
    }

    private fun setupTimePickers() = with(binding) {
        updateTimeTexts()

        cardStartTime.setOnClickListener {
            showTimePicker(
                initialMinutes = settings.autoDarkCustomStart
            ) { newMinutes ->
                settings.autoDarkCustomStart = newMinutes
                updateTimeTexts()
                if (settings.autoDarkTheme && settings.autoDarkScheduleType == DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM) {
                    rescheduleService()
                }
            }
        }

        cardEndTime.setOnClickListener {
            showTimePicker(
                initialMinutes = settings.autoDarkCustomEnd
            ) { newMinutes ->
                settings.autoDarkCustomEnd = newMinutes
                updateTimeTexts()
                if (settings.autoDarkTheme && settings.autoDarkScheduleType == DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM) {
                    rescheduleService()
                }
            }
        }
    }

    private fun updateTimeTexts() = with(binding) {
        textStartTimeValue.text = formatMinutes(settings.autoDarkCustomStart)
        textEndTimeValue.text = formatMinutes(settings.autoDarkCustomEnd)
    }

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
    }

    private fun showTimePicker(
        initialMinutes: Int,
        onTimeSelected: (Int) -> Unit
    ) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setHour(initialMinutes / 60)
            .setMinute(initialMinutes % 60)
            .setTheme(R.style.ThemeOverlay_Darq_TimePicker)
            .setTitleText(R.string.time_picker_dialog_title)
            .setPositiveButtonText(R.string.confirm)
            .setNegativeButtonText(R.string.cancel)
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedMinutes = picker.hour * 60 + picker.minute
            onTimeSelected(selectedMinutes)
        }

        picker.show(parentFragmentManager, "time_picker")
    }

    private fun rescheduleService() {
        requireContext().startForegroundService(
            Intent(requireContext(), DarqAutoDarkForegroundService::class.java).apply {
                putExtra(DarqAutoDarkForegroundService.KEY_JUST_RESCHEDULE, true)
            }
        )
    }

}
