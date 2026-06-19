package ceub.weaver.ui.mainScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NewProjectCard(onClick: () -> Unit) {
    Column(modifier = Modifier.width(160.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            color = Color(0xFF1E293B),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "+",
                    color = Color(0xFF818CF8),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Text(
            text = "Em branco",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF1F5F9)
        )
    }
}