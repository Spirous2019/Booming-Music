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

package com.mardous.booming.ui.screen.player.styles.defaultstyle

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerColorSchemeMode
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.surfaceTintTarget
import com.mardous.booming.core.model.player.tintTarget
import com.mardous.booming.core.model.player.iconButtonTintTarget
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.databinding.FragmentDefaultPlayerBinding
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.util.DISPLAY_NEXT_SONG
import com.mardous.booming.util.Preferences
import androidx.core.content.ContextCompat
import com.mardous.booming.ui.component.menu.MenuBottomSheetDialogFragment
import com.mardous.booming.ui.component.menu.findAppCompatActivity
import com.mardous.booming.ui.component.menu.newPopupMenu

import com.mardous.booming.extensions.resolveColor

/**
 * @author Christians M. A. (mardous)
 */
class DefaultPlayerFragment : AbsPlayerFragment(R.layout.fragment_default_player),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentDefaultPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: DefaultPlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.Default)

    override val playerToolbar: Toolbar
        get() = binding.toolbar

    override val blurView: ImageView
        get() = binding.blur

    private var primaryControlColor: Int = android.graphics.Color.WHITE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDefaultPlayerBinding.bind(view)
        primaryControlColor = context?.resolveColor(com.google.android.material.R.attr.colorOnSurface) ?: android.graphics.Color.WHITE
        setupToolbar()
        setupCustomBottomBar()
        inflateMenuInView(playerToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            WindowInsetsCompat.CONSUMED
        }
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupToolbar() {
        playerToolbar.setNavigationOnClickListener {
            onQuickActionEvent(NowPlayingAction.SoundSettings)
        }
    }

    private fun setupCustomBottomBar() {
        binding.customSoundSettingsButton?.setOnClickListener {
            onQuickActionEvent(NowPlayingAction.SoundSettings)
        }
        binding.customFavoriteButton?.setOnClickListener {
            onQuickActionEvent(NowPlayingAction.ToggleFavoriteState)
        }
        binding.customLyricsButton?.setOnClickListener {
            onQuickActionEvent(NowPlayingAction.Lyrics)
        }
        binding.customQueueButton?.setOnClickListener {
            onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
        }
        
        binding.customMenuButton?.let { menuBtn ->
            val popupMenu = newPopupMenu(menuBtn, R.menu.menu_now_playing) {
                onMenuInflated(it)
            }
            nowPlayingPopupMenu = popupMenu
            popupMenu.menu.setIsFavorite(isFavorite, false)
            menuBtn.setOnClickListener {
                val activity = menuBtn.context.findAppCompatActivity()
                if (activity != null) {
                    popupMenu.menu.setIsFavorite(isFavorite, false)
                    MenuBottomSheetDialogFragment()
                        .setMenu(popupMenu.menu) { itemId ->
                            val item = popupMenu.menu.findItem(itemId)
                            if (item != null) {
                                onMenuItemClick(item)
                            }
                        }
                        .show(activity.supportFragmentManager, MenuBottomSheetDialogFragment.TAG)
                } else {
                    popupMenu.setOnMenuItemClickListener { onMenuItemClick(it) }
                    popupMenu.show()
                }
            }
        }
        
        updateButtonTints()
    }

    override fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        super.onIsFavoriteChanged(isFavorite, withAnimation)
        val iconRes = if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        binding.customFavoriteButton?.setImageResource(iconRes)
        updateButtonTints()
    }

    override fun onLyricsVisibilityChange(animatorSet: android.animation.AnimatorSet, lyricsVisible: Boolean) {
        super.onLyricsVisibilityChange(animatorSet, lyricsVisible)
        val iconRes = if (lyricsVisible) R.drawable.ic_lyrics_24dp else R.drawable.ic_lyrics_outline_24dp
        binding.customLyricsButton?.setImageResource(iconRes)
        updateButtonTints()
    }

    private fun updateButtonTints() {
        val colorStateList = android.content.res.ColorStateList.valueOf(primaryControlColor)
        binding.customSoundSettingsButton?.imageTintList = colorStateList
        binding.customFavoriteButton?.imageTintList = colorStateList
        binding.customLyricsButton?.imageTintList = colorStateList
        binding.customQueueButton?.imageTintList = colorStateList
        binding.customMenuButton?.imageTintList = colorStateList
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldPrimaryControlColor = primaryControlColor
        primaryControlColor = scheme.onSurfaceColor
        return mutableListOf(
            binding.root.surfaceTintTarget(scheme.surfaceColor),
            binding.toolbar.tintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)
        ).also { list ->
            binding.customSoundSettingsButton?.let { list.add(it.iconButtonTintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)) }
            binding.customFavoriteButton?.let { list.add(it.iconButtonTintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)) }
            binding.customLyricsButton?.let { list.add(it.iconButtonTintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)) }
            binding.customQueueButton?.let { list.add(it.iconButtonTintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)) }
            binding.customMenuButton?.let { list.add(it.iconButtonTintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)) }
            list.addAll(playerControlsFragment.getTintTargets(scheme))
            updateButtonTints()
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_sound_settings)
        menu.setShowAsAction(R.id.action_favorite)
        menu.setShowAsAction(R.id.action_show_lyrics)
        setupQueueMenuItem(menu)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    private fun setupQueueMenuItem(menu: Menu = playerToolbar.menu) {
        menu.findItem(R.id.action_playing_queue)?.let {
            it.isVisible = !Preferences.isShowNextSong
            it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == DISPLAY_NEXT_SONG) {
            setupQueueMenuItem()
        }
    }

    override fun onDestroyView() {
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
        _binding = null
    }
}