package com.mardous.booming.ui.component.menu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.ui.theme.PlayerTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.ui.component.compose.MediaImage

import androidx.compose.ui.res.painterResource

data class BottomSheetMenuItem(
    val id: Int,
    val title: String,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val iconRes: Int? = null,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false
)

class MenuBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var menuItems: List<BottomSheetMenuItem> = emptyList()
    private var onMenuItemSelected: ((Int) -> Unit)? = null
    private var headerSong: Song? = null

    fun setSongHeader(song: Song?): MenuBottomSheetDialogFragment {
        this.headerSong = song
        return this
    }

    fun setMenu(menu: Menu, onItemSelected: (Int) -> Unit): MenuBottomSheetDialogFragment {
        val items = mutableListOf<BottomSheetMenuItem>()
        addMenuItems(menu, items, "")
        this.menuItems = items
        this.onMenuItemSelected = onItemSelected
        return this
    }

    private fun addMenuItems(
        menu: Menu,
        items: MutableList<BottomSheetMenuItem>,
        parentTitle: String
    ) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.hasSubMenu()) {
                val subMenu = item.subMenu
                if (subMenu != null) {
                    val currentParentTitle = item.title?.toString() ?: ""
                    val newParentTitle = if (parentTitle.isNotEmpty()) {
                        "$parentTitle $currentParentTitle"
                    } else {
                        currentParentTitle
                    }
                    addMenuItems(subMenu, items, newParentTitle)
                }
            } else {
                val itemTitle = item.title?.toString() ?: ""
                val fullTitle = if (parentTitle.isNotEmpty()) {
                    "$parentTitle $itemTitle"
                } else {
                    itemTitle
                }
                items.add(
                    BottomSheetMenuItem(
                        id = item.itemId,
                        title = fullTitle,
                        isEnabled = item.isEnabled,
                        isVisible = item.isVisible,
                        iconRes = getMenuItemIcon(item.itemId, itemTitle),
                        isCheckable = item.isCheckable,
                        isChecked = item.isChecked
                    )
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            it.behavior.skipCollapsed = true
            it.behavior.isFitToContents = true
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val playerColorScheme by playerViewModel.colorSchemeFlow.collectAsState()
                BoomingMusicTheme {
                    PlayerTheme(playerColorScheme = playerColorScheme) {
                        BottomSheetDialogSurface {
                            MenuBottomSheetScreen(
                                items = menuItems.filter { it.isVisible },
                                onItemClick = { id ->
                                    onMenuItemSelected?.invoke(id)
                                    dismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MenuBottomSheetScreen(
        items: List<BottomSheetMenuItem>,
        onItemClick: (Int) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 16.dp)
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            headerSong?.let { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(50.dp)
                    ) {
                        MediaImage(model = song)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.displayArtistName(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val nestedScrollConnection = rememberNestedScrollInteropConnection()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .nestedScroll(nestedScrollConnection)
            ) {
                itemsIndexed(items) { index, item ->
                    val alpha = if (item.isEnabled) 1f else 0.4f
                    
                    if (index > 0) {
                        val currentGroup = getMenuItemGroup(item.id, item.title)
                        val prevGroup = getMenuItemGroup(items[index - 1].id, items[index - 1].title)
                        if (currentGroup != prevGroup) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = item.isEnabled) { onItemClick(item.id) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.iconRes?.let { iconDrawable ->
                            Icon(
                                painter = painterResource(id = iconDrawable),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            modifier = Modifier.weight(1f)
                        )
                        if (item.isCheckable) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = item.isChecked,
                                onCheckedChange = null,
                                enabled = item.isEnabled
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getMenuItemIcon(itemId: Int, title: String): Int? {
        return when (itemId) {
            R.id.action_play -> R.drawable.ic_play_24dp
            R.id.action_play_next, R.id.action_put_after_current_track -> R.drawable.ic_queue_play_next_24dp
            R.id.action_add_to_playing_queue, R.id.action_playing_queue -> R.drawable.ic_queue_music_24dp
            R.id.action_add_to_playlist -> R.drawable.ic_playlist_add_24dp
            R.id.action_remove_from_playing_queue -> R.drawable.ic_clear_all_24dp
            R.id.action_remove_from_playlist -> R.drawable.ic_remove_circle_24dp
            R.id.action_stop_after_track -> R.drawable.ic_stop_circle_24dp
            R.id.action_sleep_timer -> R.drawable.ic_timer_24dp
            R.id.action_shuffle_play -> R.drawable.ic_shuffle_24dp
            R.id.action_go_to_album -> R.drawable.ic_album_24dp
            R.id.action_go_to_artist -> R.drawable.ic_artist_24dp
            R.id.menu_go_to -> R.drawable.ic_folder_24dp
            R.id.action_tag_editor -> R.drawable.ic_edit_note_24dp
            R.id.action_edit_playlist -> R.drawable.ic_edit_24dp
            R.id.action_export_playlist -> R.drawable.ic_file_export_24dp
            R.id.action_show_lyrics -> R.drawable.ic_lyrics_24dp
            R.id.action_share, R.id.action_share_now_playing -> R.drawable.ic_share_24dp
            R.id.action_delete_from_device, R.id.action_delete_playlist -> R.drawable.ic_delete_24dp
            R.id.action_blacklist -> R.drawable.ic_blacklist_24dp
            R.id.action_details -> R.drawable.ic_info_24dp
            R.id.action_play_info -> R.drawable.ic_info_24dp
            R.id.action_horizontal_albums -> R.drawable.ic_view_carousel_24dp
            R.id.action_toggle_compact_song_view -> R.drawable.ic_format_line_spacing_24dp
            R.id.action_ignore_singles -> R.drawable.ic_music_note_24dp
            R.id.action_show_album_duration -> R.drawable.ic_timer_24dp
            R.id.action_equalizer -> R.drawable.ic_equalizer_24dp
            R.id.action_sound_settings -> R.drawable.ic_volume_up_24dp
            R.id.action_web_search -> R.drawable.ic_language_24dp
            R.id.action_favorite -> R.drawable.ic_favorite_24dp
            R.id.action_set_as_ringtone -> R.drawable.ic_phonelink_ring_24dp
            R.id.action_multi_select_adapter_check_all -> R.drawable.ic_select_all_24dp
            R.id.action_change_artist_image -> R.drawable.ic_image_24dp
            R.id.action_lock -> R.drawable.ic_lock_24dp
            else -> {
                val idString = try {
                    context?.resources?.getResourceEntryName(itemId) ?: ""
                } catch (e: Exception) { "" }
                when {
                    idString.contains("stop_after") || title.contains("stop after", ignoreCase = true) -> R.drawable.ic_stop_circle_24dp
                    idString.contains("sleep_timer") || title.contains("sleep timer", ignoreCase = true) -> R.drawable.ic_timer_24dp
                    idString.contains("remove_from_playlist") || title.contains("remove from playlist", ignoreCase = true) -> R.drawable.ic_remove_circle_24dp
                    idString.contains("play_next") || idString.contains("queue_next") || idString.contains("put_after") -> R.drawable.ic_queue_play_next_24dp
                    idString.contains("playing_queue") -> R.drawable.ic_queue_music_24dp
                    idString.contains("playlist") -> R.drawable.ic_playlist_add_24dp
                    idString.contains("album") -> R.drawable.ic_album_24dp
                    idString.contains("artist") -> R.drawable.ic_artist_24dp
                    idString.contains("folder") || idString.contains("dir") -> R.drawable.ic_folder_24dp
                    idString.contains("tag") -> R.drawable.ic_edit_note_24dp
                    idString.contains("edit") -> R.drawable.ic_edit_24dp
                    idString.contains("export") -> R.drawable.ic_file_export_24dp
                    idString.contains("lyric") -> R.drawable.ic_lyrics_24dp
                    idString.contains("share") -> R.drawable.ic_share_24dp
                    idString.contains("delete") -> R.drawable.ic_delete_24dp
                    idString.contains("black") -> R.drawable.ic_blacklist_24dp
                    idString.contains("detail") || idString.contains("info") -> R.drawable.ic_info_24dp
                    idString.contains("select_all") -> R.drawable.ic_select_all_24dp
                    title.contains("Go to album", ignoreCase = true) -> R.drawable.ic_album_24dp
                    title.contains("Go to artist", ignoreCase = true) -> R.drawable.ic_artist_24dp
                    title.contains("Go to folder", ignoreCase = true) -> R.drawable.ic_folder_24dp
                    title.contains("Tag", ignoreCase = true) -> R.drawable.ic_edit_24dp
                    title.contains("Lyric", ignoreCase = true) -> R.drawable.ic_lyrics_24dp
                    title.contains("Blacklist", ignoreCase = true) -> R.drawable.ic_blacklist_24dp
                    title.contains("Detail", ignoreCase = true) -> R.drawable.ic_info_24dp
                    title.contains("Share", ignoreCase = true) -> R.drawable.ic_share_24dp
                    title.contains("Delete", ignoreCase = true) || title.contains("Remove", ignoreCase = true) -> R.drawable.ic_delete_24dp
                    else -> null
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val displayMetrics = resources.displayMetrics
                val maxHeight = (displayMetrics.heightPixels * 0.75).toInt()
                
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                
                sheet.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        sheet.viewTreeObserver.removeOnPreDrawListener(this)
                        if (sheet.height > maxHeight) {
                            sheet.layoutParams = sheet.layoutParams.apply {
                                height = maxHeight
                            }
                        }
                        return true
                    }
                })
            }
        }
    }

    private fun getMenuItemGroup(itemId: Int, title: String): Int {
        // Group 2: Navigation (Go to)
        if (itemId == R.id.action_go_to_album || itemId == R.id.action_go_to_artist || 
            itemId == R.id.menu_go_to) {
            return 2
        }

        // Group 1: Playback, Queue, Playlist, Lyrics, Favorites, Sleep Timer
        if (itemId == R.id.action_play || itemId == R.id.action_play_next || 
            itemId == R.id.action_add_to_playing_queue || itemId == R.id.action_remove_from_playing_queue || 
            itemId == R.id.action_clear_playing_queue || itemId == R.id.action_stop_after_track || 
            itemId == R.id.action_lock || itemId == R.id.action_equalizer || 
            itemId == R.id.action_sound_settings || itemId == R.id.action_sleep_timer || 
            itemId == R.id.action_playing_queue || itemId == R.id.action_favorite || 
            itemId == R.id.action_show_lyrics || itemId == R.id.action_add_to_playlist) {
            return 1
        }

        // Group 3: Metadata & Editing
        if (itemId == R.id.action_tag_editor) {
            return 3
        }

        // Group 4: Utilities / Share / Ringtone
        if (itemId == R.id.action_set_as_ringtone || itemId == R.id.action_share || 
            itemId == R.id.action_share_now_playing) {
            return 4
        }

        // Group 5: Destructive, Info, Search, Blacklist
        if (itemId == R.id.action_delete_from_device || itemId == R.id.action_details || 
            itemId == R.id.action_web_search || itemId == R.id.action_remove_from_playlist ||
            itemId == R.id.action_blacklist) {
            return 5
        }

        // Language/obfuscation fallbacks using resource names
        val idString = try {
            context?.resources?.getResourceEntryName(itemId) ?: ""
        } catch (e: Exception) {
            ""
        }
        
        if (idString.startsWith("action_go_to") || idString == "menu_go_to" || 
            title.contains("Go to", ignoreCase = true) || title.contains("Перейти", ignoreCase = true)) {
            return 2
        }
        
        if (idString.contains("play") || idString.contains("queue") || idString.contains("equalizer") || 
            idString.contains("timer") || idString.contains("favorite") || idString.contains("lyrics") || 
            idString.contains("playlist") || idString.contains("stop") || idString.contains("lock") || 
            idString.contains("sound") || idString.contains("volume")) {
            return 1
        }
        
        if (idString.contains("edit") || idString.contains("tag")) {
            return 3
        }
        
        if (idString.contains("share") || idString.contains("ringtone") || idString.contains("use_as")) {
            return 4
        }
        
        if (idString.contains("delete") || idString.contains("blacklist") || idString.contains("detail") || 
            idString.contains("search") || idString.contains("info") || idString.contains("clear")) {
            return 5
        }
        
        return 1
    }
    
    companion object {
        const val TAG = "MenuBottomSheetDialogFragment"
    }
}
