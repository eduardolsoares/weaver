package ceub.weaver.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import weaver.shared.generated.resources.Lexend_Bold
import weaver.shared.generated.resources.Lexend_Regular
import weaver.shared.generated.resources.Res

object WeaverFonts{
    @Composable
        fun getLexendFontFamily(): FontFamily {
            return FontFamily(
                Font(Res.font.Lexend_Regular, FontWeight.Normal),
                Font(Res.font.Lexend_Bold, FontWeight.Bold)
            )

    }
}