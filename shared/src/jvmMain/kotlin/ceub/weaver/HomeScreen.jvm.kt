package ceub.weaver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent as AWTKeyEvent
import java.awt.KeyEventDispatcher

import com.martmists.compose.grapheditor.compose.GraphState
import com.martmists.compose.grapheditor.compose.LocalNodeGraphStyle
import com.martmists.compose.grapheditor.compose.NodeGraph
import com.martmists.compose.grapheditor.compose.NodeGraphStyle
import com.martmists.compose.grapheditor.compose.rememberNodeScope
import com.martmists.compose.grapheditor.compose.internal.NodeScope
import com.martmists.compose.grapheditor.data.Graph
import com.martmists.compose.grapheditor.data.Node
import com.martmists.compose.grapheditor.data.NodeDefinition
import com.martmists.compose.grapheditor.data.PortDefinition
import com.martmists.compose.grapheditor.data.PortKind
import com.martmists.compose.grapheditor.data.property.StringProperty
import kotlin.math.roundToInt

data class ColumnDef(val name: String, val type: String)

val databaseTypes = mapOf(
    "PostgreSQL" to listOf(
        "integer", "bigint", "smallint", "serial", "bigserial",
        "numeric", "real", "double precision",
        "varchar", "text",
        "boolean",
        "date", "timestamp", "timestamptz", "interval",
        "uuid", "jsonb", "bytea",
    ),
    "MySQL" to listOf(
        "int", "bigint", "smallint", "tinyint", "serial",
        "decimal", "float", "double",
        "varchar", "char", "text",
        "boolean",
        "date", "timestamp", "datetime",
        "json", "blob",
    ),
)

val databaseNames = databaseTypes.keys.toList()

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
    var isPanActive by remember { mutableStateOf(false) }
    var isCtrlPressed by remember { mutableStateOf(false) }
    val style = remember(isDarkTheme) { databaseTableStyle(isDarkTheme) }

    val tableNodes = graph.nodes.filter { it.definition.name == "Table" }
    var selectedDatabase by remember { mutableStateOf(databaseNames.first()) }
    val nodeColumns = remember { mutableStateMapOf<Any, MutableList<ColumnDef>>() }

    DisposableEffect(Unit) {
        val dispatcher = KeyEventDispatcher { event ->
            if (event.keyCode == AWTKeyEvent.VK_CONTROL) {
                isCtrlPressed = event.id == AWTKeyEvent.KEY_PRESSED
            }
            false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(dispatcher)
        onDispose {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(dispatcher)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bwMaxWidth = maxWidth
        val bwMaxHeight = maxHeight
        CompositionLocalProvider(LocalNodeGraphStyle provides style) {
            Box(Modifier.fillMaxSize()) {
                NodeGraph(
                    state = state,
                    modifier = Modifier
                        .requiredWidth(bwMaxWidth)
                        .requiredHeight(bwMaxHeight)
                )

                val nodeScope = rememberNodeScope(state)
                TableNodeAttributePanel(
                    tableNodes = tableNodes,
                    nodeScope = nodeScope,
                    state = state,
                    style = style,
                    nodeColumns = nodeColumns,
                    selectedDatabase = selectedDatabase,
                )
            }
        }

        if (isPanActive || isCtrlPressed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            state.panOffset += dragAmount
                        }
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(style.colors.sidebar.copy(alpha = 0.95f))
                .border(1.dp, style.colors.sidebarBorder, RoundedCornerShape(10.dp))
                .padding(4.dp),
        ) {
            ToolbarButton(
                icon = "\u270B",
                isActive = isPanActive,
                onClick = { isPanActive = !isPanActive },
                style = style,
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

        var dbDropdownExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(style.colors.sidebar.copy(alpha = 0.9f))
                    .border(1.dp, style.colors.sidebarBorder, RoundedCornerShape(8.dp))
                    .clickable { dbDropdownExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    selectedDatabase,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = style.colors.selectionHighlight,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            DropdownMenu(
                expanded = dbDropdownExpanded,
                onDismissRequest = { dbDropdownExpanded = false },
            ) {
                databaseNames.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = if (name == selectedDatabase)
                                            style.colors.selectionHighlight
                                            else style.colors.portName,
                                    fontWeight = if (name == selectedDatabase)
                                            FontWeight.Bold
                                            else FontWeight.Normal,
                                ),
                            )
                        },
                        onClick = {
                            selectedDatabase = name
                            dbDropdownExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TableNodeAttributePanel(
    tableNodes: List<Node<*>>,
    nodeScope: NodeScope,
    state: GraphState,
    style: NodeGraphStyle,
    nodeColumns: MutableMap<Any, MutableList<ColumnDef>>,
    selectedDatabase: String,
) {
    val density = LocalDensity.current
    val zoom = state.zoom
    val panOffset = state.panOffset

    val scale = density.density * zoom
    val canvasStartX = with(density) { 190.dp.toPx() }
    val defaultType = databaseTypes[selectedDatabase]!!.first()

    tableNodes.forEach { node ->
        val columns = nodeColumns.getOrPut(node) { mutableStateListOf() }
        val nodeSz = with(nodeScope) { node.size() }

        val screenX = node.position.x * scale + panOffset.x + canvasStartX
        val screenY = node.position.y * scale + panOffset.y
        val screenW = nodeSz.width * scale
        val screenH = nodeSz.height * scale

        val panelX = screenX.roundToInt()
        val panelY = (screenY + screenH).roundToInt()

        val panelWidthDp = with(density) { screenW.toDp() }

        val bgColor = style.colors.nodeDefault
        val columnTextStyle = style.typography.portName.copy(color = style.colors.portName)

        Box(
            modifier = Modifier
                .offset { IntOffset(panelX, panelY) }
                .width(panelWidthDp),
        ) {
            Column(
                modifier = Modifier
                    .width(panelWidthDp)
                    .clip(RoundedCornerShape(0.dp, 0.dp, 4.dp, 4.dp))
                    .background(bgColor)
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .padding(bottom = 16.dp),
            ) {
                columns.forEachIndexed { i, col ->
                    var expanded by remember { mutableStateOf(false) }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    var dragOffset by remember { mutableStateOf(0f) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hoverable(interactionSource)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val threshold = with(density) { 25.dp.toPx() }
                                        val idx = columns.indexOf(col)
                                        if (idx < 0) return@detectDragGesturesAfterLongPress
                                        if (dragOffset > threshold && idx > 0) {
                                            val tmp = columns[idx]
                                            columns[idx] = columns[idx - 1]
                                            columns[idx - 1] = tmp
                                            dragOffset = 0f
                                        } else if (dragOffset < -threshold && idx < columns.lastIndex) {
                                            val tmp = columns[idx]
                                            columns[idx] = columns[idx + 1]
                                            columns[idx + 1] = tmp
                                            dragOffset = 0f
                                        }
                                    },
                                    onDragEnd = { dragOffset = 0f },
                                    onDragCancel = { dragOffset = 0f },
                                )
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            col.name.ifEmpty { "new_column" },
                            style = columnTextStyle,
                            modifier = Modifier.weight(1f),
                        )

                        Box {
                            Text(
                                col.type,
                                style = columnTextStyle,
                                modifier = Modifier
                                    .clickable { expanded = true }
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(style.colors.nodeBorder.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                var searchQuery by remember { mutableStateOf("") }
                                val filteredTypes = remember(searchQuery) {
                                    databaseTypes[selectedDatabase]!!.filter {
                                        it.contains(searchQuery, ignoreCase = true)
                                    }
                                }
                                var windowStart by remember { mutableStateOf(0) }

                                LaunchedEffect(filteredTypes.size) {
                                    val maxStart = (filteredTypes.size - 3).coerceAtLeast(0)
                                    if (windowStart > maxStart) windowStart = maxStart
                                }

                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        windowStart = 0
                                    },
                                    placeholder = {
                                        Text("search...", style = columnTextStyle)
                                    },
                                    textStyle = columnTextStyle,
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                )

                                val visibleTypes = filteredTypes.drop(windowStart).take(3)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp)
                                        .pointerInput(filteredTypes.size) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    if (event.type == PointerEventType.Scroll) {
                                                        val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                        val maxStart = (filteredTypes.size - 3).coerceAtLeast(0)
                                                        if (deltaY > 0f && windowStart < maxStart) {
                                                            windowStart++
                                                        } else if (deltaY < 0f && windowStart > 0) {
                                                            windowStart--
                                                        }
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            }
                                        },
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 2.dp),
                                    ) {
                                        visibleTypes.forEach { type ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .clickable {
                                                        columns[i] = col.copy(type = type)
                                                        expanded = false
                                                    }
                                                    .padding(horizontal = 12.dp),
                                                contentAlignment = Alignment.CenterStart,
                                            ) {
                                                Text(type, style = columnTextStyle)
                                            }
                                            if (type != visibleTypes.last()) {
                                                Spacer(Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(4.dp))

                        if (isHovered) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val list = nodeColumns[node]
                                        if (list != null) {
                                            list.removeAt(i)
                                        }
                                    }
                                    .background(style.colors.propertyFieldWarn.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "\u00D7",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = style.colors.propertyFieldWarn,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(style.colors.selectionHighlight)
                    .clickable {
                        val idx = columns.size + 1
                        columns.add(ColumnDef("column_$idx", defaultType))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit,
    style: NodeGraphStyle,
) {
    val activeModifier = if (isActive) {
        Modifier
            .background(style.colors.selectionHighlight.copy(alpha = 0.15f))
            .border(1.dp, style.colors.selectionHighlight, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(activeModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, style = TextStyle(fontSize = 18.sp))
    }
}
