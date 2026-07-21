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

data class BottomSheetMenuItem(
    val id: Int,
    val title: String,
    val isEnabled: Boolean,
    val isVisible: Boolean
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
                        isVisible = item.isVisible
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
 
            LazyColumn {
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
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                    }
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
