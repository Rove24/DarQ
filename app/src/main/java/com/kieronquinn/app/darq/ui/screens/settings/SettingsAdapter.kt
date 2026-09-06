package com.kieronquinn.app.darq.ui.screens.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.databinding.*
import com.kieronquinn.app.darq.model.settings.SettingsItem
import com.kieronquinn.app.darq.model.settings.SettingsItemType
import com.kieronquinn.app.darq.utils.Links
import com.kieronquinn.app.darq.utils.openLink
import com.kieronquinn.app.darq.utils.extensions.applyMD3SwitchMonet
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.views.applyMonet

class SettingsAdapter(context: Context, private var items: List<SettingsItem>): RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    private val visibleItems
        get() = items.filter { it.visible.invoke() }

    override fun getItemCount(): Int = visibleItems.size

    override fun getItemViewType(position: Int): Int {
        return visibleItems[position].itemType.ordinal
    }

    override fun getItemId(position: Int): Long {
        return when(val item = visibleItems[position]){
            is SettingsItem.SwitchSetting -> item.title.hashCode().toLong()
            is SettingsItem.Setting -> item.title.hashCode().toLong()
            is SettingsItem.AboutSetting -> item.title.hashCode().toLong()
            is SettingsItem.Header -> item.title.hashCode().toLong()
            is SettingsItem.SnackbarPadding -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(SettingsItemType.values()[viewType]){
            SettingsItemType.HEADER -> ViewHolder.Header(ItemHeaderBinding.inflate(layoutInflater, parent, false))
            SettingsItemType.SETTING -> ViewHolder.SettingsSetting(ItemSettingBinding.inflate(layoutInflater, parent, false))
            SettingsItemType.ABOUT_SETTING -> ViewHolder.SettingsAboutSetting(ItemSettingAboutBinding.inflate(layoutInflater, parent, false))
            SettingsItemType.SWITCH_SETTING -> ViewHolder.SettingsSwitchSetting(ItemSettingSwitchBinding.inflate(layoutInflater, parent, false))
            SettingsItemType.SNACKBAR_PADDING -> ViewHolder.SnackbarPadding(ItemSnackbarPaddingBinding.inflate(layoutInflater, parent, false))
        }
    }

    private enum class CardPosition {
        TOP, MIDDLE, BOTTOM, SINGLE
    }

    private fun getCardPosition(position: Int): CardPosition {
        val isPrevSameGroup = position > 0 &&
                visibleItems[position - 1] !is SettingsItem.Header &&
                visibleItems[position - 1] !is SettingsItem.AboutSetting
        val isNextSameGroup = position < visibleItems.size - 1 &&
                visibleItems[position + 1] !is SettingsItem.Header &&
                visibleItems[position + 1] !is SettingsItem.AboutSetting &&
                visibleItems[position + 1] !is SettingsItem.SnackbarPadding
        return when {
            !isPrevSameGroup && !isNextSameGroup -> CardPosition.SINGLE
            !isPrevSameGroup && isNextSameGroup -> CardPosition.TOP
            isPrevSameGroup && isNextSameGroup -> CardPosition.MIDDLE
            else -> CardPosition.BOTTOM
        }
    }

    private fun applyCardLayout(root: View, cardPosition: CardPosition) {
        val bgRes = when(cardPosition) {
            CardPosition.SINGLE -> R.drawable.bg_group_card_single
            CardPosition.TOP -> R.drawable.bg_group_card_top
            CardPosition.MIDDLE -> R.drawable.bg_group_card_middle
            CardPosition.BOTTOM -> R.drawable.bg_group_card_bottom
        }
        root.background = ContextCompat.getDrawable(root.context, bgRes)

        val lp = root.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val margin16 = root.context.resources.getDimensionPixelSize(R.dimen.padding_16)
        val margin4 = (4 * root.context.resources.displayMetrics.density).toInt()
        val margin8 = (8 * root.context.resources.displayMetrics.density).toInt()
        lp.marginStart = margin16
        lp.marginEnd = margin16
        when(cardPosition) {
            CardPosition.SINGLE -> {
                lp.topMargin = margin4
                lp.bottomMargin = margin4
            }
            CardPosition.TOP -> {
                lp.topMargin = margin4
                lp.bottomMargin = 0
            }
            CardPosition.MIDDLE -> {
                lp.topMargin = 0
                lp.bottomMargin = 0
            }
            CardPosition.BOTTOM -> {
                lp.topMargin = 0
                lp.bottomMargin = margin8
            }
        }
        root.layoutParams = lp
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = visibleItems[holder.adapterPosition]
        when(holder){
            is ViewHolder.Header -> setupHeader(holder.binding, item as SettingsItem.Header)
            is ViewHolder.SettingsSetting -> setupSetting(holder.binding, item as SettingsItem.Setting, holder.adapterPosition)
            is ViewHolder.SettingsAboutSetting -> setupTripleTapActionSetting(holder.binding, item as SettingsItem.AboutSetting)
            is ViewHolder.SettingsSwitchSetting -> setupSettingSwitch(holder.binding, item as SettingsItem.SwitchSetting, holder.adapterPosition)
            is ViewHolder.SnackbarPadding -> {}
        }
    }

    private fun setupHeader(binding: ItemHeaderBinding, item: SettingsItem.Header) = with(binding) {
        itemHeadingTitle.text = item.title
        itemHeadingTitle.setTextColor(ContextCompat.getColor(root.context, R.color.category_header_text))
    }

    private fun setupSetting(binding: ItemSettingBinding, item: SettingsItem.Setting, position: Int) = with(binding) {
        val cardPosition = getCardPosition(position)
        applyCardLayout(root, cardPosition)

        itemSettingTitle.text = item.title
        if(item.content.isNullOrEmpty()){
            itemSettingContent.isVisible = false
        }else{
            itemSettingContent.isVisible = true
            itemSettingContent.text = item.content
        }
        if(item.icon != 0) {
            itemSettingIcon.setImageResource(item.icon)
        }else{
            itemSettingIcon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        item.tapAction?.let { action ->
            root.setOnClickListener {
                action.invoke()
            }
        }
        if(!item.centerIconVertically){
            binding.root.gravity = Gravity.NO_GRAVITY
        }
    }

    private var lastAboutTapTime = 0L
    private var aboutTapCount = 0
    private var aboutToast: Toast? = null

    private fun setupTripleTapActionSetting(binding: ItemSettingAboutBinding, item: SettingsItem.AboutSetting) = with(binding) {
        itemSettingTitle.text = item.title
        if(item.content.isNullOrEmpty()){
            itemSettingContent.isVisible = false
        }else{
            itemSettingContent.isVisible = true
            itemSettingContent.text = item.content
        }
        if(item.icon != 0) {
            itemSettingIcon.setImageResource(item.icon)
        }else{
            itemSettingIcon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        root.setOnClickListener {
            // Spring press feedback animation
            root.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction {
                root.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
            }.start()

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAboutTapTime > 2000L) {
                aboutTapCount = 0
            }
            lastAboutTapTime = currentTime
            aboutTapCount++

            val devOptionsItem = items.firstOrNull { it is SettingsItem.Setting && it.icon == R.drawable.ic_developer_options_round }
            val isDevOptionsVisible = devOptionsItem?.visible?.invoke() ?: false

            val remaining = 3 - aboutTapCount
            aboutToast?.cancel()
            if (aboutTapCount < 3) {
                val message = if (!isDevOptionsVisible) {
                    root.context.getString(R.string.toast_developer_options_steps_enable, remaining)
                } else {
                    root.context.getString(R.string.toast_developer_options_steps_disable, remaining)
                }
                aboutToast = Toast.makeText(root.context, message, Toast.LENGTH_SHORT).apply { show() }
            } else {
                aboutTapCount = 0
                val message = if (!isDevOptionsVisible) {
                    root.context.getString(R.string.toast_developer_options_enabled)
                } else {
                    root.context.getString(R.string.toast_developer_options_disabled)
                }
                aboutToast = Toast.makeText(root.context, message, Toast.LENGTH_SHORT).apply { show() }
                item.tripleTapAction?.invoke()
            }
        }

        chipGithub.setOnClickListener {
            it.context.openLink(Links.LINK_GITHUB)
        }
    }

    private fun setupSettingSwitch(binding: ItemSettingSwitchBinding, item: SettingsItem.SwitchSetting, position: Int) = with(binding) {
        val cardPosition = getCardPosition(position)
        applyCardLayout(root, cardPosition)

        itemSettingSwitchTitle.text = item.title
        itemSettingSwitchSwitch.applyMD3SwitchMonet()
        if(item.content.isNullOrEmpty()){
            itemSettingSwitchContent.isVisible = false
        }else{
            itemSettingSwitchContent.isVisible = true
            itemSettingSwitchContent.text = item.content
        }
        if(item.icon != 0) {
            itemSettingSwitchIcon.setImageResource(item.icon)
        }else{
            itemSettingSwitchIcon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        itemSettingSwitchSwitch.setOnCheckedChangeListener(null)
        itemSettingSwitchSwitch.isChecked = item.setting.get()
        if (item.rowClickAction != null) {
            itemSettingSwitchSwitch.isClickable = true
            itemSettingSwitchSwitch.isFocusable = true
            itemSettingSwitchSwitch.setOnCheckedChangeListener { button, isChecked ->
                if(item.tapAction != null){
                    if(item.tapAction.invoke(isChecked)){
                        item.setting.set(isChecked)
                    }else{
                        button.isChecked = !isChecked
                    }
                }else {
                    item.setting.set(isChecked)
                }
            }
            root.setOnClickListener {
                item.rowClickAction.invoke()
            }
        } else {
            itemSettingSwitchSwitch.isClickable = false
            itemSettingSwitchSwitch.isFocusable = false
            itemSettingSwitchSwitch.setOnCheckedChangeListener { button, isChecked ->
                if(item.tapAction != null){
                    if(item.tapAction.invoke(isChecked)){
                        item.setting.set(isChecked)
                    }else{
                        button.isChecked = !isChecked
                    }
                }else {
                    item.setting.set(isChecked)
                }
            }
            root.setOnClickListener {
                itemSettingSwitchSwitch.toggle()
            }
        }
        if(!item.centerIconVertically){
            binding.root.gravity = Gravity.NO_GRAVITY
        }
    }

    fun notifySwitchSettings(){
        items.forEachIndexed { index, settingsItem ->
            if(settingsItem is SettingsItem.SwitchSetting) {
                notifyItemChanged(index)
            }
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemHeaderBinding): ViewHolder(binding)
        data class SettingsSetting(override val binding: ItemSettingBinding): ViewHolder(binding)
        data class SettingsAboutSetting(override val binding: ItemSettingAboutBinding): ViewHolder(binding)
        data class SettingsSwitchSetting(override val binding: ItemSettingSwitchBinding): ViewHolder(binding)
        data class SnackbarPadding(override val binding: ItemSnackbarPaddingBinding): ViewHolder(binding)
    }

}