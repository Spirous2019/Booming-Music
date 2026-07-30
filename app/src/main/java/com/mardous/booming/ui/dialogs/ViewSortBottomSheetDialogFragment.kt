package com.mardous.booming.ui.dialogs

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.mardous.booming.R
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.core.model.sort.KeySortItem
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.extensions.resources.primaryColor
import com.mardous.booming.extensions.resolveColor

class ViewSortBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var sortMode: SortMode? = null
    var onSortChanged: (() -> Unit)? = null

    var showViewType: Boolean = false
    var selectedViewType: GridViewType? = null
    var onViewTypeChanged: ((GridViewType) -> Unit)? = null

    var showOnlyAlbumArtists: Boolean = false
    var isOnlyAlbumArtists: Boolean = false
    var onOnlyAlbumArtistsChanged: ((Boolean) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.let { bsd ->
            bsd.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            bsd.behavior.skipCollapsed = true
            bsd.behavior.isFitToContents = true
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_view_sort_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as? androidx.core.widget.NestedScrollView)?.isNestedScrollingEnabled = true

        val sortContainer = view.findViewById<LinearLayout>(R.id.sort_options_container)
        val dividerViewType = view.findViewById<View>(R.id.divider_view_type)
        val titleViewType = view.findViewById<TextView>(R.id.title_view_type)
        val viewTypeContainer = view.findViewById<LinearLayout>(R.id.view_type_options_container)
        val dividerArtistsToggle = view.findViewById<View>(R.id.divider_artists_toggle)
        val switchOnlyAlbumArtists = view.findViewById<MaterialSwitch>(R.id.switch_only_album_artists)

        setupSortOptions(sortContainer)

        if (showViewType) {
            dividerViewType.isVisible = true
            titleViewType.isVisible = true
            viewTypeContainer.isVisible = true
            setupViewTypeOptions(viewTypeContainer)
        }

        if (showOnlyAlbumArtists) {
            dividerArtistsToggle.isVisible = true
            switchOnlyAlbumArtists.isVisible = true
            switchOnlyAlbumArtists.isChecked = isOnlyAlbumArtists
            switchOnlyAlbumArtists.setOnCheckedChangeListener { _, isChecked ->
                onOnlyAlbumArtistsChanged?.invoke(isChecked)
            }
        }
    }

    private fun setupSortOptions(container: LinearLayout) {
        val mode = sortMode ?: return
        val items = mode::class.java.declaredFields
            .mapNotNull {
                try {
                    it.isAccessible = true
                    it.get(mode) as? KeySortItem
                } catch (e: Exception) {
                    null
                }
            }

        val keyItems = getSortKeyItems(mode)

        keyItems.forEach { item ->
            val isSelected = item.key == mode.selectedKey
            val isDescending = mode.selectedDescending

            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_sort_option, container, false)

            val text = itemView.findViewById<TextView>(R.id.sort_title)
            val icon = itemView.findViewById<ImageView>(R.id.sort_arrow_icon)
            val check = itemView.findViewById<ImageView>(R.id.sort_check_icon)

            text.setText(item.title)

            if (isSelected) {
                val primary = requireContext().primaryColor()
                text.setTextColor(primary)
                check.isVisible = true
                check.imageTintList = ColorStateList.valueOf(primary)
                icon.isVisible = true
                icon.setImageResource(if (isDescending) R.drawable.ic_keyboard_arrow_down_24dp else R.drawable.ic_keyboard_arrow_up_24dp)
                icon.imageTintList = ColorStateList.valueOf(primary)
            } else {
                text.setTextColor(requireContext().resolveColor(android.R.attr.textColorPrimary))
                check.isVisible = false
                icon.isVisible = false
            }

            itemView.setOnClickListener {
                if (isSelected) {
                    mode.selectedDescending = !mode.selectedDescending
                } else {
                    mode.selectedKey = item.key
                }
                onSortChanged?.invoke()
                dismiss()
            }

            container.addView(itemView)
        }
    }

    private fun getSortKeyItems(mode: SortMode): List<KeySortItem> {
        val itemsField = SortMode::class.java.getDeclaredField("items")
        itemsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val items = itemsField.get(mode) as? List<*> ?: emptyList<Any>()
        return items.filterIsInstance<KeySortItem>()
    }

    private fun setupViewTypeOptions(container: LinearLayout) {
        val current = selectedViewType ?: GridViewType.Normal
        GridViewType.entries.forEach { viewType ->
            val isSelected = viewType == current
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_sort_option, container, false)

            val text = itemView.findViewById<TextView>(R.id.sort_title)
            val icon = itemView.findViewById<ImageView>(R.id.sort_arrow_icon)
            val check = itemView.findViewById<ImageView>(R.id.sort_check_icon)

            text.setText(viewType.titleRes)
            icon.isVisible = false

            if (isSelected) {
                val primary = requireContext().primaryColor()
                text.setTextColor(primary)
                check.isVisible = true
                check.imageTintList = ColorStateList.valueOf(primary)
            } else {
                text.setTextColor(requireContext().resolveColor(android.R.attr.textColorPrimary))
                check.isVisible = false
            }

            itemView.setOnClickListener {
                onViewTypeChanged?.invoke(viewType)
                dismiss()
            }

            container.addView(itemView)
        }
    }
}
