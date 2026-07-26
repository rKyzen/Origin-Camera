package com.origin.browser.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.origin.browser.R
import com.origin.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameInputDialog(
    initialName: String,
    onSaveName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(initialName) }
    val ntype82 = FontFamily(Font(R.font.ntype82_regular))

    Dialog(
        onDismissRequest = {
            if (initialName.isNotBlank()) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = initialName.isNotBlank(),
            dismissOnClickOutside = initialName.isNotBlank()
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            OriginSurface,
                            OriginDarkBackground
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            OriginWhite.copy(alpha = 0.5f),
                            OriginOutline,
                            OriginWhite.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top App Logo Badge with Outer Glow Ring
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(OriginSurfaceVariant)
                        .border(
                            width = 1.dp,
                            brush = Brush.radialGradient(
                                colors = listOf(OriginWhite, OriginOutline)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Origin Logo",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "Welcome to Origin",
                    fontFamily = ntype82,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OriginWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "Personalize your browser. What should we call you?",
                    fontSize = 13.sp,
                    color = OriginMutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Styled Input Field with Leading Profile Icon & Clear Button
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = {
                        Text(
                            text = "Enter your name...",
                            color = OriginMutedText,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "👤",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        if (textValue.isNotEmpty()) {
                            IconButton(onClick = { textValue = "" }) {
                                Text(
                                    text = "✕",
                                    color = OriginMutedText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OriginWhite,
                        unfocusedBorderColor = OriginOutlineHighlight,
                        focusedTextColor = OriginWhite,
                        unfocusedTextColor = OriginWhite,
                        focusedContainerColor = OriginSurfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = OriginSurfaceVariant.copy(alpha = 0.4f),
                        cursorColor = OriginWhite
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (textValue.isNotBlank()) {
                            onSaveName(textValue.trim())
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button: Primary Continue / Save Button
                Button(
                    onClick = {
                        if (textValue.isNotBlank()) {
                            onSaveName(textValue.trim())
                        }
                    },
                    enabled = textValue.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OriginWhite,
                        contentColor = OriginBlack,
                        disabledContainerColor = OriginOutline,
                        disabledContentColor = OriginMutedText
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (initialName.isBlank()) "Get Started" else "Save Name",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Cancel Button (if user is editing existing name)
                if (initialName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
                            color = OriginMutedText,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
