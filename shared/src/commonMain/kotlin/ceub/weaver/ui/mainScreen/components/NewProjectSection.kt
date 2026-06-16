package ceub.weaver.ui.mainScreen.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NewProjectSection(
    onCardClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0A0C0F),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            ) {
                Text(
                    text = "Começar um novo projeto",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF1F5F9)
                )
                Spacer(modifier = Modifier.height(20.dp))

                NewProjectCard(onClick = onCardClick)
            }
        }
    }
}