package com.simplemobiletools.commons.compose.alert_dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AlertDialogButtons(
    positiveButton: String,
    onPositivePressed: () -> Unit,
    negativeButton: String?,
    onNegativePressed: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (negativeButton != null) {
            TextButton(onClick = { onNegativePressed?.invoke() }) {
                Text(text = negativeButton)
            }
        }
        TextButton(onClick = onPositivePressed) {
            Text(text = positiveButton))
        }
    }
}
