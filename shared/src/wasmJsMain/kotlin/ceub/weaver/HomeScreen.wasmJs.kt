package ceub.weaver

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun HomeScreen(
    onBackToMain: () -> Unit,
) {
    Row {
        Text("<")
        Text("BEM VINDO AO WEAVER STUDIO.")
    }
}
