package com.kieronquinn.app.darq.ui.screens.bottomsheets.errors

import android.app.Dialog
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.ui.base.BaseBottomSheetDialogFragment

class ServiceTimeoutBottomSheetFragment: BaseBottomSheetDialogFragment() {

    override val iconRes = R.drawable.ic_developer_options_service_info

    override val title by lazy {
        getString(R.string.bottom_sheet_service_timeout_title)
    }

    override val content by lazy {
        getString(R.string.bottom_sheet_service_timeout_content)
    }

    override val positiveText by lazy {
        getString(R.string.bottom_sheet_service_timeout_positive)
    }

    override val cancelable = false

    override fun onPositiveClicked(dialog: Dialog) {
        super.onPositiveClicked(dialog)
        requireActivity().finish()
    }

}