package ceub.weaver

import androidx.compose.runtime.Composable
import ceub.weaver.domain.model.GraphSnapshot

@Composable
expect fun HomeScreen(
    onBackToMain: () -> Unit = {},
    projectId: String = "",
    onSaveGraph: suspend (String, GraphSnapshot) -> Boolean = { _, _ -> true },
    onLoadGraph: suspend (String) -> GraphSnapshot? = { null },
)
