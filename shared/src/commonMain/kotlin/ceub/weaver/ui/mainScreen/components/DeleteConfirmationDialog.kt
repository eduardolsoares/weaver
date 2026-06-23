package ceub.weaver.ui.mainScreen.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ceub.weaver.domain.model.ProjectResponse

@Composable
fun DeleteConfirmationDialog(
    project: ProjectResponse?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (project == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B), // Slate-800
        title = {
            Text(
                text = "Excluir Projeto",
                color = Color(0xFFF1F5F9),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "Tem certeza que deseja deletar o projeto \"${project.name}\"?",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Deletar", color = Color(0xFFEF4444))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}