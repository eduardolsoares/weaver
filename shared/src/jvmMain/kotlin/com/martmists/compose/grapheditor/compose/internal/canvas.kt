package com.martmists.compose.grapheditor.compose.internal

import androidx.annotation.FloatRange
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.*
import com.martmists.compose.grapheditor.compose.ConnectionType
import com.martmists.compose.grapheditor.compose.GraphState
import com.martmists.compose.grapheditor.compose.NodeGraphStyle
import com.martmists.compose.grapheditor.data.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt


interface NodeScope {
    val state: GraphState
    val measurer: TextMeasurer
    val scale: Float
    val style: NodeGraphStyle
    val visibleRect: Rect

    val textDensity: Density
    fun worldToScreen(world: Offset) = Offset(
        (world.x - visibleRect.left) * scale,
        (world.y - visibleRect.top) * scale
    )
    fun worldToScreen(world: Size) = Size(
        world.width * scale,
        world.height * scale
    )
    fun screenToWorld(screen: Offset) = Offset(
        screen.x / scale + visibleRect.left,
        screen.y / scale + visibleRect.top
    )
    fun screenToWorld(world: Size) = Size(
        world.width / scale,
        world.height / scale
    )

    fun measureNodeWidth(node: Node<*>): Float {
        val nodeSize = measurer.measure(node.name, style = style.typography.nodeName).size.width
        val typeSize = measurer.measure(node.definition.name, style = style.typography.nodeTypeName).size.width
        val maxPortSize = node.definition.ports.maxOf { measurer.measure(it.name, style = style.typography.portName).size.width }
        val fullWidth = maxOf(
            nodeSize / 2 + typeSize + NODE_TYPE_LEFT_PADDING + NODE_TYPE_RIGHT_PADDING,
            maxPortSize + PORT_PADDING
        ) * 2
        return fullWidth.coerceAtLeast(NODE_MIN_WIDTH)
    }
    fun findPortAtPos(pos: Offset): Pair<Node<*>, PortDefinition>?
    fun findNodeAtPos(pos: Offset): Node<*>?
    fun findConnectionsInBox(box: Rect): List<Connection>
    fun findConnectionAtPos(pos: Offset): Connection?
}

internal class NodeScopeImpl(override val state: GraphState, private val currentDensity: Density, override val style: NodeGraphStyle, override val measurer: TextMeasurer) : NodeScope {
    override val scale by derivedStateOf { currentDensity.density * state.zoom }

    override val visibleRect by derivedStateOf {
        Rect(
            -state.panOffset / scale,
            state.canvasRect.size / scale,
        )
    }

    override val textDensity by derivedStateOf {
        Density(currentDensity.density * state.zoom, currentDensity.fontScale)
    }

    override fun findPortAtPos(pos: Offset): Pair<Node<*>, PortDefinition>? {
        val w = screenToWorld(pos)
        for (node in state.graph.nodes) {
            for ((i, p) in node.definition.inputPorts.withIndex() + node.definition.outputPorts.withIndex()) {
                val offset = node.portOffset(i, p in node.definition.outputPorts)
                val delta = w - offset
                if (sqrt(delta.x * delta.x + delta.y * delta.y) <= PORT_SIZE) {
                    return node to p
                }
            }
        }
        return null
    }

    override fun findNodeAtPos(pos: Offset): Node<*>? {
        val w = screenToWorld(pos)
        for (node in state.graph.nodes) {
            if (w in Rect(node.position, node.size())) {
                return node
            }
        }
        return null
    }

    override fun findConnectionsInBox(box: Rect): List<Connection> {
        val found = mutableListOf<Connection>()

        for (conn in state.graph.connections) {
            val start = conn.fromNode.portOffset(conn.fromNode.definition.outputPorts.indexOf(conn.fromPort), true)
            val end = conn.toNode.portOffset(conn.toNode.definition.inputPorts.indexOf(conn.toPort), false)

            val isIntersect = when (style.connectionStyle) {
                ConnectionType.STRAIGHT -> box.intersects(start, end)
                ConnectionType.SQUARE -> {
                    val centerX = minOf(start.x, end.x) + abs(start.x - end.x) / 2
                    val parts = listOf(
                        start to Offset(centerX, start.y),
                        end to Offset(centerX, end.y),
                        Offset(centerX, start.y) to Offset(centerX, end.y)
                    )
                    parts.any { box.intersects(it.first, it.second) }
                }

                ConnectionType.BEZIER -> box.intersectsBezier(start, end)
            }
            if (isIntersect) {
                found.add(conn)
            }
        }

        return found
    }

    override fun findConnectionAtPos(pos: Offset): Connection? {
        val w = screenToWorld(pos)
        val box = Rect(w, Size.Zero).inflate(2 * CONNECTION_WIDTH)
        return findConnectionsInBox(box).firstOrNull()
    }


}

internal class NodeCanvasScope(s: DrawScope, n: NodeScope) : DrawScope by s, NodeScope by n {
    fun drawWorldLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: PathEffect? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) = drawLine(
        color,
        worldToScreen(start),
        worldToScreen(end),
        strokeWidth * scale,
        cap,
        pathEffect,
        alpha,
        colorFilter,
        blendMode
    )

    fun drawWorldCircle(
        color: Color,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) = drawCircle(
        color,
        radius * scale,
        worldToScreen(center),
        alpha,
        style,
        colorFilter,
        blendMode
    )

    fun drawWorldRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) = drawRect(
        color,
        worldToScreen(topLeft),
        worldToScreen(size),
        alpha,
        style,
        colorFilter,
        blendMode
    )

    fun drawWorldRoundRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        cornerRadius: CornerRadius = CornerRadius.Zero,
        style: DrawStyle = Fill,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) = drawRoundRect(
        color,
        worldToScreen(topLeft),
        worldToScreen(size),
        CornerRadius(cornerRadius.x * scale, cornerRadius.y * scale),
        style,
        alpha,
        colorFilter,
        blendMode,
    )

    fun drawConnection(color: Color, stroke: Float, start: Offset, end: Offset) {
        when (style.connectionStyle) {
            ConnectionType.STRAIGHT -> {
                drawWorldLine(color, start, end, stroke)
            }

            ConnectionType.SQUARE -> {
                val centerX = minOf(start.x, end.x) + abs(start.x - end.x) / 2
                val min = if (start.x < centerX) start else end
                val max = if (start.x > centerX) start else end
                drawWorldLine(color, min, Offset(centerX + stroke / 2, min.y), stroke)
                drawWorldLine(color, max, Offset(centerX - stroke / 2, max.y), stroke)
                drawWorldLine(color, Offset(centerX, min.y), Offset(centerX, max.y), stroke)
            }

            ConnectionType.BEZIER -> {
                val horizontalDistance = abs(end.x - start.x)
                val controlPointOffset = horizontalDistance / 2f

                val startScreen = worldToScreen(start)
                val p1 = worldToScreen(start + Offset(controlPointOffset, 0f))
                val p2 = worldToScreen(end - Offset(controlPointOffset, 0f))
                val endScreen = worldToScreen(end)


                val path = Path().apply {
                    moveTo(startScreen.x, startScreen.y)
                    cubicTo(
                        x1 = p1.x, y1 = p1.y,
                        x2 = p2.x, y2 = p2.y,
                        x3 = endScreen.x, y3 = endScreen.y
                    )
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = stroke * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

internal fun NodeCanvasScope.drawGrid() {
    val lineScales = arrayOf(192f, 48f)
    var alpha = 1f

    for (s in lineScales) {
        val step = s * scale
        if (step < 5f) continue

        var xPos = (floor((visibleRect.left / s).toDouble()) * s).toFloat()
        while (xPos <= visibleRect.right) {
            val screenX = (xPos - visibleRect.left) * scale
            drawLine(style.colors.gridLine.copy(alpha=alpha), Offset(screenX, 0f), Offset(screenX, size.height))
            xPos += s
        }

        var yPos = (floor((visibleRect.top / s).toDouble()) * s).toFloat()
        while (yPos <= visibleRect.bottom) {
            val screenY = (yPos - visibleRect.top) * scale
            drawLine(style.colors.gridLine.copy(alpha=alpha), Offset(0f, screenY), Offset(size.width, screenY))
            yPos += s
        }

        alpha *= 0.8f
    }
}

internal fun NodeCanvasScope.drawNode(node: Node<*>) {
    val isHighlight = node in state.selectionNodes || node == state.detailsNode

    val nodeColor = if (isHighlight) {
        style.colors.nodeSelected
    } else {
        style.colors.nodeMap[node.definition.name] ?: style.colors.nodeDefault
    }

    drawWorldRoundRect(
        nodeColor,
        node.position,
        node.size(),
        style.nodeCorners,
    )
    drawWorldRoundRect(
        nodeColor.withSaturationDelta(-0.06f).withValueDelta(0.12f),
        node.position,
        Size(node.size().width, NODE_TITLE_HEIGHT),
        style.nodeCorners,
    )
    drawWorldRoundRect(
        nodeColor.withSaturationDelta(-0.06f).withValueDelta(0.12f),
        node.position + Offset(0f, NODE_TITLE_HEIGHT / 2),
        Size(node.size().width, NODE_TITLE_HEIGHT / 2),
    )

    // Highlights
    drawWorldRoundRect(
        if (isHighlight) style.colors.selectionHighlight else style.colors.nodeBorder,
        node.position,
        node.size(),
        style.nodeCorners,
        Stroke(NODE_SELECTED_HIGHLIGHT_SIZE * scale)
    )
    drawWorldLine(
        if (isHighlight) style.colors.selectionHighlight else style.colors.nodeBorder,
        node.position + Offset(0f, NODE_TITLE_HEIGHT),
        node.position + Offset(node.size().width, NODE_TITLE_HEIGHT),
        strokeWidth = NODE_SELECTED_HIGHLIGHT_SIZE / 2,
    )

    val measured = measurer.measure(node.name, style = style.typography.nodeName.copy(color=style.colors.nodeName), density = textDensity)
    val pos = node.position + Offset(node.size().width / 2, NODE_TITLE_HEIGHT / 2)
    drawText(measured, topLeft = worldToScreen(pos) - Offset(measured.size.width.toFloat() / 2, measured.size.height.toFloat() / 2))

    val typeMeasured = measurer.measure(node.definition.name, style = style.typography.nodeTypeName.copy(color=style.colors.nodeTypeName), density = textDensity)
    val typePos = node.position + Offset(node.size().width - NODE_TYPE_RIGHT_PADDING, NODE_TITLE_HEIGHT / 2)
    drawText(typeMeasured, topLeft = worldToScreen(typePos) - Offset(typeMeasured.size.width.toFloat(), typeMeasured.size.height.toFloat() / 2))

    val ports = node.definition.inputPorts.withIndex() + node.definition.outputPorts.withIndex()
    for ((i, p) in ports) {
        val isOutput = p in node.definition.outputPorts
        val portPos = node.portOffset(i, isOutput)
        val portColor = style.colors.portMap[p.dataType] ?: style.colors.portDefault

        drawWorldCircle(style.colors.background, PORT_SIZE + PORT_CUTOUT, portPos)
        drawWorldCircle(portColor, PORT_SIZE, portPos)

        val portText = measurer.measure(p.name, style = style.typography.portName.copy(style.colors.portName), density = textDensity)
        if (isOutput) {
            drawText(portText, topLeft = worldToScreen(portPos - Offset(PORT_SIZE + PORT_CUTOUT + 2, 0f)) - Offset(portText.size.width.toFloat(), portText.size.height.toFloat() / 2))
        } else {
            drawText(portText, topLeft = worldToScreen(portPos + Offset(PORT_SIZE + PORT_CUTOUT + 2, 0f)) - Offset(0f, portText.size.height.toFloat() / 2))
        }
    }
}

internal fun NodeCanvasScope.drawConnection(connection: Connection) {
    val startPos = connection.fromNode.portOffset(connection.fromNode.definition.outputPorts.indexOf(connection.fromPort), true)
    val endPos = connection.toNode.portOffset(connection.toNode.definition.inputPorts.indexOf(connection.toPort), false)

    val color = style.colors.wireMap[connection.fromPort.dataType] ?: style.colors.wireDefault

    drawWorldCircle(color, radius = CONNECTION_PORT_SIZE, center = startPos)
    drawWorldCircle(color, radius = CONNECTION_PORT_SIZE, center = endPos)

    val isSelected = connection in state.selectionConnections

    val items = if (isSelected) listOf(
        style.colors.selectionHighlight to CONNECTION_HIGHLIGHT_WIDTH,
        color to CONNECTION_WIDTH,
    ) else listOf(
        color to CONNECTION_WIDTH,
    )

    for ((c, s) in items) {
        drawConnection(c, s, startPos, endPos)
    }
}
