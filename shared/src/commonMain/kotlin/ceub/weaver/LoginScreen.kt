package ceub.weaver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import weaver.shared.generated.resources.*
import ceub.weaver.ui.theme.*

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoogleLoginClick: () -> Unit, errorMessage: String?) {
    val lexendFont = WeaverFonts.getLexendFontFamily()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Weaver Studio",
                color = WeaverColors.ThemePurple,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 85.sp,
                fontFamily = lexendFont
            )
            Text(
                text = "Design database architectures and generate DDL scripts.",
                color = WeaverColors.TextGray,
                fontSize = 30.sp,
                modifier = Modifier.padding(top = 10.dp),
                fontFamily = lexendFont
            )

            Spacer(modifier = Modifier.weight(1f))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

        }

        Column(
            modifier = Modifier.fillMaxHeight().weight(1f).padding(start = 40.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Create your account",
                color = WeaverColors.TextGray,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 30.dp),
                fontFamily = lexendFont
            )

            SocialButton(
                text = "Continue with Google",
                icon = painterResource(Res.drawable.icn_google),
                onClick = onGoogleLoginClick,
            )
            SocialButton(
                "Continue with GitHub",
                icon = painterResource(Res.drawable.icn_github),
                onGoogleLoginClick
            )
        }
    }
}

@Composable
fun SocialButton(text: String, icon: Painter, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(55.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, WeaverColors.InputBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = "Logo $text",
                tint = Color.Unspecified,
                modifier = Modifier.size(30.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(text, fontSize = 14.sp)
        }
    }
}

