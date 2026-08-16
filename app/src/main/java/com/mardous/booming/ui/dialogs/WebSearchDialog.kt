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

package com.mardous.booming.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.core.model.WebSearchEngine
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.EXTRA_SONG
import com.mardous.booming.extensions.media.searchQuery
import com.mardous.booming.extensions.openWeb
import com.mardous.booming.extensions.toChooser
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.ui.theme.PlayerTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class WebSearchDialog : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

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
        val song = BundleCompat.getParcelable(requireArguments(), EXTRA_SONG, Song::class.java)!!

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val playerColorScheme by playerViewModel.colorSchemeFlow.collectAsState()
                BoomingMusicTheme {
                    PlayerTheme(playerColorScheme = playerColorScheme) {
                        BottomSheetDialogSurface {
                            WebSearchScreen(
                                song = song,
                                onEngineClick = { engine ->
                                    startActivity(
                                        song.searchQuery(engine).openWeb()
                                            .toChooser(getString(R.string.web_search))
                                    )
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
    private fun WebSearchScreen(
        song: Song,
        onEngineClick: (WebSearchEngine) -> Unit
    ) {
        val engines = WebSearchEngine.entries

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 16.dp)
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.where_do_you_want_to_search),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (song != Song.emptySong) {
                    Text(
                        text = "${song.title} • ${song.artistName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            engines.forEach { engine ->
                val iconRes = when (engine) {
                    WebSearchEngine.Google -> R.drawable.ic_language_24dp
                    WebSearchEngine.YouTube -> R.drawable.ic_play_circle_24dp
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEngineClick(engine) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = stringResource(engine.nameRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    companion object {
        fun create(song: Song) = WebSearchDialog().withArgs {
            putParcelable(EXTRA_SONG, song)
        }
    }
}