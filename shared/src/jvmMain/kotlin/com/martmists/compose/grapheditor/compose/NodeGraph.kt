package com.martmists.compose.grapheditor.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProviderAtPosition
import com.martmists.compose.grapheditor.compose.internal.CONNECTION_WIDTH
import com.martmists.compose.grapheditor.compose.internal.NODE_CLICK_TIMEOUT
import com.martmists.compose.grapheditor.compose.internal.NodeCanvasScope
import com.martmists.compose.grapheditor.compose.internal.NodeScope
import com.martmists.compose.grapheditor.compose.internal.NodeScopeImpl
import com.martmists.compose.grapheditor.compose.internal.drawConnection
import com.martmists.compose.grapheditor.compose.internal.drawGrid
import com.martmists.compose.grapheditor.compose.internal.drawNode
import com.martmists.compose.grapheditor.data.property.BoolProperty
import com.martmists.compose.grapheditor.data.property.ChoiceProperty
import com.martmists.compose.grapheditor.data.Connection
import com.martmists.compose.grapheditor.data.property.FloatProperty
import com.martmists.compose.grapheditor.data.Graph
import com.martmists.compose.grapheditor.data.GraphEvent
import com.martmists.compose.grapheditor.data.property.IntProperty
import com.martmists.compose.grapheditor.data.Node
import com.martmists.compose.grapheditor.data.NodeDefinition
import com.martmists.compose.grapheditor.data.PortDefinition
import com.martmists.compose.grapheditor.data.PortKind
import com.martmists.compose.grapheditor.data.property.PropertyDefinition
import com.martmists.compose.grapheditor.data.property.StringProperty
import kotlinx.coroutines.delay
import java.awt.Cursor
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

internal enum class Action {
    IDLE,
    DRAGGING_DEFINITION,
    DRAGGING_NODES,
    DRAGGING_NODE_IMMEDIATE,
    DRAWING_LINE,
    SELECTING_AREA,
    PANNING
}

@Suppress("UNCHECKED_CAST")
internal fun Graph<*>.removeNode(node: Node<*>, isUndo: Boolean = false) {
    (this as Graph<Unit>).removeNode(node as Node<Unit>, isUndo)
}

@Composable
fun rememberNodeScope(state: GraphState): NodeScope {
    val style = LocalNodeGraphStyle.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    return remember(state, style, density) { NodeScopeImpl(state, density, style, measurer) }
}

@Stable
class GraphState(
    val graph: Graph<*>,
    internal val undoQueueSize: Int = 50
) {
    // TODO: Find a way to animate scale/pan smoothly *unless* panning, which needs to be immediate
    var panOffset by mutableStateOf(Offset.Zero)
    var zoom by mutableStateOf(1f)

    // Cursor pos
    internal var cursorPosition by mutableStateOf(Offset.Zero)
    internal var canvasRect by mutableStateOf(Rect(Offset.Zero, Size(1f, 1f)))

    // Action start pos
    internal var actionStartPosition by mutableStateOf(Offset.Zero)
    internal var actionStartTime by mutableStateOf(Clock.System.now())

    // Selected node (properties view only)
    internal var detailsNode by mutableStateOf<Node<*>?>(null)

    // Selected definition (dragging from palette)
    internal var queueAdd by mutableStateOf(false)
    internal var selectedDefinition by mutableStateOf<NodeDefinition?>(null)

    // Selected elements (for moving, etc)
    internal var selectionNodes by mutableStateOf<Set<Node<*>>>(emptySet())
    internal var selectionConnections by mutableStateOf<Set<Connection>>(emptySet())

    // Current cursor action
    internal var currentAction by mutableStateOf(Action.IDLE)

    // Context menu
    internal var contextMenuOpen by mutableStateOf(false)
    internal var contextMenuPos by mutableStateOf(Offset.Zero)

    // Queue of stuff to undo
    internal val undoQueue = mutableListOf<GraphEvent>()

    fun deleteSelection() {
        for (conn in selectionConnections) {
            graph.disconnect(conn)
        }
        for (node in selectionNodes) {
            if (node == detailsNode) {
                detailsNode = null
            }
            graph.removeNode(node)
        }
        selectionNodes = emptySet()
        selectionConnections = emptySet()
    }

    context(scope: NodeScope)
    fun fitToView(density: Float) {
        val nodes = graph.nodes
        if (nodes.isEmpty()) return
        val fitPadding = 40
        val nodeRects = nodes.map {
            Rect(it.position, with(scope) { it.size() })
        }
        val minX = nodeRects.minOf { it.left } - fitPadding
        val minY = nodeRects.minOf { it.top } - fitPadding
        val maxX = nodeRects.maxOf { it.right } + fitPadding
        val maxY = nodeRects.maxOf { it.bottom } + fitPadding
        zoom = minOf(canvasRect.width / ((maxX - minX) * density), canvasRect.height / ((maxY - minY) * density), 2.0f)
        val offX = canvasRect.width / 2f - ((minX + maxX) / 2f) * zoom * density
        val offY = canvasRect.height / 2f - ((minY + maxY) / 2f) * zoom * density
        panOffset = Offset(offX, offY)
    }

    // TODO: Support redo
    fun undo() {
        if (undoQueue.isEmpty()) return

        when (val toUndo = undoQueue.removeLast()) {
            is GraphEvent.ConnectionCreate -> {
                graph.disconnect(toUndo.connection, true)
            }
            is GraphEvent.ConnectionDelete -> {
                graph.tryConnect(
                    toUndo.connection.fromNode,
                    toUndo.connection.fromPort,
                    toUndo.connection.toNode,
                    toUndo.connection.toPort,
                    true,
                )
            }
            is GraphEvent.NodeCreate -> {
                graph.removeNode(toUndo.node, true)
            }
            is GraphEvent.NodeDelete -> {
                @Suppress("UNCHECKED_CAST")
                (graph as Graph<Unit>).addNodeInternal(toUndo.node as Node<Unit>, true)
            }
            is GraphEvent.NodePropertyUpdate -> {
                @Suppress("UNCHECKED_CAST")
                (graph as Graph<Unit>).updateProperty(toUndo.node as Node<Unit>, toUndo.property, toUndo.oldValue, true)
            }
        }
    }
}

@Composable
fun NodeGraph(
    state: GraphState,
    modifier: Modifier = Modifier,
) {
    val style = LocalNodeGraphStyle.current
    val density = LocalDensity.current

    var paletteWidth by remember { mutableStateOf(185f) }
    var propsWidth by remember { mutableStateOf(240f) }
    val isSidebarOpen by remember {
        derivedStateOf {
            state.detailsNode != null
        }
    }

    LaunchedEffect(Unit) {
        state.graph.events.collect { event ->
            if (event.isUndo) return@collect

            state.undoQueue.add(event)
            if (state.undoQueue.size > state.undoQueueSize) {
                state.undoQueue.removeAt(0)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = style.colors.background)
    ) {
        // TODO: Maybe allow custom grouping and/or layout with a composable?
        // TODO: Scrollbar styling
        val defListState = rememberLazyListState()
        LazyColumn(modifier = Modifier.width(paletteWidth.dp), state=defListState) {
            stickyHeader {
                PanelHeader("Nodes")
            }

            items(state.graph.definitions) {
                NodeGraphDefinition(state, it)
            }
        }
        ResizeHandle { delta ->
            val deltaDp = with(density) { delta.toDp().value }
            paletteWidth = (paletteWidth + deltaDp).coerceIn(80f, 500f)  // TODO: Style min/max width
        }
        NodeGraphCanvas(Modifier.weight(1f).fillMaxHeight()
            .clipToBounds(), state)

        // TODO: Only animate when toggled, not when resizing with the handle
        val width by animateDpAsState(if (isSidebarOpen) propsWidth.dp else 0.dp)
        val visible by derivedStateOf {
            width != 0.dp
        }
        if (visible) {
            ResizeHandle { delta ->
                val deltaDp = with(density) { delta.toDp().value }
                propsWidth = (propsWidth - deltaDp).coerceIn(80f, 500f)  // TODO: Style min/max width
            }
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(width)
                    .background(style.colors.sidebar)
            ) {
                // TODO: Maybe allow custom composables below properties?
                state.detailsNode?.let { node ->
                    PanelHeader(node.definition.name)

                    // TODO: Scrollbar styling
                    val propListState = rememberLazyListState()
                    LazyColumn(modifier=Modifier.fillMaxSize().padding(16.dp), state=propListState, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        item {
                            // TODO: Renames are not handled by undo logic
                            NodeGraphProp("name", node.name, { it.isNotBlank() }, {
                                node.name = it.trim()
                                node.computedSize = null
                            })
                        }

                        item {
                            Text("PORTS", style = style.typography.propertyHeaderText.copy(style.colors.propertyHeaderText))
                        }

                        item {
                            Text(
                                buildAnnotatedString {
                                    fun ports(prefix: String, portList: List<PortDefinition>) {
                                        if (portList.isNotEmpty()) {
                                            val firstType = portList.first().dataType
                                            if (portList.all { it.dataType == firstType }) {
                                                withStyle(SpanStyle(color = style.colors.portMap[firstType] ?: style.colors.portDefault)) {
                                                    if (firstType != "any") {
                                                        append("$prefix ($firstType): ")
                                                    } else {
                                                        append("$prefix: ")
                                                    }
                                                    for ((i, p) in portList.withIndex()) {
                                                        append(p.name)
                                                        if (i != portList.lastIndex) {
                                                            append(", ")
                                                        }
                                                    }
                                                }
                                            } else {
                                                appendLine("$prefix:")
                                                val groups = portList.groupBy { it.dataType }.toList()
                                                for ((g, e) in groups.withIndex()) {
                                                    val (type, ports) = e
                                                    withStyle(SpanStyle(color = style.colors.portMap[type] ?: style.colors.portDefault)) {
                                                        append("  ($type): ")
                                                        for ((i, p) in ports.withIndex()) {
                                                            append(p.name)
                                                            if (i != ports.lastIndex) {
                                                                append(", ")
                                                            } else if (g != groups.lastIndex) {
                                                                appendLine()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    ports("IN", node.definition.inputPorts)
                                    if (node.definition.inputPorts.isNotEmpty()) appendLine()
                                    ports("OUT", node.definition.outputPorts)
                                },
                                style = style.typography.propertyFieldName.copy(color = style.colors.propertyFieldName)
                            )
                        }

                        item {
                            Text("PROPERTIES", style = style.typography.propertyHeaderText.copy(color = style.colors.propertyHeaderText))
                        }

                        items(node.definition.properties) {
                            NodeGraphProp(node, it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PanelHeader(title: String) {
    val style = LocalNodeGraphStyle.current

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(style.colors.sidebarHeader)
            .padding(20.dp)
    ) {
        Text(title, style=style.typography.sidebarHeaderText.copy(style.colors.sidebarHeaderText))
    }
}

@Composable
fun ResizeHandle(onDelta: (Float) -> Unit) {
    val style = LocalNodeGraphStyle.current
    Box(
        modifier = Modifier
            .width(5.dp)
            .fillMaxHeight()
            .background(style.colors.sidebarBorder)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDelta(drag.x)
                }
            }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NodeGraphCanvas(modifier: Modifier, state: GraphState) {
    val style = LocalNodeGraphStyle.current
    val densityObj = LocalDensity.current

    val nodeScope = rememberNodeScope(state)

    LaunchedEffect(state.queueAdd) {
        if (state.queueAdd) {
            val node = state.graph.addNode(state.selectedDefinition!!, nodeScope.screenToWorld(state.cursorPosition))
            with (nodeScope) {
                node.position -= Offset(node.size().width / 2, node.size().height / 2)
            }
            state.selectedDefinition = null
            state.queueAdd = false
        }
    }

    val requester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        requester.requestFocus()
    }

    Box(modifier) {
        Canvas(
            modifier
                .fillMaxSize()
                .clipToBounds()
                .onGloballyPositioned {
                    state.canvasRect = Rect(it.positionInWindow(), it.size.toSize())
                }
                .focusRequester(requester)
                .focusable()
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.F -> {
                                with (nodeScope) {
                                    state.fitToView(densityObj.density)
                                }
                            }
                            Key.Z -> {
                                if (it.isCtrlPressed) {
                                    state.undo()
                                }
                            }
                            Key.Delete -> {
                                state.detailsNode?.let {
                                    state.selectionNodes += setOf(it)
                                }
                                state.deleteSelection()
                            }
                            else -> return@onKeyEvent false
                        }
                        return@onKeyEvent true
                    }
                    false
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            val pos = change.position

                            when (event.type) {
                                PointerEventType.Scroll -> {
                                    val delta = change.scrollDelta.y
                                    val factor = if (delta < 0) 1.12f else 1f / 1.12f
                                    val ns = (state.zoom * factor).coerceIn(0.15f, 3f)  // TODO: Style
                                    val nx = pos.x - (pos.x - state.panOffset.x) * (ns / state.zoom)
                                    val ny = pos.y - (pos.y - state.panOffset.y) * (ns / state.zoom)
                                    state.panOffset = Offset(nx, ny)
                                    state.zoom = ns
                                }

                                PointerEventType.Press -> {
                                    requester.requestFocus()

                                    when (event.button) {
                                        PointerButton.Primary -> {
                                            if (state.selectionNodes.isEmpty() && nodeScope.findPortAtPos(pos) != null) {
                                                state.detailsNode = null
                                                state.currentAction = Action.DRAWING_LINE
                                            } else {
                                                val node = nodeScope.findNodeAtPos(pos)
                                                if (node != null) {
                                                    if (state.selectionNodes.isEmpty()) {
                                                        state.currentAction = Action.DRAGGING_NODE_IMMEDIATE
                                                        state.selectionNodes = setOf(node)
                                                        state.selectionConnections = emptySet()
                                                    } else if (node in state.selectionNodes) {
                                                        state.currentAction = Action.DRAGGING_NODES
                                                    }
                                                } else {
                                                    state.detailsNode = null
                                                    state.selectionNodes = emptySet()

                                                    val conn = nodeScope.findConnectionAtPos(pos)
                                                    if (conn != null) {
                                                        state.selectionConnections = setOf(conn)
                                                    } else {
                                                        state.selectionConnections = emptySet()
                                                        state.currentAction = Action.SELECTING_AREA
                                                    }
                                                }
                                            }
                                            state.actionStartPosition = pos
                                            state.actionStartTime = Clock.System.now()
                                            state.contextMenuOpen = false
                                        }

                                        PointerButton.Secondary -> {
                                            state.contextMenuPos = pos
                                            state.contextMenuOpen = true

                                            if (state.selectionNodes.isEmpty() && state.selectionConnections.isEmpty()) {
                                                val node = nodeScope.findNodeAtPos(pos)
                                                if (node != null) {
                                                    state.selectionNodes = setOf(node)
                                                } else {
                                                    val conn = nodeScope.findConnectionAtPos(pos)
                                                    if (conn != null) {
                                                        state.selectionConnections = setOf(conn)
                                                    }
                                                }
                                            }
                                        }

                                        PointerButton.Tertiary -> {
                                            state.currentAction = Action.PANNING
                                            state.contextMenuOpen = false
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    when (state.currentAction) {
                                        Action.PANNING -> {
                                            state.panOffset += pos - state.cursorPosition
                                        }

                                        Action.DRAGGING_NODE_IMMEDIATE, Action.DRAGGING_NODES -> {
                                            for (node in state.selectionNodes) {
                                                node.position += nodeScope.screenToWorld(pos) - nodeScope.screenToWorld(state.cursorPosition)
                                            }
                                        }

                                        Action.SELECTING_AREA -> {
                                            val inSelect = mutableSetOf<Node<*>>()

                                            val tl = nodeScope.screenToWorld(
                                                Offset(
                                                    min(state.actionStartPosition.x, pos.x),
                                                    min(state.actionStartPosition.y, pos.y),
                                                )
                                            )
                                            val br = nodeScope.screenToWorld(
                                                Offset(
                                                    max(state.actionStartPosition.x, pos.x),
                                                    max(state.actionStartPosition.y, pos.y),
                                                )
                                            )

                                            val selectionRect = Rect(tl, br)

                                            for (node in state.graph.nodes) {
                                                val nodeRect = with (nodeScope) { Rect(node.position, node.size()) }
                                                if (nodeRect.overlaps(selectionRect)) {
                                                    inSelect.add(node)
                                                }
                                            }

                                            val inSelectConn = nodeScope.findConnectionsInBox(selectionRect).toSet()

                                            if (state.selectionNodes != inSelect) {
                                                state.selectionNodes = inSelect
                                            }
                                            if (state.selectionConnections != inSelectConn) {
                                                state.selectionConnections = inSelectConn
                                            }
                                        }

                                        Action.IDLE, Action.DRAWING_LINE, Action.DRAGGING_DEFINITION -> { /* Do nothing */
                                        }
                                    }
                                    state.cursorPosition = pos
                                }

                                PointerEventType.Release -> {
                                    when (state.currentAction) {
                                        Action.PANNING, Action.SELECTING_AREA, Action.DRAGGING_NODES, Action.IDLE, Action.DRAGGING_DEFINITION -> { /* Do nothing */
                                        }

                                        Action.DRAGGING_NODE_IMMEDIATE -> {
                                            state.selectionNodes = emptySet()
                                            val now = Clock.System.now()
                                            if (now - state.actionStartTime < NODE_CLICK_TIMEOUT) {
                                                state.detailsNode = nodeScope.findNodeAtPos(pos)
                                            }
                                        }

                                        Action.DRAWING_LINE -> run {
                                            val (sn, sp) = nodeScope.findPortAtPos(state.actionStartPosition) ?: return@run
                                            val (en, ep) = nodeScope.findPortAtPos(pos) ?: return@run
                                            state.graph.tryConnect(sn, sp, en, ep)
                                        }
                                    }
                                    state.currentAction = Action.IDLE
                                }
                            }
                        }
                    }
                }

        ) {
            val drawScope = NodeCanvasScope(this, nodeScope)
            drawScope.apply {
                drawRect(style.colors.background, Offset.Zero, size)

                drawGrid()

                val bounds = Rect(Offset.Zero, size)
                for (node in state.graph.nodes) {
                    if (!bounds.overlaps(Rect(worldToScreen(node.position), worldToScreen(node.size())))) continue
                    drawNode(node)
                }

                for (connection in state.graph.connections) {
                    drawConnection(connection)
                }

                if (state.currentAction == Action.SELECTING_AREA) {
                    val p1 = state.actionStartPosition
                    val p2 = state.cursorPosition
                    val tl = Offset(
                        min(p1.x, p2.x),
                        min(p1.y, p2.y),
                    )
                    val sizeOff = Offset(
                        max(p1.x, p2.x),
                        max(p1.y, p2.y),
                    ) - tl
                    val size = Size(sizeOff.x, sizeOff.y)
                    drawRect(style.colors.selectionRect, topLeft = tl, size = size)
                }

                if (state.currentAction == Action.DRAWING_LINE) run {
                    val (node, port) = findPortAtPos(state.actionStartPosition) ?: return@run
                    val endData = findPortAtPos(state.cursorPosition)

                    val startPos = if (port in node.definition.outputPorts) {
                        node.portOffset(node.definition.outputPorts.indexOf(port), true)
                    } else {
                        node.portOffset(node.definition.inputPorts.indexOf(port), false)
                    }

                    val endPos = if (endData == null) screenToWorld(state.cursorPosition) else run {
                        val (en, ep) = endData
                        if (ep.kind == port.kind) return@run screenToWorld(state.cursorPosition)
                        if (ep in en.definition.outputPorts) {
                            en.portOffset(en.definition.outputPorts.indexOf(ep), true)
                        } else {
                            en.portOffset(en.definition.inputPorts.indexOf(ep), false)
                        }
                    }

                    val color = style.colors.wireMap[port.dataType] ?: style.colors.wireDefault

                    if (port.kind == PortKind.Output) {
                        drawConnection(color, CONNECTION_WIDTH, startPos, endPos)
                    } else {
                        drawConnection(color, CONNECTION_WIDTH, endPos, startPos)
                    }
                }

                if (state.currentAction == Action.DRAGGING_DEFINITION) {
                    val tempNode = Node(state.graph, state.selectedDefinition!!, screenToWorld(state.cursorPosition))
                    tempNode.position -= Offset(tempNode.size().width / 2, tempNode.size().height / 2)
                    drawNode(tempNode)
                }
            }
        }

        if (state.contextMenuOpen) {
            Popup(
                PopupPositionProviderAtPosition(state.contextMenuPos, true, Offset.Zero, windowMarginPx = 2),
                onDismissRequest = {
                    state.contextMenuOpen = false
                }
            ) {
                Column(
                    Modifier
                        .wrapContentHeight()
                        .width(IntrinsicSize.Max)
                        .clip(RoundedCornerShape(4.dp))
                        .background(style.colors.contextMenu)
                        .padding(4.dp)
                ) {
                    if (state.selectionNodes.isEmpty() && state.selectionConnections.isEmpty()) {
                        // Context for canvas
                        ContextMenuButton(state, "Undo", "Ctrl+Z") {
                            state.undo()
                        }
                        // TODO: Auto-layout nodes button?
                        ContextMenuButton(state, "Fit to view", "F") {
                            with (nodeScope) {
                                state.fitToView(densityObj.density)
                            }
                        }
                        ContextMenuButton(state, "Delete everything", "Del", true) {
                            state.selectionConnections = state.graph.connections.toSet()
                            state.selectionNodes = state.graph.nodes.toSet()
                            state.deleteSelection()
                        }
                    } else if (state.selectionNodes.size == 1 && state.selectionConnections.isEmpty()) {
                        // Single node
                        ContextMenuButton(state, "Edit properties") {
                            state.detailsNode = state.selectionNodes.first()
                        }
                        ContextMenuButton(state, "Disconnect all") {
                            for (c in state.selectionNodes.first().connections) {
                                state.graph.disconnect(c)
                            }
                        }
                        ContextMenuButton(state, "Delete", "Del", true) {
                            state.deleteSelection()
                        }
                    } else if (state.selectionConnections.size == 1 && state.selectionNodes.isEmpty()) {
                        // Single connection
                        ContextMenuButton(state, "Delete", "Del", true) {
                            state.deleteSelection()
                        }
                    } else {
                        // Multiple nodes and/or multiple connections
                        ContextMenuButton(state, "Delete all", "Del", true) {
                            state.deleteSelection()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NodeGraphDefinition(state: GraphState, definition: NodeDefinition) {
    var nodePos by remember { mutableStateOf(Offset.Zero) }
    var pressTime by remember { mutableStateOf(Clock.System.now()) }
    val style = LocalNodeGraphStyle.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal=2.dp, vertical=1.dp)
            .background(style.colors.nodeMap[definition.name] ?: style.colors.nodeDefault)
            .padding(4.dp)
            .onGloballyPositioned {
                nodePos = it.positionInWindow()
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (event.button == PointerButton.Primary) {
                                    state.currentAction = Action.DRAGGING_DEFINITION
                                    state.selectedDefinition = definition
                                    pressTime = Clock.System.now()
                                } else continue
                            }
                            PointerEventType.Move -> {
                                state.cursorPosition = nodePos + change.position - state.canvasRect.topLeft
                            }
                            PointerEventType.Release -> {
                                if (state.cursorPosition in Rect(Offset.Zero, state.canvasRect.size)) {
                                    state.queueAdd = true
                                } else if (Clock.System.now() - pressTime < NODE_CLICK_TIMEOUT) {
                                    state.cursorPosition = state.canvasRect.center - state.canvasRect.topLeft
                                    state.queueAdd = true
                                }
                                state.currentAction = Action.IDLE
                            }
                            else -> continue
                        }
                        change.consume()
                    }
                }
            }
    ) {
        Text(definition.name, style = style.typography.paletteNodeName.copy(style.colors.paletteNodeName))
    }
}

@Composable
fun ContextMenuButton(state: GraphState, label: String, shortcut: String? = null, isDanger: Boolean = false, onClick: () -> Unit) {
    val style = LocalNodeGraphStyle.current

    TextButton(
        onClick = {
            state.contextMenuOpen = false
            onClick()
        },
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            label,
            style = if (isDanger) style.typography.contextMenuWarn.copy(color=style.colors.contextMenuWarn)
                    else style.typography.contextMenuText.copy(color=style.colors.contextMenuText),
            modifier = Modifier.weight(1f))
        shortcut?.let {
            Text(
                it,
                style = if (isDanger) style.typography.contextMenuShortcut.copy(color=style.colors.contextMenuWarn)
                        else style.typography.contextMenuShortcut.copy(color=style.colors.contextMenuText),
                modifier = Modifier.padding(start=20.dp)
            )
        }
    }
}

@Composable
fun NodeGraphProp(node: Node<*>, prop: PropertyDefinition<*>) {
    val fromString: (String) -> Any? = remember(prop) {
        when (prop) {
            is BoolProperty -> { it: String -> it.toBooleanStrictOrNull() }
            is ChoiceProperty -> { it: String -> it.takeIf { it in prop.choices } }
            is FloatProperty -> { it: String -> it.toFloatOrNull() }
            is IntProperty -> { it: String -> it.toIntOrNull() }
            is StringProperty -> { it: String -> it }
        }
    }

    var currentValue by remember {
        mutableStateOf((node.properties[prop.name] ?: prop.default).toString())
    }

    LaunchedEffect(currentValue) {
        if (currentValue != (node.properties[prop.name] ?: prop.default).toString()) {
            delay(1.seconds)

            val v = fromString(currentValue) ?: return@LaunchedEffect
            @Suppress("UNCHECKED_CAST")
            (node.graph as Graph<Unit>).updateProperty(node as Node<Unit>, prop.name, v)
        }
    }

    NodeGraphProp(prop.name, currentValue, {
        @Suppress("UNCHECKED_CAST")
        fromString(it)?.let { t -> (prop as PropertyDefinition<Any>).validate(t) } ?: false
    }) {
        currentValue = it
    }
}

@Composable
fun NodeGraphProp(label: String, valueString: String, validate: (String) -> Boolean, onChange: (String) -> Unit) {
    val style = LocalNodeGraphStyle.current

    var display by remember(label, valueString) { mutableStateOf(valueString) }
    val isValid by remember {
        derivedStateOf {
            validate(display)
        }
    }
    val borderColor by animateColorAsState(if (isValid) style.colors.propertyFieldBorder else style.colors.propertyFieldWarn)

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(label, style = style.typography.propertyFieldName.copy(color=style.colors.propertyFieldName), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                display,
                {
                    display = it
                    if (validate(it)) {
                        onChange(it)
                    }
                },
                singleLine = true,
                textStyle = style.typography.propertyFieldContent.copy(color=style.colors.propertyFieldContent),
                cursorBrush = SolidColor(style.colors.propertyFieldContent),
                modifier = Modifier
                    .weight(1f)
                    .background(style.colors.propertyFieldBackground)
                    .border(1.dp, borderColor)
                    .padding(horizontal = 6.dp, vertical = 4.dp),

            )
            AnimatedVisibility(!isValid) {
                Text(
                    "Invalid value!",
                    style = style.typography.propertyFieldWarn.copy(color = style.colors.propertyFieldWarn),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}
