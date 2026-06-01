package ceub.weaver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.martmists.compose.grapheditor.compose.GraphState
import com.martmists.compose.grapheditor.compose.LocalNodeGraphStyle
import com.martmists.compose.grapheditor.compose.NodeGraph
import com.martmists.compose.grapheditor.data.Graph
import com.martmists.compose.grapheditor.data.NodeDefinition
import com.martmists.compose.grapheditor.data.PortDefinition
import com.martmists.compose.grapheditor.data.PortKind
import com.martmists.compose.grapheditor.data.property.StringProperty

@Composable
actual fun HomeScreen() {
    val tableDef = remember {
        NodeDefinition(
            name = "Table",
            ports = listOf(
                PortDefinition("FK in", PortKind.Input, dataType = "table"),
                PortDefinition("FK out", PortKind.Output, dataType = "table"),
            ),
            properties = listOf(
                StringProperty("Name", "new_table"),
                StringProperty("Schema", "public"),
                StringProperty("Comment", ""),
            )
        )
    }
    val enumDef = remember {
        NodeDefinition(
            name = "Enum",
            ports = listOf(
                PortDefinition("ref", PortKind.Output, dataType = "enum"),
            ),
            properties = listOf(
                StringProperty("Name", "new_enum"),
                StringProperty("Values", ""),
            )
        )
    }
    val noteDef = remember {
        NodeDefinition(
            name = "Note",
            properties = listOf(
                StringProperty("Text", ""),
            )
        )
    }

    val definitions = remember { listOf(tableDef, enumDef, noteDef) }
    val graph = remember { Graph<Unit>(definitions) }

    LaunchedEffect(Unit) {
        val table1 = graph.addNode(tableDef, Offset(100f, 100f))
        val table2 = graph.addNode(tableDef, Offset(500f, 200f))
        graph.tryConnect(
            table1, table1.definition.outputPorts.first(),
            table2, table2.definition.inputPorts.first()
        )
    }

    val state = remember { GraphState(graph) }
    var isDarkTheme by remember { mutableStateOf(true) }
    val style = remember(isDarkTheme) { databaseTableStyle(isDarkTheme) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNodeGraphStyle provides style) {
            NodeGraph(
                state = state,
                modifier = Modifier
                    .requiredWidth(maxWidth)
                    .requiredHeight(maxHeight)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable { isDarkTheme = !isDarkTheme }
                .background(style.colors.sidebar.copy(alpha = 0.9f))
                .border(1.dp, style.colors.sidebarBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isDarkTheme) "\u2600\uFE0F" else "\uD83C\uDF19",
                style = TextStyle(fontSize = 16.sp),
            )
        }
    }
}
