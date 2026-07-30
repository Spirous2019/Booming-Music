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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.ui.adapters.song.SongAdapter
import com.mardous.booming.core.model.MediaEvent
import com.mardous.booming.databinding.FragmentMainRecyclerBinding
import com.mardous.booming.extensions.createBoomingMusicBalloon
import com.mardous.booming.extensions.dp
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.resources.createFastScroller
import com.mardous.booming.extensions.resources.onVerticalScroll
import com.mardous.booming.extensions.resources.primaryColor
import com.mardous.booming.extensions.resolveColor
import com.mardous.booming.extensions.setSupportActionBar
import com.mardous.booming.extensions.topLevelTransition
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.IScrollHelper
import com.mardous.booming.ui.dialogs.playlists.ImportPlaylistDialog
import com.mardous.booming.ui.screen.other.ShuffleModeFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageView
import android.widget.TextView
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.core.model.sort.KeySortItem
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.ui.dialogs.SortBottomSheetDialogFragment
import me.zhanghai.android.fastscroll.FastScroller
import org.koin.android.ext.android.inject

abstract class AbsRecyclerViewFragment<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsMainActivityFragment(R.layout.fragment_main_recycler), IScrollHelper {

    protected var _binding: FragmentMainRecyclerBinding? = null
    protected val binding get() = _binding!!

    protected var adapter: A? = null
    protected var layoutManager: LM? = null

    val toolbar: Toolbar get() = binding.appBarLayout.toolbar
    val shuffleButton: FloatingActionButton get() = binding.shuffleButton

    abstract val isShuffleVisible: Boolean
    abstract val titleRes: Int

    protected val sharedPreferences: SharedPreferences by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyWindowInsetsFromView(view)

        _binding = FragmentMainRecyclerBinding.bind(view)

        topLevelTransition(view)
        setSupportActionBar(toolbar)

        initLayoutManager()
        initAdapter()
        checkForMargins()
        setUpRecyclerView()
        setupToolbar()
        setupSubHeader()

        binding.swipeRefresh.setColorSchemeColors(requireContext().primaryColor())
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(requireContext().resolveColor(com.google.android.material.R.attr.colorSurfaceContainer))
        binding.swipeRefresh.setOnRefreshListener {
            libraryViewModel.refreshLibrary(requireContext().applicationContext)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                libraryViewModel.isRefreshingFlow.collect { refreshing ->
                    binding.swipeRefresh.isRefreshing = refreshing
                }
            }
        }

        // Add listeners when shuffle is visible
        if (isShuffleVisible) {
            binding.recyclerView.onVerticalScroll(
                viewLifecycleOwner,
                onScrollDown = { binding.shuffleButton.hide() },
                onScrollUp = { binding.shuffleButton.show() }
            )
            binding.shuffleButton.apply {
                setOnClickListener {
                    onShuffleClicked()
                }
                setOnLongClickListener {
                    onShuffleLongClick()
                }
            }
        } else {
            binding.shuffleButton.isVisible = false
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateScrollToPlayingButtonVisibility()
            }
        })

        binding.scrollToPlayingButton.setOnClickListener {
            val songAdapter = adapter as? SongAdapter
            val songs = songAdapter?.dataSet ?: emptyList()
            val playingSong = playerViewModel.currentSong
            val position = if (playingSong.id != -1L) songs.indexOfFirst { it.id == playingSong.id } else -1
            if (position != -1) {
                val smoothScroller = object : LinearSmoothScroller(activity) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }
                smoothScroller.targetPosition = position
                binding.recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
            }
        }

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.totalMargin)
        }
        libraryViewModel.getFabMargin().observe(viewLifecycleOwner) {
            binding.shuffleButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it.totalMargin
            }
            binding.scrollToPlayingButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it.totalMargin
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            launch {
                playerViewModel.mediaEvent.collect {
                    if (it == MediaEvent.PlaybackStarted) {
                        whichFragment<DialogFragment>("SHUFFLE_MODE")
                            ?.dismissAllowingStateLoss()
                    }
                }
            }
            launch {
                playerViewModel.currentSongFlow.collect {
                    updateScrollToPlayingButtonVisibility()
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        _binding?.shuffleButton?.doOnLayout {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                val balloon = createBoomingMusicBalloon("shuffle_button_tip") {
                    setDismissWhenClicked(true)
                    setText(getString(R.string.shuffle_button_tip))
                }
                _binding?.shuffleButton?.let {
                    if (it.isVisible && balloon?.isShowing == false) {
                        balloon.showAlignTop(it, yOff = (-8).dp(resources))
                    }
                }
            }
        }
    }

    open fun onShuffleClicked() {}

    open fun onShuffleLongClick(): Boolean {
        var shuffleModeFragment = whichFragment<ShuffleModeFragment>("SHUFFLE_MODE")
        if (shuffleModeFragment == null) {
            shuffleModeFragment = ShuffleModeFragment()
            shuffleModeFragment.show(childFragmentManager, "SHUFFLE_MODE")
            return true
        }
        return false
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.nav_search)
        }
        val appName = resources.getString(titleRes)
        binding.appBarLayout.title = appName
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = this@AbsRecyclerViewFragment.layoutManager
            adapter = this@AbsRecyclerViewFragment.adapter
            createFastScroller(this)
        }
    }

    protected open fun createFastScroller(recyclerView: RecyclerView): FastScroller {
        return recyclerView.createFastScroller()
    }

    private fun initAdapter() {
        adapter = createAdapter()
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    protected open val emptyMessageRes: Int
        @StringRes get() = R.string.empty_label

    private fun checkIsEmpty() {
        binding.emptyText.setText(emptyMessageRes)
        binding.empty.isVisible = adapter!!.itemCount == 0
    }

    private fun checkForMargins() {
        checkForMargins(binding.swipeRefresh)
    }

    private fun initLayoutManager() {
        layoutManager = createLayoutManager()
    }

    protected abstract fun createLayoutManager(): LM

    protected abstract fun createAdapter(): A

    protected fun invalidateLayoutManager() {
        initLayoutManager()
        binding.recyclerView.layoutManager = layoutManager
    }

    protected fun invalidateAdapter() {
        initAdapter()
        checkIsEmpty()
        binding.recyclerView.adapter = adapter
    }

    val recyclerView get() = binding.recyclerView

    val container get() = binding.root

    override fun scrollToTop() {
        recyclerView.scrollToPosition(0)
        binding.appBarLayout.setExpanded(true, true)
    }

    open fun getSortMode(): SortMode? = null
    open fun showViewTypeInBottomSheet(): Boolean = false
    open fun selectedViewType(): GridViewType? = null
    open fun onViewTypeChanged(viewType: GridViewType) {}
    open fun showOnlyAlbumArtistsInBottomSheet(): Boolean = false
    open fun isOnlyAlbumArtists(): Boolean = false
    open fun onOnlyAlbumArtistsChanged(enabled: Boolean) {}

    fun updateSubHeaderSortText() {
        val mode = getSortMode() ?: return
        val itemsField = SortMode::class.java.getDeclaredField("items")
        itemsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val items = itemsField.get(mode) as? List<*> ?: emptyList<Any>()
        val keyItems = items.filterIsInstance<KeySortItem>()

        val activeItem = keyItems.find { it.key == mode.selectedKey }
        if (activeItem != null) {
            val sortText = _binding?.appBarLayout?.findViewById<TextView>(R.id.sort_text)
            sortText?.setText(activeItem.title)
            val sortArrow = _binding?.appBarLayout?.findViewById<ImageView>(R.id.sort_arrow)
            sortArrow?.setImageResource(
                if (mode.selectedDescending) R.drawable.ic_keyboard_arrow_down_24dp
                else R.drawable.ic_keyboard_arrow_up_24dp
            )
        }
    }

    fun setSubHeaderItemCount(count: Int, labelSingularRes: Int, labelPluralRes: Int) {
        val label = if (count == 1) getString(labelSingularRes) else getString(labelPluralRes)
        _binding?.appBarLayout?.findViewById<TextView>(R.id.item_count_text)?.text = "$count $label"
    }

    protected open fun setupSubHeader() {
        val sortBtn = _binding?.appBarLayout?.findViewById<View>(R.id.sort_button)
        sortBtn?.setOnClickListener {
            val mode = getSortMode() ?: return@setOnClickListener
            val dialog = SortBottomSheetDialogFragment().apply {
                this.sortMode = mode
                this.onSortChanged = {
                    updateSubHeaderSortText()
                    onSortModeChanged()
                }
            }
            dialog.show(childFragmentManager, "SORT_BOTTOM_SHEET")
        }
        updateSubHeaderSortText()
    }

    open fun onSortModeChanged() {
        adapter?.notifyDataSetChanged()
    }

    override fun onPrepareMenu(menu: Menu) {
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_library, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> findNavController().navigate(R.id.nav_settings)
            R.id.action_import_playlist -> ImportPlaylistDialog().show(childFragmentManager, "IMPORT_PLAYLIST")
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        checkForMargins()
    }

    override fun onDestroyView() {
        _binding?.recyclerView?.layoutManager = null
        _binding?.recyclerView?.adapter = null
        _binding = null
        super.onDestroyView()
        layoutManager = null
        adapter = null
    }

    override fun onPause() {
        super.onPause()
        (adapter as? AbsMultiSelectAdapter<*, *>)?.actionMode?.finish()
    }

    private fun updateScrollToPlayingButtonVisibility() {
        val songAdapter = adapter as? SongAdapter
        val songs = songAdapter?.dataSet ?: emptyList()
        val playingSong = playerViewModel.currentSong
        val position = if (playingSong.id != -1L) songs.indexOfFirst { it.id == playingSong.id } else -1

        if (position != -1) {
            val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
            val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: -1
            val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: -1

            val isNotVisible = position < firstVisible || position > lastVisible

            if (isNotVisible) {
                binding.scrollToPlayingButton.show()
            } else {
                binding.scrollToPlayingButton.hide()
            }
        } else {
            binding.scrollToPlayingButton.hide()
        }
    }
}