package ceub.weaver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.AWTEvent
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent as AWTKeyEvent
import java.awt.event.MouseEvent as AWTMouseEvent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ColumnDef(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean = false,
    val isNotNull: Boolean = false,
    val isUnique: Boolean = false,
)

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

val typeMapping = mapOf(
    "PostgreSQL->MySQL" to mapOf(
        "integer" to "int", "bigint" to "bigint", "smallint" to "smallint",
        "serial" to "serial", "bigserial" to "serial",
        "numeric" to "decimal", "real" to "float", "double precision" to "double",
        "varchar" to "varchar", "text" to "text",
        "boolean" to "boolean",
        "date" to "date", "timestamp" to "timestamp", "timestamptz" to "timestamp",
        "interval" to "", "uuid" to "", "jsonb" to "json", "bytea" to "blob",
    ),
    "MySQL->PostgreSQL" to mapOf(
        "int" to "integer", "bigint" to "bigint", "smallint" to "smallint",
        "tinyint" to "smallint", "serial" to "serial",
        "decimal" to "numeric", "float" to "real", "double" to "double precision",
        "varchar" to "varchar", "char" to "varchar", "text" to "text",
        "boolean" to "boolean",
        "date" to "date", "timestamp" to "timestamp", "datetime" to "timestamp",
        "json" to "jsonb", "blob" to "bytea",
    ),
)

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
    var isRightClicking by remember { mutableStateOf(false) }
    var showDdl by remember { mutableStateOf(false) }
    val style = remember(isDarkTheme) { databaseTableStyle(isDarkTheme) }

    val tableNodes = graph.nodes.filter { it.definition.name == "Table" }
    var selectedDatabase by remember { mutableStateOf(databaseNames.first()) }
    val nodeColumns = remember { mutableStateMapOf<Any, MutableList<ColumnDef>>() }

    var previousDatabase by remember { mutableStateOf(selectedDatabase) }
    LaunchedEffect(selectedDatabase) {
        if (selectedDatabase == previousDatabase) return@LaunchedEffect
        if (nodeColumns.isEmpty()) return@LaunchedEffect
        val mappingKey = "${previousDatabase}->${selectedDatabase}"
        val mapping = typeMapping[mappingKey] ?: return@LaunchedEffect
        val oldTypes = databaseTypes[previousDatabase] ?: return@LaunchedEffect
        for ((node, cols) in nodeColumns) {
            for (i in cols.indices) {
                val col = cols[i]
                if (col.type in oldTypes) {
                    val newType = mapping[col.type] ?: col.type
                    cols[i] = col.copy(type = newType)
                }
            }
        }
        previousDatabase = selectedDatabase
    }

    DisposableEffect(Unit) {
        val dispatcher = KeyEventDispatcher { event ->
            if (event.keyCode == AWTKeyEvent.VK_CONTROL) {
                isCtrlPressed = event.id == AWTKeyEvent.KEY_PRESSED
            }
            false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(dispatcher)

        val mouseListener = AWTEventListener { event ->
            if (event is AWTMouseEvent) {
                when (event.id) {
                    AWTMouseEvent.MOUSE_PRESSED -> {
                        if (event.button == AWTMouseEvent.BUTTON3) {
                            isRightClicking = true
                        }
                    }
                    AWTMouseEvent.MOUSE_RELEASED, AWTMouseEvent.MOUSE_EXITED -> {
                        if (event.button == AWTMouseEvent.BUTTON3) {
                            isRightClicking = false
                        }
                    }
                }
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(mouseListener, AWTEvent.MOUSE_EVENT_MASK)

        onDispose {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(dispatcher)
            Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener)
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
                    isRightClicking = isRightClicking,
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

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDdl = !showDdl }
                    .background(if (showDdl) style.colors.selectionHighlight.copy(alpha = 0.2f) else style.colors.sidebar.copy(alpha = 0.9f))
                    .border(1.dp, if (showDdl) style.colors.selectionHighlight else style.colors.sidebarBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "SQL",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showDdl) style.colors.selectionHighlight else style.colors.portName,
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 60.dp, end = 12.dp, bottom = 12.dp),
        ) {
            AnimatedVisibility(
                visible = showDdl,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                val ddlText = remember(nodeColumns, graph, selectedDatabase) {
                    NodeParser.generate(graph, nodeColumns, selectedDatabase)
                }
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(style.colors.nodeDefault)
                        .border(1.dp, style.colors.nodeBorder, RoundedCornerShape(8.dp)),
                ) {
                    val scope = rememberCoroutineScope()
                    var showCopiedToast by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxSize().padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(style.colors.sidebar.copy(alpha = 0.3f))
                                .border(1.dp, style.colors.nodeBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                                .verticalScroll(scrollState),
                        ) {
                            Text(
                                ddlText.ifEmpty { "-- No tables or enums defined" },
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = style.colors.portName,
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(style.colors.sidebar.copy(alpha = 0.9f))
                                .border(1.dp, style.colors.sidebarBorder, RoundedCornerShape(4.dp))
                                .clickable {
                                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                    clipboard.setContents(StringSelection(ddlText), null)
                                    showCopiedToast = true
                                    scope.launch {
                                        delay(2000)
                                        showCopiedToast = false
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(14.dp)) {
                                val c = style.colors.portName
                                val w = size.width
                                val h = size.height
                                val pad = w * 0.15f
                                val clipW = w - pad * 2
                                val clipH = h - pad * 2
                                val topH = clipH * 0.3f
                                val bodyH = clipH - topH

                                drawRoundRect(c, topLeft = Offset(pad, pad), size = Size(clipW, clipH), cornerRadius = CornerRadius(w * 0.15f, w * 0.15f), style = Stroke(w * 0.12f))
                                drawRoundRect(c, topLeft = Offset(pad, pad + topH), size = Size(clipW, bodyH), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f), style = Stroke(w * 0.12f))
                                val innerPad = w * 0.3f
                                val lineY1 = pad + topH + bodyH * 0.3f
                                val lineY2 = pad + topH + bodyH * 0.55f
                                val lineY3 = pad + topH + bodyH * 0.8f
                                drawLine(c, Offset(innerPad, lineY1), Offset(w - innerPad, lineY1), strokeWidth = w * 0.08f)
                                drawLine(c, Offset(innerPad, lineY2), Offset(w - innerPad, lineY2), strokeWidth = w * 0.08f)
                                drawLine(c, Offset(innerPad, lineY3), Offset(w - innerPad, lineY3), strokeWidth = w * 0.08f)
                            }
                        }

                        if (showCopiedToast) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(y = 30.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(style.colors.selectionHighlight.copy(alpha = 0.9f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "Copied!",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = style.colors.nodeDefault,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
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
    isRightClicking: Boolean,
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
                    var contextExpanded by remember { mutableStateOf(false) }
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
                        Box(Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    col.name.ifEmpty { "new_column" },
                                    style = columnTextStyle,
                                )

                                if (col.isPrimaryKey) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "PK",
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = style.colors.selectionHighlight,
                                        ),
                                    )
                                }
                                if (col.isNotNull) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "NN",
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = style.colors.propertyFieldWarn,
                                        ),
                                    )
                                }
                                if (col.isUnique) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "UQ",
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = style.colors.portName,
                                        ),
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Press && isRightClicking) {
                                                    contextExpanded = true
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        }
                                    },
                            )

                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = style.colors.nodeDefault,
                                    onSurface = style.colors.portName,
                                ),
                            ) {
                                DropdownMenu(
                                    expanded = contextExpanded,
                                    onDismissRequest = { contextExpanded = false },
                                ) {
                                    val toggle = { flag: Boolean -> !flag }
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = col.isPrimaryKey,
                                                    onCheckedChange = null,
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Primary Key", style = columnTextStyle)
                                            }
                                        },
                                        onClick = {
                                            columns[i] = col.copy(isPrimaryKey = toggle(col.isPrimaryKey))
                                            contextExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = col.isNotNull,
                                                    onCheckedChange = null,
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Not Null", style = columnTextStyle)
                                            }
                                        },
                                        onClick = {
                                            columns[i] = col.copy(isNotNull = toggle(col.isNotNull))
                                            contextExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = col.isUnique,
                                                    onCheckedChange = null,
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Unique", style = columnTextStyle)
                                            }
                                        },
                                        onClick = {
                                            columns[i] = col.copy(isUnique = toggle(col.isUnique))
                                            contextExpanded = false
                                        },
                                    )
                                }
                            }
                        }

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
                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = style.colors.nodeDefault,
                                    onSurface = style.colors.portName,
                                ),
                            ) {
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

                                    Box(Modifier.fillMaxWidth().background(style.colors.nodeDefault)) {
                                        Column(Modifier.fillMaxWidth()) {
                                            TextField(
                                                value = searchQuery,
                                                onValueChange = {
                                                    searchQuery = it
                                                    windowStart = 0
                                                },
                                                placeholder = {
                                                    Text("search...", style = columnTextStyle)
                                                },
                                                textStyle = columnTextStyle.copy(color = style.colors.portName),
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedIndicatorColor = style.colors.selectionHighlight.copy(alpha = 0.5f),
                                                    unfocusedIndicatorColor = style.colors.nodeBorder.copy(alpha = 0.3f),
                                                    focusedTextColor = style.colors.portName,
                                                    unfocusedTextColor = style.colors.portName,
                                                    cursorColor = style.colors.selectionHighlight,
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
