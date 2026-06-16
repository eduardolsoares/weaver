package ceub.weaver.ui.mainScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RecentProjectCard(
    title: String,
    lastModified: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(180.dp)
            .padding(bottom = 16.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            color = Color(0xFF1E293B),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .width(if (index == 1) 80.dp else 60.dp)
                                .height(8.dp)
                                .background(Color(0xFF475569), shape = RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = lastModified,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            // 🟢 3. Modificado o container dos três pontinhos para abrir o Dropdown
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(24.dp)
                    .clickable { expanded = true }, // Abre o menu ao clicar nos três pontinhos
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⋮",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 🟢 4. O Menu flutuante acoplado exatamente abaixo dos três pontinhos
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }, // Fecha se o usuário clicar fora
                    modifier = Modifier.background(Color(0xFF1E293B)) // Mantém o tema Dark do card
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Renomear",
                                color = Color(0xFFF1F5F9)
                            )
                        },
                        onClick = {
                            expanded = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Deletar",
                                color = Color(0xFFEF4444) // Vermelho Tailwind (destructive action)
                            )
                        },
                        onClick = {
                            expanded = false // Fecha a barrinha
                            onDeleteClick()  // Dispara o callback que remove do banco
                        }
                    )
                }
            }
        }
    }
}