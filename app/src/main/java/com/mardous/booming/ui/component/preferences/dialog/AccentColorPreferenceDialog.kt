package com.mardous.booming.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.core.model.theme.AccentColor
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.Preferences

class AccentColorPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentAccent = Preferences.accentColor

        return MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        BoomingMusicTheme {
                            AccentColorDialogContent(
                                selectedAccent = currentAccent,
                                onAccentSelected = { selectedColor ->
                                    if (selectedColor != Preferences.accentColor) {
                                        Preferences.accentColor = selectedColor
                                        dismiss()
                                        requireActivity().recreate()
                                    } else {
                                        dismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            )
            .create()
    }

    @Composable
    private fun AccentColorDialogContent(
        selectedAccent: AccentColor,
        onAccentSelected: (AccentColor) -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_palette_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.accent_color_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color choices list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(AccentColor.entries) { accent ->
                        val isSelected = accent == selectedAccent
                        AccentColorItem(
                            accent = accent,
                            isSelected = isSelected,
                            onClick = { onAccentSelected(accent) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AccentColorItem(
        accent: AccentColor,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color Circle Swatch
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.previewColor),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24dp),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Color Name
            Text(
                text = stringResource(accent.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Selected Radio
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}
