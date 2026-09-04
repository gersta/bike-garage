package de.gerritstapper.bikegarage.feature.part

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.gerritstapper.bikegarage.R

@Composable
internal fun PartDeletionDialog(
    state: PartDeletionUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!state.showConfirmation) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.part_delete_confirmation_title)) },
        text = {
            Text(
                stringResource(
                    R.string.part_delete_confirmation_message,
                    state.part?.name.orEmpty(),
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !state.isDeleting,
            ) {
                Text(stringResource(R.string.part_delete_confirmation_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isDeleting,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
