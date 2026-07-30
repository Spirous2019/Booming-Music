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

package com.mardous.booming.ui.screen.library.playlists

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.view.isVisible
import com.mardous.booming.R
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.core.sort.PlaylistSortMode
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.extensions.navigation.playlistDetailArgs
import com.mardous.booming.ui.IPlaylistCallback
import com.mardous.booming.ui.adapters.PlaylistAdapter
import com.mardous.booming.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.booming.ui.component.menu.onPlaylistMenu
import com.mardous.booming.ui.component.menu.onPlaylistsMenu
import com.mardous.booming.ui.dialogs.playlists.CreatePlaylistDialog
import com.mardous.booming.ui.dialogs.playlists.ImportPlaylistDialog
import com.mardous.booming.ui.screen.library.ReloadType

/**
 * @author Christians M. A. (mardous)
 */
class PlaylistListFragment : AbsRecyclerViewCustomGridSizeFragment<PlaylistAdapter, GridLayoutManager>(),
    IPlaylistCallback {

    override val titleRes: Int = R.string.playlists_label
    override val isShuffleVisible: Boolean = false
    override val emptyMessageRes: Int
        get() = R.string.no_device_playlists

    override val maxGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.max_playlist_columns_land)
        else resources.getInteger(R.integer.max_playlist_columns)

    override val itemLayoutRes: Int
        get() = if (isGridMode) R.layout.item_playlist
        else R.layout.item_list

    override fun getSortMode(): SortMode = PlaylistSortMode.AllPlaylists

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val actionBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.header_action_button)
        actionBtn?.isVisible = true
        actionBtn?.setIconResource(R.drawable.ic_add_24dp)
        actionBtn?.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), actionBtn)
            popup.menu.add(0, 1, 0, R.string.new_playlist_title)
            popup.menu.add(0, 2, 1, R.string.action_import_playlist)

            com.mardous.booming.ui.component.menu.MenuBottomSheetDialogFragment()
                .setMenu(popup.menu) { itemId ->
                    when (itemId) {
                        1 -> CreatePlaylistDialog().show(childFragmentManager, "NEW_PLAYLIST")
                        2 -> ImportPlaylistDialog().show(childFragmentManager, "IMPORT_PLAYLIST")
                    }
                }
                .show(childFragmentManager, com.mardous.booming.ui.component.menu.MenuBottomSheetDialogFragment.TAG)
        }

        libraryViewModel.getPlaylists().observe(viewLifecycleOwner) { playlists ->
            val sortedPlaylists = with(PlaylistSortMode.AllPlaylists) { playlists.sorted() }
            adapter?.dataSet = sortedPlaylists
            setSubHeaderItemCount(sortedPlaylists.size, R.string.playlist_label, R.string.playlists_label)
        }
    }

    override fun onSortModeChanged() {
        libraryViewModel.getPlaylists().value?.let { playlists ->
            val sortedPlaylists = with(PlaylistSortMode.AllPlaylists) { playlists.sorted() }
            adapter?.dataSet = sortedPlaylists
            setSubHeaderItemCount(sortedPlaylists.size, R.string.playlist_label, R.string.playlists_label)
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireContext(), gridSize)
    }

    override fun createAdapter(): PlaylistAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.dataSet ?: ArrayList()
        return PlaylistAdapter(mainActivity, dataSet, itemLayoutRes, this)
    }

    override fun playlistClick(playlist: PlaylistWithSongs) {
        findNavController().navigate(R.id.nav_playlist_detail, playlistDetailArgs(playlist.playlistEntity.playListId))
    }

    override fun playlistMenuItemClick(playlist: PlaylistWithSongs, menuItem: MenuItem): Boolean {
        return playlist.onPlaylistMenu(this, menuItem)
    }

    override fun playlistsMenuItemClick(playlists: List<PlaylistWithSongs>, menuItem: MenuItem) {
        playlists.onPlaylistsMenu(this, menuItem)
    }

    override fun onMediaContentChanged() {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun onFavoriteContentChanged() {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    override fun showViewTypeInBottomSheet(): Boolean = false
    override fun getSavedViewType(): GridViewType {
        return GridViewType.Normal
    }

    override fun saveViewType(viewType: GridViewType) {}

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(GRID_SIZE, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit { putInt(GRID_SIZE, newGridSize) }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val GRID_SIZE = "playlists_grid_size"
    }
}