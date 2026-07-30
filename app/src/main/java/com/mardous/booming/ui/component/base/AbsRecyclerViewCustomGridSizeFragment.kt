/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.component.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.R
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.extensions.isLandscape

import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.mardous.booming.ui.dialogs.LayoutViewBottomSheetDialogFragment

abstract class AbsRecyclerViewCustomGridSizeFragment<Adt : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsRecyclerViewFragment<Adt, LM>() {

    protected var gridSize: Int
        get() = getSavedGridSize()
        set(newGridSize) {
            val oldLayoutRes = itemLayoutRes
            saveGridSize(newGridSize)
            if (oldLayoutRes != itemLayoutRes) {
                invalidateLayoutManager()
                invalidateAdapter()
            } else {
                onGridSizeChanged(isLandscape, newGridSize)
            }
        }

    protected var viewType: GridViewType
        get() = getSavedViewType()
        set(newViewType) {
            saveViewType(newViewType)
            invalidateAdapter()
        }

    protected val isGridMode: Boolean
        get() = gridSize > maxGridSizeForList

    protected open val maxGridSize: Int
        get() = if (isLandscape) {
            resources.getInteger(R.integer.max_columns_land)
        } else resources.getInteger(R.integer.max_columns)

    protected open val maxGridSizeForList: Int
        get() = if (isLandscape) {
            resources.getInteger(R.integer.default_list_columns_land)
        } else resources.getInteger(R.integer.default_list_columns)

    protected open val defaultGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.default_grid_columns_land)
        else resources.getInteger(R.integer.default_grid_columns)

    private var currentLayoutRes = 0

    @get:LayoutRes
    protected open val itemLayoutRes: Int
        get() = if (isGridMode) {
            viewType.layoutRes
        } else R.layout.item_list

    protected val isLandscape: Boolean
        get() = resources.isLandscape

    protected fun notifyLayoutResChanged(@LayoutRes res: Int) {
        currentLayoutRes = res
        applyRecyclerViewPaddingForLayoutRes(recyclerView, currentLayoutRes)
    }

    private fun applyRecyclerViewPaddingForLayoutRes(recyclerView: RecyclerView, @LayoutRes itemLayoutRes: Int) {
        val miniPlayerHeight = libraryViewModel.getMiniPlayerMargin().value?.totalMargin ?: 0
        val padding = GridViewType.getMarginForLayout(itemLayoutRes)
        recyclerView.setPadding(padding, padding, padding, padding + miniPlayerHeight)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyRecyclerViewPaddingForLayoutRes(recyclerView, currentLayoutRes)
    }

    override fun showViewTypeInBottomSheet(): Boolean = true
    override fun selectedViewType(): GridViewType = viewType
    override fun onViewTypeChanged(viewType: GridViewType) {
        this.viewType = viewType
    }

    protected open val showLayoutButtonInSubHeader: Boolean = true

    override fun setupSubHeader() {
        super.setupSubHeader()
        val layoutBtn = _binding?.appBarLayout?.findViewById<MaterialButton>(R.id.header_layout_button)
        if (showLayoutButtonInSubHeader) {
            layoutBtn?.isVisible = true
            layoutBtn?.setOnClickListener {
                val dialog = LayoutViewBottomSheetDialogFragment().apply {
                    this.currentGridSize = this@AbsRecyclerViewCustomGridSizeFragment.gridSize
                    this.maxGridSize = this@AbsRecyclerViewCustomGridSizeFragment.maxGridSize
                    this.onGridSizeSelected = { newGrid ->
                        this@AbsRecyclerViewCustomGridSizeFragment.gridSize = newGrid
                    }
                    this.showViewType = this@AbsRecyclerViewCustomGridSizeFragment.showViewTypeInBottomSheet()
                    this.selectedViewType = this@AbsRecyclerViewCustomGridSizeFragment.selectedViewType()
                    this.onViewTypeChanged = { viewType ->
                        this@AbsRecyclerViewCustomGridSizeFragment.onViewTypeChanged(viewType)
                    }
                    this.showOnlyAlbumArtists = this@AbsRecyclerViewCustomGridSizeFragment.showOnlyAlbumArtistsInBottomSheet()
                    this.isOnlyAlbumArtists = this@AbsRecyclerViewCustomGridSizeFragment.isOnlyAlbumArtists()
                    this.onOnlyAlbumArtistsChanged = { enabled ->
                        this@AbsRecyclerViewCustomGridSizeFragment.onOnlyAlbumArtistsChanged(enabled)
                    }
                }
                dialog.show(childFragmentManager, "LAYOUT_VIEW_BOTTOM_SHEET")
            }
        } else {
            layoutBtn?.isVisible = false
        }
    }

    protected abstract fun getSavedViewType(): GridViewType
    protected abstract fun saveViewType(viewType: GridViewType)
    protected abstract fun getSavedGridSize(): Int
    protected abstract fun saveGridSize(newGridSize: Int)
    protected abstract fun onGridSizeChanged(isLand: Boolean, gridColumns: Int)
}