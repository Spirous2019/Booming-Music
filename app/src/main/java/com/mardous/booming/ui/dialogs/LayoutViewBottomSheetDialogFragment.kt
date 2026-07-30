package com.mardous.booming.ui.dialogs

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.ui.theme.PlayerTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LayoutViewBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    var currentGridSize: Int = 3
    var maxGridSize: Int = 6
    var onGridSizeSelected: ((Int) -> Unit)? = null

    var showViewType: Boolean = false
    var selectedViewType: GridViewType? = null
    var onViewTypeChanged: ((GridViewType) -> Unit)? = null

    var showOnlyAlbumArtists: Boolean = false
    var isOnlyAlbumArtists: Boolean = false
    var onOnlyAlbumArtistsChanged: ((Boolean) -> Unit)? = null

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
                val displayMetrics = resources.displayMetrics
                val maxHeight = (displayMetrics.heightPixels * 0.70).toInt()
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                BottomSheetBehavior.from(sheet).apply {
                    this.maxHeight = maxHeight
                }
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
                        BottomSheetDialogSurface {
                            LayoutViewBottomSheetScreen()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LayoutViewBottomSheetScreen() {
        var gridSize by remember { mutableIntStateOf(currentGridSize) }
        var activeViewType by remember { mutableStateOf(selectedViewType) }
        var onlyAlbumArtistsState by remember { mutableStateOf(isOnlyAlbumArtists) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 16.dp)
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = stringResource(R.string.layout_grid_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )

            val nestedScrollConnection = rememberNestedScrollInteropConnection()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .nestedScroll(nestedScrollConnection)
            ) {
                // 1. Grid Size Section (Horizontal Pill Selector)
                item {
                    Text(
                        text = stringResource(R.string.grid_size_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    val sizeLimit = maxOf(1, minOf(8, maxGridSize))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..sizeLimit).forEach { count ->
                            val isSelected = count == gridSize
                            Surface(
                                onClick = {
                                    gridSize = count
                                    onGridSizeSelected?.invoke(count)
                                },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. View Type Section (if enabled for this screen)
                if (showViewType) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                        Text(
                            text = stringResource(R.string.view_type_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }

                    items(GridViewType.entries) { type ->
                        val isSelected = type == activeViewType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeViewType = type
                                    onViewTypeChanged?.invoke(type)
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(type.titleRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24dp),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Only Album Artists Section (if on Artists screen)
                if (showOnlyAlbumArtists) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newToggle = !onlyAlbumArtistsState
                                    onlyAlbumArtistsState = newToggle
                                    onOnlyAlbumArtistsChanged?.invoke(newToggle)
                                }
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.show_album_artists),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = onlyAlbumArtistsState,
                                onCheckedChange = { checked ->
                                    onlyAlbumArtistsState = checked
                                    onOnlyAlbumArtistsChanged?.invoke(checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
