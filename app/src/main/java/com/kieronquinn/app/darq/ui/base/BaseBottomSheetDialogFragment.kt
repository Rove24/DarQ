package com.kieronquinn.app.darq.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.view.*
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import com.kieronquinn.app.darq.components.navigation.Navigation
import com.kieronquinn.app.darq.databinding.FragmentBottomSheetBinding
import com.kieronquinn.app.darq.providers.blur.BlurProvider
import com.kieronquinn.app.darq.ui.utils.autoCleared
import com.kieronquinn.monetcompat.core.MonetCompat
import org.koin.android.ext.android.inject

abstract class BaseBottomSheetDialogFragment : AppCompatDialogFragment() {

    internal val navigation by inject<Navigation>()
    private val blurProvider by inject<BlurProvider>()

    internal val monet by lazy {
        MonetCompat.getInstance()
    }

    internal var binding by autoCleared<FragmentBottomSheetBinding>()

    abstract val title: CharSequence
    abstract val content: CharSequence

    open val iconRes: Int? = null

    open val positiveText: CharSequence? = null
    open val negativeText: CharSequence? = null
    open val neutralText: CharSequence? = null

    open val cancelable: Boolean = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        isCancelable = cancelable
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
            activity?.window?.let { appWin ->
                blurProvider.applyDialogBlur(this, appWin, 1f)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val dialogWin = this.dialog?.window
        val appWin = activity?.window
        if (dialogWin != null && appWin != null) {
            blurProvider.applyDialogBlur(dialogWin, appWin, 0f)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bottomSheetTitle.text = title

        if (content is String && (content.contains("<") && content.contains(">"))) {
            binding.bottomSheetContent.text = Html.fromHtml(content.toString(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            binding.bottomSheetContent.text = content
        }

        if (iconRes != null) {
            binding.dialogIcon.setImageResource(iconRes!!)
            binding.dialogIconContainer.isVisible = true
        } else {
            binding.dialogIconContainer.isVisible = false
        }

        val d = dialog ?: return

        if (positiveText != null) {
            binding.bottomSheetPositive.isVisible = true
            binding.bottomSheetPositive.text = positiveText
            binding.bottomSheetPositive.setOnClickListener {
                onPositiveClicked(d)
            }
        } else {
            binding.bottomSheetPositive.isVisible = false
        }

        if (negativeText != null) {
            binding.bottomSheetNegative.isVisible = true
            binding.bottomSheetNegative.text = negativeText
            binding.bottomSheetNegative.setOnClickListener {
                onNegativeClicked(d)
            }
        } else {
            binding.bottomSheetNegative.isVisible = false
        }

        if (neutralText != null) {
            binding.bottomSheetNeutral.isVisible = true
            binding.bottomSheetNeutral.text = neutralText
            binding.bottomSheetNeutral.setOnClickListener {
                onNeutralClicked(d)
            }
        } else {
            binding.bottomSheetNeutral.isVisible = false
        }
    }

    open fun onPositiveClicked(dialog: Dialog) {
        dialog.dismiss()
    }

    open fun onNegativeClicked(dialog: Dialog) {
        dialog.dismiss()
    }

    open fun onNeutralClicked(dialog: Dialog) {
        dialog.dismiss()
    }

}