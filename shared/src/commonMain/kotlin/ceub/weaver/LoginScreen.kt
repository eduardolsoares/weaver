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
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

import weaver.shared.generated.resources.Res
import weaver.shared.generated.resources.*

val CorRoxa = Color(0xFF7022B8)
val CorCinzaTexto = Color(0xFFC7C7C7)
val CorAzulBotao = Color(0xFF0080FF)
val CorBordaInput = Color(0xFF333333)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val lexendFont = getLexendFontFamily()
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
                color = CorRoxa,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 85.sp,
                fontFamily = lexendFont
            )
            Text(
                text = "Design database architectures and generate DDL scripts.",
                color = CorCinzaTexto,
                fontSize = 30.sp,
                modifier = Modifier.padding(top = 10.dp),
                fontFamily = lexendFont
            )

            Spacer(modifier = Modifier.weight(1f))

        }

        Column(
            modifier = Modifier.fillMaxHeight().weight(1f).padding(start = 40.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Create your account",
                color = CorCinzaTexto,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 30.dp),
                fontFamily = lexendFont
            )

            SocialButton(
                "Continue with Google",
                icon = painterResource(Res.drawable.icn_google)
                )
            Spacer(modifier = Modifier.height(15.dp))
            SocialButton(
                "Continue with GitHub",
                icon = painterResource(Res.drawable.icn_github),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 40.dp),
                color = CorBordaInput
            )

            Text("Email", color = CorCinzaTexto, fontSize = 14.sp, fontFamily = lexendFont)
            var email by remember { mutableStateOf("") }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Enter your email address", color = Color.Gray, fontFamily = lexendFont) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = CorBordaInput,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Text(
                "Use an organization email to easily collaborate with teammates.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 40.dp),
                fontFamily = lexendFont
            )

            Button(
                onClick = { onLoginSuccess() },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CorAzulBotao),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = lexendFont)
            }
        }
    }
}

@Composable
fun SocialButton(text: String, icon: Painter) {
    OutlinedButton(
        onClick = { },
        modifier = Modifier.fillMaxWidth().height(55.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CorBordaInput)
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

@Composable
fun getLexendFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.Lexend_Regular, FontWeight.Normal),
        Font(Res.font.Lexend_Bold, FontWeight.Bold)
    )
}