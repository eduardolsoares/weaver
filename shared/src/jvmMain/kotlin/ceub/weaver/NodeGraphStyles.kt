package ceub.weaver

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.martmists.compose.grapheditor.compose.ConnectionType
import com.martmists.compose.grapheditor.compose.NodeGraphColors
import com.martmists.compose.grapheditor.compose.NodeGraphStyle
import com.martmists.compose.grapheditor.compose.NodeGraphTypography

fun databaseTableStyle(isDark: Boolean): NodeGraphStyle =
    if (isDark) darkTableStyle() else lightTableStyle()

private fun darkTableStyle() = NodeGraphStyle(
    colors = NodeGraphColors(
        background = Color(0xFF0A0C0F),
        gridLine = Color(0xFF1A2330),
        nodeName = Color(0xFFE1EAF3),
        nodeTypeName = Color(0xFF6D7F8F),
        nodeDefault = Color(0xFF121926),
        nodeSelected = Color(0xFF1E2A3A),
        nodeBorder = Color(0xFF273445),
        nodeMap = mapOf(
            "Table" to Color(0xFF121926),
            "Enum" to Color(0xFF1A1425),
            "Note" to Color(0xFF141A1F),
        ),
        portName = Color(0xFF8B9DC4),
        portDefault = Color(0xFF3FB950),
        portMap = mapOf(
            "table" to Color(0xFF3FB950),
            "enum" to Color(0xFFD29922),
        ),
        wireDefault = Color(0xFF4A9EFF),
        wireMap = mapOf(
            "table" to Color(0xFF4A9EFF),
            "enum" to Color(0xFFD29922),
        ),
        selectionRect = Color(0x2E1F6FEB),
        selectionHighlight = Color(0xFF1F6FEB),
        sidebar = Color(0xFF111418),
        sidebarHeader = Color(0xFF171B22),
        sidebarHeaderText = Color(0xFFE1EAF3),
        sidebarBorder = Color(0xFF1E2835),
        paletteNodeName = Color(0xFFC9D1D9),
        propertyHeaderText = Color(0xFF303948),
        propertyFieldName = Color(0xFF6D7F8F),
        propertyFieldContent = Color(0xFFE1EAF3),
        propertyFieldBackground = Color(0xFF171B22),
        propertyFieldBorder = Color(0xFF262D39),
        propertyFieldWarn = Color(0xFFF85149),
        contextMenu = Color(0xFF1C2533),
        contextMenuText = Color(0xFFC9D1D9),
        contextMenuWarn = Color(0xFFF85149),
    ),
    typography = NodeGraphTypography.single(
        TextStyle.Default.copy(fontFamily = FontFamily.Monospace)
    ),
    connectionStyle = ConnectionType.BEZIER,
    nodeCorners = CornerRadius(6f, 6f),
)

private fun lightTableStyle() = NodeGraphStyle(
    colors = NodeGraphColors(
        background = Color(0xFFF0F2F5),
        gridLine = Color(0xFFE2E5EB),
        nodeName = Color(0xFF1A1D23),
        nodeTypeName = Color(0xFF6B7280),
        nodeDefault = Color(0xFFFFFFFF),
        nodeSelected = Color(0xFFE8F0FE),
        nodeBorder = Color(0xFFD1D5DB),
        nodeMap = mapOf(
            "Table" to Color(0xFFFFFFFF),
            "Enum" to Color(0xFFFFF9ED),
            "Note" to Color(0xFFF9FAFB),
        ),
        portName = Color(0xFF6B7280),
        portDefault = Color(0xFF10B981),
        portMap = mapOf(
            "table" to Color(0xFF10B981),
            "enum" to Color(0xFFF59E0B),
        ),
        wireDefault = Color(0xFF3B82F6),
        wireMap = mapOf(
            "table" to Color(0xFF3B82F6),
            "enum" to Color(0xFFF59E0B),
        ),
        selectionRect = Color(0x2E3B82F6),
        selectionHighlight = Color(0xFF3B82F6),
        sidebar = Color(0xFFFFFFFF),
        sidebarHeader = Color(0xFFF3F4F6),
        sidebarHeaderText = Color(0xFF1A1D23),
        sidebarBorder = Color(0xFFE5E7EB),
        paletteNodeName = Color(0xFF1A1D23),
        propertyHeaderText = Color(0xFF9CA3AF),
        propertyFieldName = Color(0xFF6B7280),
        propertyFieldContent = Color(0xFF1A1D23),
        propertyFieldBackground = Color(0xFFF9FAFB),
        propertyFieldBorder = Color(0xFFE5E7EB),
        propertyFieldWarn = Color(0xFFEF4444),
        contextMenu = Color(0xFFFFFFFF),
        contextMenuText = Color(0xFF1A1D23),
        contextMenuWarn = Color(0xFFEF4444),
    ),
    typography = NodeGraphTypography.single(
        TextStyle.Default.copy(fontFamily = FontFamily.Monospace)
    ),
    connectionStyle = ConnectionType.BEZIER,
    nodeCorners = CornerRadius(6f, 6f),
)
