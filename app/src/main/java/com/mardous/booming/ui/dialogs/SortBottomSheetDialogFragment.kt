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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.core.model.sort.SortKey
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.ui.component.compose.BottomSheetDialogSurface
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.ui.theme.PlayerTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SortBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    var sortMode: SortMode? = null
    var onSortChanged: (() -> Unit)? = null

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
                            sortMode?.let { mode ->
                                SortBottomSheetScreen(
                                    sortMode = mode,
                                    onSortChanged = {
                                        onSortChanged?.invoke()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SortBottomSheetScreen(
        sortMode: SortMode,
        onSortChanged: () -> Unit
    ) {
        var currentKey by remember { mutableStateOf(sortMode.selectedKey) }
        var isDescending by remember { mutableStateOf(sortMode.selectedDescending) }
        val sortKeys = sortMode.keys

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
                text = stringResource(R.string.sort_by_label),
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
                items(sortKeys) { item ->
                    val isSelected = item.key == currentKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sortMode.selectedKey = item.key
                                currentKey = item.key
                                onSortChanged()
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(item.title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

            if (currentKey != SortKey.Custom) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Descending Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newDesc = !isDescending
                            sortMode.selectedDescending = newDesc
                            isDescending = newDesc
                            onSortChanged()
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.descending_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isDescending,
                        onCheckedChange = { checked ->
                            sortMode.selectedDescending = checked
                            isDescending = checked
                            onSortChanged()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "SortBottomSheetDialogFragment"

        fun newInstance(sortMode: SortMode, onSortChanged: () -> Unit): SortBottomSheetDialogFragment {
            return SortBottomSheetDialogFragment().apply {
                this.sortMode = sortMode
                this.onSortChanged = onSortChanged
            }
        }
    }
}
