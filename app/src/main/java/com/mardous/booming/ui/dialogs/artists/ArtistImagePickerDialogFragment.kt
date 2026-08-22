package com.mardous.booming.ui.dialogs.artists

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.coil.CustomArtistImageManager
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.remote.deezer.model.DeezerArtist
import com.mardous.booming.extensions.utilities.normalize
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.ui.theme.PlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

enum class ImageSource(val title: String) {
    DuckDuckGo("DuckDuckGo Web Search"),
    Deezer("Deezer Portraits"),
    ITunes("iTunes Portraits")
}

class ArtistImagePickerDialogFragment : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()
    private val customImageManager: CustomArtistImageManager by inject()
    private val repository: Repository by inject()

    var artist: Artist? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val currentArtist = artist
        if (uri != null && currentArtist != null) {
            lifecycleScope.launch {
                customImageManager.setNoImage(currentArtist, false)
                customImageManager.setCustomImage(currentArtist, uri)
                setFragmentResult(REQUEST_KEY, Bundle())
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let { bsd ->
            bsd.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            bsd.behavior.skipCollapsed = true
            bsd.behavior.isFitToContents = true
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
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
                        val nestedScrollInterop = rememberNestedScrollInteropConnection()
                        BottomSheetDialogSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(nestedScrollInterop)
                        ) {
                            ArtistImagePickerContent()
                        }
                    }
                }
            }
        }
    }

    /**
     * Represents a candidate image in the picker grid.
     * @param label The display label (artist name or image title)
     * @param url The image URL
     * @param source The remote provider source category
     */
    private data class CandidateImage(
        val label: String,
        val url: String,
        val source: ImageSource
    )

    @Composable
    private fun ArtistImagePickerContent() {
        val currentArtist = artist ?: return
        var searchQuery by remember { mutableStateOf(currentArtist.name) }
        var isSearching by remember { mutableStateOf(false) }
        var candidates by remember { mutableStateOf<List<CandidateImage>>(emptyList()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(searchQuery) {
            if (searchQuery.isBlank()) return@LaunchedEffect
            isSearching = true
            try {
                val resolvedName = DeezerArtist.ARTIST_ALIASES[searchQuery.trim().lowercase()] ?: searchQuery
                val allCandidates = mutableListOf<CandidateImage>()
                val seenHashes = mutableSetOf<String>()

                withContext(Dispatchers.IO) {
                    // Fetch DuckDuckGo, Deezer, and iTunes concurrently in priority order
                    val duckDeferred = async {
                        try { repository.duckDuckGoArtistPortraits(resolvedName) } catch (_: Exception) { emptyList() }
                    }
                    val deezerDeferred = async {
                        try { repository.deezerArtist(resolvedName, 30, 0) } catch (_: Exception) { null }
                    }
                    val itunesDeferred = async {
                        try { repository.iTunesArtist(resolvedName) } catch (_: Exception) { null }
                    }

                    val duckResults = duckDeferred.await()
                    val deezerResult = deezerDeferred.await()
                    val itunesResult = itunesDeferred.await()

                    // 1. DuckDuckGo Web Portrait Photos (Priority 1)
                    for ((label, url) in duckResults) {
                        if (url.isNotBlank() && url.startsWith("http") && seenHashes.add(url)) {
                            allCandidates.add(CandidateImage(label, url, ImageSource.DuckDuckGo))
                        }
                    }

                    // 2. Deezer Artist Avatar (Priority 2)
                    if (deezerResult != null) {
                        val filtered = deezerResult.getFilteredCandidates(resolvedName)
                        for ((name, url) in filtered) {
                            if (url.isNotBlank() && url.startsWith("http") && !url.contains("/images/artist//")) {
                                val hash = extractImageHash(url)
                                if (hash == null || seenHashes.add(hash)) {
                                    allCandidates.add(CandidateImage(name, url, ImageSource.Deezer))
                                }
                            }
                        }
                    }

                    // 3. iTunes Artist Portraits (Priority 3)
                    if (itunesResult != null) {
                        val filtered = itunesResult.getFilteredCandidates(resolvedName)
                        for ((name, url) in filtered) {
                            if (url.isNotBlank() && url.startsWith("http") && seenHashes.add(url)) {
                                allCandidates.add(CandidateImage(name, url, ImageSource.ITunes))
                            }
                        }
                    }
                }

                candidates = allCandidates
            } catch (e: Exception) {
                candidates = emptyList()
            } finally {
                isSearching = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.change_artist_image_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search_artist_images_hint)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.choose_from_device),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            customImageManager.setNoImage(currentArtist, true)
                            setFragmentResult(REQUEST_KEY, Bundle())
                            dismissAllowingStateLoss()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.remove_image_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (candidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_images_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val duckCandidates = candidates.filter { it.source == ImageSource.DuckDuckGo }
                val deezerCandidates = candidates.filter { it.source == ImageSource.Deezer }
                val itunesCandidates = candidates.filter { it.source == ImageSource.ITunes }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. DuckDuckGo Section (Priority 1)
                    if (duckCandidates.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            CategoryHeader(ImageSource.DuckDuckGo.title, duckCandidates.size)
                        }
                        items(duckCandidates) { candidate ->
                            CandidateImageCard(candidate, currentArtist, scope)
                        }
                    }

                    // 2. Deezer Section (Priority 2)
                    if (deezerCandidates.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            CategoryHeader(ImageSource.Deezer.title, deezerCandidates.size)
                        }
                        items(deezerCandidates) { candidate ->
                            CandidateImageCard(candidate, currentArtist, scope)
                        }
                    }

                    // 3. iTunes Section (Priority 3)
                    if (itunesCandidates.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            CategoryHeader(ImageSource.ITunes.title, itunesCandidates.size)
                        }
                        items(itunesCandidates) { candidate ->
                            CandidateImageCard(candidate, currentArtist, scope)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CategoryHeader(title: String, count: Int) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }

    @Composable
    private fun CandidateImageCard(
        candidate: CandidateImage,
        currentArtist: Artist,
        scope: kotlinx.coroutines.CoroutineScope
    ) {
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    scope.launch {
                        val success = downloadAndSetImage(currentArtist, candidate.url)
                        if (success) {
                            setFragmentResult(REQUEST_KEY, Bundle())
                            dismissAllowingStateLoss()
                        }
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                AsyncImage(
                    model = candidate.url,
                    contentDescription = candidate.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    text = candidate.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }

    /**
     * Extracts the unique image hash from a Deezer artist image URL for deduplication.
     */
    private fun extractImageHash(url: String): String? {
        val regex = Regex("/images/artist/([^/]+)/")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    /**
     * Extracts the unique image hash from a Deezer cover image URL for deduplication.
     */
    private fun extractCoverHash(url: String): String? {
        val regex = Regex("/images/cover/([^/]+)/")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    private suspend fun downloadAndSetImage(artist: Artist, urlString: String): Boolean {
        customImageManager.setNoImage(artist, false)
        return customImageManager.setCustomImageFromUrl(artist, urlString)
    }

    companion object {
        const val TAG = "ArtistImagePickerDialogFragment"
        const val REQUEST_KEY = "artist_image_updated"

        fun newInstance(artist: Artist): ArtistImagePickerDialogFragment {
            return ArtistImagePickerDialogFragment().apply {
                this.artist = artist
            }
        }
    }
}
