package org.mexa.clube.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.mexa.clube.ui.theme.Pro

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "O'chirish",
    dismissText: String = "Bekor qilish",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Pro.TextMain, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Pro.TextDim) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) { Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = Pro.TextDim) }
        }
    )
}
