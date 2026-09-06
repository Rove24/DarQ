package com.kieronquinn.app.darq.ui.screens.settings.apppicker

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.databinding.ItemAppBinding
import com.kieronquinn.app.darq.databinding.ItemHeaderBinding
import com.kieronquinn.app.darq.databinding.ItemSnackbarPaddingBinding
import com.kieronquinn.app.darq.model.settings.AppPickerItem
import com.kieronquinn.app.darq.model.settings.AppPickerItemType
import com.kieronquinn.app.darq.utils.AppIconRequestHandler
import com.kieronquinn.app.darq.utils.extensions.applyMD3SwitchMonet
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.squareup.picasso.Picasso

class SettingsAppPickerAdapter(
    private val context: Context,
    private val items: MutableList<AppPickerItem>,
    private val onPackageEnabledChanged: (AppPickerItem.App) -> Unit
): RecyclerView.Adapter<SettingsAppPickerAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private val picasso by lazy {
        Picasso.get()
    }

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return item.itemType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(AppPickerItemType.values().find { it.ordinal == viewType }!!){
            AppPickerItemType.HEADER -> ViewHolder.ItemHeaderViewHolder(ItemHeaderBinding.inflate(layoutInflater, parent, false))
            AppPickerItemType.APP -> ViewHolder.ItemAppViewHolder(ItemAppBinding.inflate(layoutInflater, parent, false))
            AppPickerItemType.SNACKBAR_PADDING -> ViewHolder.ItemSnackbarPaddingViewHolder(ItemSnackbarPaddingBinding.inflate(layoutInflater, parent, false))
        }
    }

    private enum class CardPosition {
        TOP, MIDDLE, BOTTOM, SINGLE
    }

    private fun getCardPosition(position: Int): CardPosition {
        val isPrevSameGroup = position > 0 && items[position - 1] is AppPickerItem.App
        val isNextSameGroup = position < items.size - 1 && items[position + 1] is AppPickerItem.App
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
                lp.bottomMargin = margin4
            }
        }
        root.layoutParams = lp
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[holder.adapterPosition]
        when(holder){
            is ViewHolder.ItemHeaderViewHolder -> (item as? AppPickerItem.Header)?.let {
                holder.binding.itemHeadingTitle.text = it.title
                holder.binding.itemHeadingTitle.setTextColor(ContextCompat.getColor(context, R.color.category_header_text))
            }
            is ViewHolder.ItemAppViewHolder -> setupApp(holder.binding, item as AppPickerItem.App, holder.adapterPosition)
            is ViewHolder.ItemSnackbarPaddingViewHolder -> {}
        }
    }

    private fun setupApp(binding: ItemAppBinding, item: AppPickerItem.App, position: Int){
        val cardPosition = getCardPosition(position)
        applyCardLayout(binding.root, cardPosition)

        with(binding){
            title.text = item.label
            val uri = Uri.parse("${AppIconRequestHandler.SCHEME_PNAME}:${item.packageName}")
            picasso.load(uri).into(icon)
            checkbox.applyMD3SwitchMonet()
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = item.enabled
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                item.enabled = isChecked
                onPackageEnabledChanged.invoke(item)
            }
            root.setOnClickListener {
                checkbox.toggle()
            }
        }
    }

    fun setItems(items: List<AppPickerItem>){
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return when(val item = items[position]){
            is AppPickerItem.Header -> ("Header_" + item.title).hashCode().toLong()
            is AppPickerItem.App -> item.packageName.hashCode().toLong()
            is AppPickerItem.SnackbarPadding -> "SnackbarPadding".hashCode().toLong()
        }
    }

    fun addSnackbarPadding(){
        if(!items.contains(AppPickerItem.SnackbarPadding)){
            items.add(AppPickerItem.SnackbarPadding)
            notifyItemInserted(items.size - 1)
        }
    }

    fun removeSnackbarPadding(){
        if(items.contains(AppPickerItem.SnackbarPadding)){
            items.remove(AppPickerItem.SnackbarPadding)
            notifyItemRemoved(items.size)
        }
    }

    sealed class ViewHolder(open val view: View): RecyclerView.ViewHolder(view) {
        data class ItemHeaderViewHolder(val binding: ItemHeaderBinding): ViewHolder(binding.root)
        data class ItemAppViewHolder(val binding: ItemAppBinding): ViewHolder(binding.root)
        data class ItemSnackbarPaddingViewHolder(val binding: ItemSnackbarPaddingBinding): ViewHolder(binding.root)
    }

}