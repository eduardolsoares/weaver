package com.martmists.compose.grapheditor.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.martmists.compose.grapheditor.compose.internal.withSaturationDelta
import com.martmists.compose.grapheditor.compose.internal.withValueDelta

data class NodeGraphColors(
    // === Main canvas ===
    val background: Color = Color(0xFF0A0C0F),
    val gridLine: Color = Color(0xFF1A2330),

    // == Nodes ==
    val nodeName: Color = Color(0xFFE1EAF3),
    val nodeTypeName: Color = Color(0xFF6D7F8F),
    val nodeDefault: Color = Color(0xFF0D1A2E),
    val nodeSelected: Color = Color(0xFF08162C),
    val nodeBorder: Color = Color(0xFF171B22),
    val nodeMap: Map<String, Color> = emptyMap(),

    // == Ports ==
    val portName: Color = Color(0xFF6D7F8F),
    val portDefault: Color = Color(0xFF3FB950),
    val portMap: Map<String, Color> = emptyMap(),

    // == Connections ==
    val wireDefault: Color = Color(0xFF388BFD),
    val wireMap: Map<String, Color> = emptyMap(),

    // Selections
    val selectionRect: Color = Color(0x2E1F6FEB),
    val selectionHighlight: Color = Color(0xFF1F6FEB),

    // === Sidebars ===
    val sidebar: Color = Color(0xFF111418),
    val sidebarHeader: Color = Color(0xFF171B22),
    val sidebarHeaderText: Color = Color(0xFFE1EAF3),
    val sidebarBorder: Color = Color(0xFF171B22),

    // == Palette Sidebar ==
    val paletteNodeName: Color = Color(0xFFC9D1D9),

    // == Property Sidebar ==
    val propertyHeaderText: Color = Color(0xFF303948),
    val propertyFieldName: Color = Color(0xFF6D7F8F),
    val propertyFieldContent: Color = Color(0xFFE1EAF3),
    val propertyFieldBackground: Color = Color(0xFF171B22),
    val propertyFieldBorder: Color = Color(0xFF262D39),
    val propertyFieldWarn: Color = Color(0xFFF85149),

    // === Other ===
    val contextMenu: Color = Color(0xFF171B22),
    val contextMenuText: Color = Color(0xFFC9D1D9),
    val contextMenuWarn: Color = Color(0xFFF85149),
) {
    companion object {
        fun themed(
            background: Color = Color(0xFF0A0C0F),
            panel: Color = Color(0xFF111418),
            accent: Color = Color(0xFF1F6FEB),
            text: Color = Color(0xFFC9D1D9),
            alert: Color = Color(0xFFF85149),
            wire: Color = Color(0xFF388BFD),
            port: Color = Color(0xFF3FB950),
            nodeMap: Map<String, Color> = emptyMap(),
            portMap: Map<String, Color> = emptyMap(),
            wireMap: Map<String, Color> = emptyMap(),
        ) = NodeGraphColors(
            background=background,
            gridLine=background.withSaturationDelta(0.13f).withValueDelta(0.13f),
            nodeName=text.withValueDelta(0.1f),
            nodeTypeName=text.withSaturationDelta(0.16f).withValueDelta(-0.29f),
            nodeDefault=accent.withSaturationDelta(-0.15f).withValueDelta(-0.74f),
            nodeSelected=accent.withSaturationDelta(-0.06f).withValueDelta(-0.75f),
            nodeBorder=panel.withSaturationDelta(0.06f).withValueDelta(0.04f),
            nodeMap=nodeMap,
            portName=text.withSaturationDelta(0.16f).withValueDelta(-0.29f),
            portDefault=port,
            portMap=portMap,
            wireDefault=wire,
            wireMap=wireMap,
            selectionRect=accent.copy(alpha=0.18f),
            selectionHighlight=accent,
            sidebar=panel,
            sidebarHeader=panel.withSaturationDelta(0.06f).withValueDelta(0.04f),
            sidebarHeaderText=text.withValueDelta(0.1f),
            sidebarBorder=panel.withSaturationDelta(0.06f).withValueDelta(0.04f),
            paletteNodeName=text,
            propertyHeaderText=panel.withSaturationDelta(0.05f).withValueDelta(0.19f),
            propertyFieldName=text.withSaturationDelta(0.16f).withValueDelta(-0.29f),
            propertyFieldContent=text.withValueDelta(0.1f),
            propertyFieldBackground=panel.withSaturationDelta(0.06f).withValueDelta(0.04f),
            propertyFieldBorder=panel.withSaturationDelta(0.05f).withValueDelta(0.13f),
            propertyFieldWarn=alert,
            contextMenu=panel.withSaturationDelta(0.06f).withValueDelta(0.04f),
            contextMenuText=text,
            contextMenuWarn=alert,
        )
    }
}

enum class ConnectionType {
    STRAIGHT,
    SQUARE,
    BEZIER,
}

// Note: Colors on this are ignored! Text colors are used from NodeGraphColors
data class NodeGraphTypography(
    val nodeName: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val nodeTypeName: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
    val portName: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
    val sidebarHeaderText: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val paletteNodeName: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val propertyHeaderText: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val propertyFieldName: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val propertyFieldContent: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val propertyFieldWarn: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val contextMenuText: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val contextMenuWarn: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
    val contextMenuShortcut: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
) {
    companion object {
        fun single(
            style: TextStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
        ) = NodeGraphTypography(
            style,
            style.copy(fontSize = 8.sp),
            style.copy(fontSize = 12.sp),
            style,
            style,
            style,
            style,
            style,
            style,
            style,
            style,
            style.copy(fontSize = 10.sp),
        )
    }
}

data class NodeGraphStyle(
    val colors: NodeGraphColors = NodeGraphColors.themed(),
    val typography: NodeGraphTypography = NodeGraphTypography.single(),
    val connectionStyle: ConnectionType = ConnectionType.BEZIER,
    val nodeCorners: CornerRadius = CornerRadius(4f, 4f),
)

val LocalNodeGraphStyle = staticCompositionLocalOf { NodeGraphStyle() }
