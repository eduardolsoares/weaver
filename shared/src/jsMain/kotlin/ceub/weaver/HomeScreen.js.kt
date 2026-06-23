package ceub.weaver

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ceub.weaver.domain.model.GraphSnapshot

@Composable
actual fun HomeScreen(
    onBackToMain: () -> Unit,
    projectId: String,
    onSaveGraph: suspend (String, GraphSnapshot) -> Boolean,
    onLoadGraph: suspend (String) -> GraphSnapshot?,
) {
    Row {
        Text("<")
        Text("BEM VINDO AO WEAVER STUDIO.")
    }
}
