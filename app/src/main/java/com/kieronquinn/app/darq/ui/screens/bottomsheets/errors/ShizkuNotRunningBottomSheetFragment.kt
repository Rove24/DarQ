package com.kieronquinn.app.darq.ui.screens.bottomsheets.errors

import android.app.Dialog
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.model.shizuku.ShizukuConstants
import com.kieronquinn.app.darq.ui.base.BaseBottomSheetDialogFragment

class ShizkuNotRunningBottomSheetFragment: BaseBottomSheetDialogFragment() {

    override val iconRes = R.drawable.ic_developer_options_kill

    override val title by lazy {
        getString(R.string.bottom_sheet_shizuku_not_running_title)
    }

    override val content by lazy {
        getString(R.string.bottom_sheet_shizuku_not_running_content)
    }

    override val positiveText by lazy {
        getString(R.string.bottom_sheet_shizuku_not_running_positive)
    }

    override val negativeText by lazy {
        getString(R.string.bottom_sheet_shizuku_not_running_negative)
    }

    override val cancelable = false

    override fun onPositiveClicked(dialog: Dialog) {
        super.onPositiveClicked(dialog)
        val packageManager = requireContext().packageManager
        startActivity(packageManager.getLaunchIntentForPackage(ShizukuConstants.SHIZUKU_PACKAGE_NAME))
    }

    override fun onNegativeClicked(dialog: Dialog) {
        super.onNegativeClicked(dialog)
        requireActivity().finish()
    }

}