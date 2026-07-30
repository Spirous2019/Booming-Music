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

package com.mardous.booming.ui.screen.queue

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.Px
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.mardous.booming.R
import com.mardous.booming.coil.songImage
import com.mardous.booming.core.model.theme.NowPlayingButtonStyle
import com.mardous.booming.data.model.QueuePosition
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentQueueBinding
import com.mardous.booming.extensions.Space
import com.mardous.booming.extensions.applyBottomWindowInsets
import com.mardous.booming.extensions.dip
import com.mardous.booming.extensions.dp
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.songInfo
import com.mardous.booming.extensions.resources.createFastScroller
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.song.PlayingQueueSongAdapter
import com.mardous.booming.ui.component.menu.newPopupMenu
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.dialogs.playlists.AddToPlaylistDialog
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.screen.player.QUEUE_DEBOUNCE
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.roundToInt

/**
 * @author Christians M. A. (mardous)
 */
class QueueFragment : BottomSheetDialogFragment(R.layout.fragment_queue), ISongCallback {

    private val playerViewModel: PlayerViewModel by activityViewModel()
    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private var playingQueueAdapter: PlayingQueueSongAdapter? = null
    private var dragDropManager: RecyclerViewDragDropManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var layoutManager: LinearLayoutManager? = null

    private val playlist: List<Song>
        get() = playerViewModel.queue

    private val position: QueuePosition
        get() = playerViewModel.position

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme_EdgeToEdge

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val queueHeight = getFixedQueueHeight()
        if (!isLandscape()) {
            if (Preferences.queueHeight) {
                dialog.behavior.apply {
                    peekHeight = queueHeight
                    maxHeight = queueHeight
                }
            }
        } else {
            dialog.behavior.peekHeight = queueHeight
        }
        return dialog
    }

    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQueueBinding.bind(view)
        binding.recyclerView.isNestedScrollingEnabled = true
        binding.recyclerView.applyBottomWindowInsets(
            addedSpace = Space.bottom(8.dp(view.context))
        )

        playingQueueAdapter = PlayingQueueSongAdapter(
            activity = requireActivity(),
            playlist = playlist.toMutableList(),
            current = position.current,
            callback = this
        ).also { adapter ->
            dragDropManager = RecyclerViewDragDropManager().also { manager ->
                wrappedAdapter = manager.createWrappedAdapter(adapter)
            }
        }

        layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = RefactoredDefaultItemAnimator()

        dragDropManager!!.attachRecyclerView(_binding!!.recyclerView)
        layoutManager!!.scrollToPosition(position.current)

        binding.recyclerView.createFastScroller()

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(playerViewModel.queueFlow, playerViewModel.positionFlow) { queue, position ->
                Pair(queue, position)
            }.debounce(QUEUE_DEBOUNCE).collectLatest { (queue, position) ->
                // debounce queue updates to avoid UI flickering
                if (queue.isEmpty()) {
                    findNavController().navigateUp()
                } else {
                    val firstVisibleItemPosition = layoutManager?.findFirstVisibleItemPosition()
                    if (firstVisibleItemPosition == position.current) {
                        syncListPosition()
                    }
                    playingQueueAdapter?.setPlayingQueue(queue.toMutableList(), position.current)
                }
            }
        }
    }

    @Px
    private fun getFixedQueueHeight(): Int {
        val windowManager = requireContext().getSystemService<WindowManager>()
        if (windowManager == null) return dip(R.dimen.queue_height)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val height = metrics.bounds.height() + insets.bottom
            return (height / 2f).roundToInt()
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = resources.displayMetrics
            return (displayMetrics.heightPixels / 2f).roundToInt()
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return song.onSongMenu(this, menuItem)
    }

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {}

    private fun syncListPosition() {
        binding.recyclerView.stopScroll()
        layoutManager?.scrollToPositionWithOffset(position.current, 0)
    }

    override fun onPause() {
        dragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onDestroyView() {
        dragDropManager?.release()
        dragDropManager = null

        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        wrappedAdapter = null
        playingQueueAdapter = null

        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = null
        binding.recyclerView.layoutManager = null

        layoutManager = null
        super.onDestroyView()
        _binding = null
    }
}