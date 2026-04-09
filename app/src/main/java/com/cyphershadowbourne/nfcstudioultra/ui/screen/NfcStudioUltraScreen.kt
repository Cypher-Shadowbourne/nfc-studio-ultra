package com.cyphershadowbourne.nfcstudioultra.ui.screen

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefRecordType
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.ui.NfcStudioUiState
import com.cyphershadowbourne.nfcstudioultra.ui.StatusTone
import com.cyphershadowbourne.nfcstudioultra.ui.UiFeedbackType
import com.cyphershadowbourne.nfcstudioultra.ui.theme.DeepNavy
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NeonBlue
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NeonCyan
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NeonMagenta
import com.cyphershadowbourne.nfcstudioultra.ui.theme.PanelBorder
import com.cyphershadowbourne.nfcstudioultra.ui.theme.PanelSurface
import com.cyphershadowbourne.nfcstudioultra.ui.theme.TextMuted
import com.cyphershadowbourne.nfcstudioultra.ui.theme.TextPrimary

@Composable
fun NfcStudioUltraScreen(
    state: NfcStudioUiState,
    onReadMode: () -> Unit,
    onWriteMode: () -> Unit,
    onEraseMode: () -> Unit,
    onIdleMode: () -> Unit,
    onWriteTypeSelected: (NdefRecordType) -> Unit,
    onTextChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailToChanged: (String) -> Unit,
    onEmailSubjectChanged: (String) -> Unit,
    onEmailBodyChanged: (String) -> Unit,
    onSmsNumberChanged: (String) -> Unit,
    onSmsBodyChanged: (String) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissAction: () -> Unit,
    onClearRead: () -> Unit
) {
    val view = LocalView.current

    LaunchedEffect(state.feedbackEventId) {
        if (state.feedbackEventId == 0L) return@LaunchedEffect

        when (state.feedbackType) {
            UiFeedbackType.SUCCESS -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            UiFeedbackType.ERROR -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            UiFeedbackType.WARNING -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            UiFeedbackType.INFO -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            UiFeedbackType.NONE -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHeader()

            TopModeBanner(state = state)

            StatusBanner(state = state)

            PanelCard(title = "1. WHAT TO DO") {
                LabeledValue(
                    label = "MODE",
                    value = state.mode.name
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "DO THIS NOW",
                    value = state.armedMessage
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "LAST MESSAGE",
                    value = state.lastActionMessage
                )
            }

            PanelCard(title = "2. PICK A MODE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeButton(
                        title = "READ",
                        selected = state.mode == NfcMode.READ,
                        onClick = onReadMode,
                        modifier = Modifier.weight(1f)
                    )
                    ModeButton(
                        title = "WRITE",
                        selected = state.mode == NfcMode.WRITE,
                        onClick = onWriteMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeButton(
                        title = "ERASE",
                        selected = state.mode == NfcMode.ERASE,
                        onClick = onEraseMode,
                        modifier = Modifier.weight(1f)
                    )
                    ModeButton(
                        title = "STOP",
                        selected = state.mode == NfcMode.IDLE,
                        onClick = onIdleMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            PanelCard(title = "3. PICK WHAT TO WRITE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "TEXT",
                        selected = state.writeData.type == NdefRecordType.TEXT,
                        onClick = { onWriteTypeSelected(NdefRecordType.TEXT) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "URL",
                        selected = state.writeData.type == NdefRecordType.URL,
                        onClick = { onWriteTypeSelected(NdefRecordType.URL) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "PHONE",
                        selected = state.writeData.type == NdefRecordType.PHONE,
                        onClick = { onWriteTypeSelected(NdefRecordType.PHONE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "EMAIL",
                        selected = state.writeData.type == NdefRecordType.EMAIL,
                        onClick = { onWriteTypeSelected(NdefRecordType.EMAIL) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "SMS",
                        selected = state.writeData.type == NdefRecordType.SMS,
                        onClick = { onWriteTypeSelected(NdefRecordType.SMS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LabeledValue(
                    label = "CURRENT TYPE",
                    value = state.writeData.type.name
                )
            }

            PanelCard(title = "4. ENTER DATA") {
                when (state.writeData.type) {
                    NdefRecordType.TEXT -> {
                        UltraTextField(
                            label = "Text",
                            value = state.writeData.text,
                            placeholder = "Type text here",
                            onValueChange = onTextChanged
                        )
                    }

                    NdefRecordType.URL -> {
                        UltraTextField(
                            label = "Website",
                            value = state.writeData.url,
                            placeholder = "example.com",
                            onValueChange = onUrlChanged
                        )
                    }

                    NdefRecordType.PHONE -> {
                        UltraTextField(
                            label = "Phone Number",
                            value = state.writeData.phoneNumber,
                            placeholder = "+441234567890",
                            onValueChange = onPhoneChanged
                        )
                    }

                    NdefRecordType.EMAIL -> {
                        UltraTextField(
                            label = "Email Address",
                            value = state.writeData.emailTo,
                            placeholder = "name@example.com",
                            onValueChange = onEmailToChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Subject",
                            value = state.writeData.emailSubject,
                            placeholder = "Subject",
                            onValueChange = onEmailSubjectChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Message",
                            value = state.writeData.emailBody,
                            placeholder = "Type your message",
                            minLines = 3,
                            onValueChange = onEmailBodyChanged
                        )
                    }

                    NdefRecordType.SMS -> {
                        UltraTextField(
                            label = "Phone Number",
                            value = state.writeData.smsNumber,
                            placeholder = "+441234567890",
                            onValueChange = onSmsNumberChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Message",
                            value = state.writeData.smsBody,
                            placeholder = "Type your message",
                            minLines = 3,
                            onValueChange = onSmsBodyChanged
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LabeledValue(
                    label = "PREVIEW",
                    value = state.writeData.describeForUi()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "READY TO WRITE",
                    value = if (state.canWrite) {
                        "Yes"
                    } else {
                        "No"
                    }
                )
            }

            PanelCard(title = "5. MAIN BUTTONS") {
                GradientActionButton(
                    text = "READ TAG",
                    enabled = state.mode != NfcMode.READ,
                    onClick = onReadMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientActionButton(
                    text = "WRITE TAG",
                    enabled = state.mode != NfcMode.WRITE && state.canWrite,
                    onClick = onWriteMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientActionButton(
                    text = "ERASE TAG",
                    enabled = state.mode != NfcMode.ERASE,
                    onClick = onEraseMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientActionButton(
                    text = "STOP NFC",
                    enabled = state.mode != NfcMode.IDLE,
                    onClick = onIdleMode
                )
            }

            PanelCard(title = "6. LAST TAG") {
                LabeledValue(
                    label = "CONTENT",
                    value = if (state.lastRead.isBlank()) "Nothing read yet." else state.lastRead
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "DETAILS",
                    value = if (state.lastDetails.isBlank()) "No details yet." else state.lastDetails
                )

                Spacer(modifier = Modifier.height(14.dp))

                GradientActionButton(
                    text = "CLEAR RESULT",
                    enabled = state.lastRead.isNotBlank() || state.lastDetails.isNotBlank(),
                    onClick = onClearRead
                )
            }

            Text(
                text = "BY CYPHER SHADOWBOURNE",
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun TopModeBanner(state: NfcStudioUiState) {
    GradientActionButton(
        text = when (state.mode) {
            NfcMode.READ -> "READ READY"
            NfcMode.WRITE -> if (state.canWrite) "WRITE READY" else "ADD WRITE DATA"
            NfcMode.ERASE -> "ERASE READY"
            NfcMode.IDLE -> "NFC STOPPED"
        },
        enabled = false,
        onClick = {}
    )
}

@Composable
private fun StatusBanner(state: NfcStudioUiState) {
    val toneColor = when (state.statusTone) {
        StatusTone.INFO -> NeonCyan
        StatusTone.SUCCESS -> NeonBlue
        StatusTone.WARNING -> NeonMagenta
        StatusTone.ERROR -> Color(0xFFFF4D6D)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, toneColor.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(26.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "STATUS",
                color = toneColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.status,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = state.armedMessage,
                color = TextPrimary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    val lineGradient = Brush.horizontalGradient(
        colors = listOf(NeonCyan, NeonBlue, NeonMagenta)
    )

    val ultraGradient = Brush.horizontalGradient(
        colors = listOf(NeonCyan, NeonBlue, NeonMagenta)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Text(
            text = "NFC STUDIO",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = ultraGradient
                    )
                ) {
                    append("ULTRA")
                }
            },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(lineGradient, RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "BY CYPHER SHADOWBOURNE",
            color = TextMuted,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = PanelBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun PanelCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            NeonCyan.copy(alpha = 0.55f),
                            NeonMagenta.copy(alpha = 0.55f)
                        )
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextMuted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (selected) {
        Brush.horizontalGradient(listOf(NeonCyan, NeonBlue, NeonMagenta))
    } else {
        Brush.horizontalGradient(listOf(PanelSurface, PanelSurface))
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(18.dp))
                .border(
                    1.dp,
                    if (selected) Color.Transparent else PanelBorder,
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WriteTypeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (selected) {
        Brush.horizontalGradient(listOf(NeonMagenta, NeonBlue))
    } else {
        Brush.horizontalGradient(listOf(PanelSurface, PanelSurface))
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (selected) Color.Transparent else PanelBorder,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val enabledBrush = Brush.horizontalGradient(listOf(NeonCyan, NeonBlue, NeonMagenta))
    val disabledBrush = Brush.horizontalGradient(
        listOf(
            PanelBorder.copy(alpha = 0.55f),
            PanelBorder.copy(alpha = 0.55f)
        )
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.72f)
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) enabledBrush else disabledBrush,
                    shape = RoundedCornerShape(26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                PanelBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = NeonCyan,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun UltraTextField(
    label: String,
    value: String,
    placeholder: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextMuted) },
        placeholder = { Text(placeholder, color = TextMuted) },
        minLines = minLines,
        maxLines = if (minLines > 1) 6 else 1,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = neonTextFieldColors()
    )
}

@Composable
private fun neonTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = PanelSurface,
    unfocusedContainerColor = PanelSurface,
    disabledContainerColor = PanelSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NeonCyan,
    focusedIndicatorColor = NeonMagenta,
    unfocusedIndicatorColor = PanelBorder,
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = TextMuted
)