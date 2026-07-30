package com.mardous.booming.ui.dialogs.songs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.model.Artist
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.component.compose.MediaImage
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.ui.theme.PlayerTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ArtistChooserBottomSheet : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var onArtistSelected: ((String) -> Unit)? = null

    fun setCallback(callback: (String) -> Unit): ArtistChooserBottomSheet {
        this.onArtistSelected = callback
        return this
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
        val artistNames = arguments?.getStringArrayList(ARG_ARTISTS) ?: emptyList<String>()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val playerColorScheme by playerViewModel.colorSchemeFlow.collectAsState()
                BoomingMusicTheme {
                    PlayerTheme(playerColorScheme = playerColorScheme) {
                        BottomSheetDialogSurface {
                            ArtistChooserScreen(
                                artistNames = artistNames,
                                onArtistClick = { name ->
                                    onArtistSelected?.invoke(name)
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
    private fun ArtistChooserScreen(
        artistNames: List<String>,
        onArtistClick: (String) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = stringResource(R.string.action_go_to_artist),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val nestedScrollConnection = rememberNestedScrollInteropConnection()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .padding(bottom = 16.dp)
                    .nestedScroll(nestedScrollConnection)
            ) {
                items(artistNames) { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val artist = Artist(
                            id = -1L,
                            albums = emptyList(),
                            filterSingles = false,
                            nameOverride = name
                        )
                        MediaImage(
                            model = artist,
                            placeholderIcon = R.drawable.ic_artist_24dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_ARTISTS = "arg_artists"

        fun create(artistNames: List<String>): ArtistChooserBottomSheet {
            val fragment = ArtistChooserBottomSheet()
            val args = Bundle().apply {
                putStringArrayList(ARG_ARTISTS, ArrayList(artistNames))
            }
            fragment.arguments = args
            return fragment
        }
    }
}
