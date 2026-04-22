package com.cyphershadowbourne.nfcstudioultra.ui.screen

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import com.cyphershadowbourne.nfcstudioultra.domain.model.WriteTemplate
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefRecordType
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.nfc.ParsedNdefRecord
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onLocationLatitudeChanged: (String) -> Unit,
    onLocationLongitudeChanged: (String) -> Unit,
    onContactNameChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onContactEmailChanged: (String) -> Unit,
    onContactOrganizationChanged: (String) -> Unit,
    onWifiSsidChanged: (String) -> Unit,
    onWifiPasswordChanged: (String) -> Unit,
    onWifiAuthTypeChanged: (String) -> Unit,
    onCalendarTitleChanged: (String) -> Unit,
    onCalendarLocationChanged: (String) -> Unit,
    onCalendarDescriptionChanged: (String) -> Unit,
    onCalendarStartChanged: (String) -> Unit,
    onCalendarEndChanged: (String) -> Unit,
    onSmartPosterTitleChanged: (String) -> Unit,
    onAarPackageNameChanged: (String) -> Unit,
    onMimeTypeChanged: (String) -> Unit,
    onMimePayloadChanged: (String) -> Unit,
    onMimeIsHexChanged: (Boolean) -> Unit,
    onExternalDomainChanged: (String) -> Unit,
    onExternalTypeChanged: (String) -> Unit,
    onExternalPayloadChanged: (String) -> Unit,
    onExternalIsHexChanged: (Boolean) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissAction: () -> Unit,
    onClearRead: () -> Unit,
    onShowInspector: (HistoryItem) -> Unit,
    onShowInspectorForLastRead: () -> Unit,
    onHideInspector: () -> Unit,
    history: kotlinx.coroutines.flow.StateFlow<List<HistoryItem>>,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    templates: kotlinx.coroutines.flow.StateFlow<List<WriteTemplate>>,
    onSaveTemplate: (String) -> Unit,
    onLoadTemplate: (WriteTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onRenameTemplate: (String, String) -> Unit,
    onShowSaveTemplateDialog: () -> Unit,
    onHideSaveTemplateDialog: () -> Unit,
    onMultiWriteMode: () -> Unit,
    onAddToMultiWriteList: () -> Unit,
    onRemoveFromMultiWriteList: (Int) -> Unit,
    onMoveMultiWriteItem: (Int, Int) -> Unit,
    onClearMultiWriteList: () -> Unit,
    onCloneMode: () -> Unit,
    onClearCloneData: () -> Unit,
    onCompareMode: () -> Unit,
    onClearCompareData: () -> Unit,
    urlSafetyEnabled: Boolean,
    onUrlSafetyToggle: (Boolean) -> Unit,
    expertModeEnabled: Boolean,
    onExpertModeToggle: (Boolean) -> Unit
) {
    val view = LocalView.current
    var currentTab by remember { mutableIntStateOf(0) }
    val historyItems by history.collectAsState()
    val templateList by templates.collectAsState()

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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = PanelSurface,
                contentColor = NeonCyan,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Nfc, contentDescription = "Scan") },
                    label = { Text("SCAN", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = PanelBorder
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Save, contentDescription = "Templates") },
                    label = { Text("TEMPLATES", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonMagenta,
                        selectedTextColor = NeonMagenta,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = PanelBorder
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("HISTORY", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = PanelBorder
                    )
                )
            }
        },
        containerColor = DeepNavy
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (currentTab == 0) {
                ScanTab(
                    state = state,
                    onReadMode = onReadMode,
                    onWriteMode = onWriteMode,
                    onEraseMode = onEraseMode,
                    onIdleMode = onIdleMode,
                    onWriteTypeSelected = onWriteTypeSelected,
                    onTextChanged = onTextChanged,
                    onUrlChanged = onUrlChanged,
                    onPhoneChanged = onPhoneChanged,
                    onEmailToChanged = onEmailToChanged,
                    onEmailSubjectChanged = onEmailSubjectChanged,
                    onEmailBodyChanged = onEmailBodyChanged,
                    onSmsNumberChanged = onSmsNumberChanged,
                    onSmsBodyChanged = onSmsBodyChanged,
                    onLocationLatitudeChanged = onLocationLatitudeChanged,
                    onLocationLongitudeChanged = onLocationLongitudeChanged,
                    onContactNameChanged = onContactNameChanged,
                    onContactPhoneChanged = onContactPhoneChanged,
                    onContactEmailChanged = onContactEmailChanged,
                    onContactOrganizationChanged = onContactOrganizationChanged,
                    onWifiSsidChanged = onWifiSsidChanged,
                    onWifiPasswordChanged = onWifiPasswordChanged,
                    onWifiAuthTypeChanged = onWifiAuthTypeChanged,
                    onCalendarTitleChanged = onCalendarTitleChanged,
                    onCalendarLocationChanged = onCalendarLocationChanged,
                    onCalendarDescriptionChanged = onCalendarDescriptionChanged,
                    onCalendarStartChanged = onCalendarStartChanged,
                    onCalendarEndChanged = onCalendarEndChanged,
                    onSmartPosterTitleChanged = onSmartPosterTitleChanged,
                    onAarPackageNameChanged = onAarPackageNameChanged,
                    onMimeTypeChanged = onMimeTypeChanged,
                    onMimePayloadChanged = onMimePayloadChanged,
                    onMimeIsHexChanged = onMimeIsHexChanged,
                    onExternalDomainChanged = onExternalDomainChanged,
                    onExternalTypeChanged = onExternalTypeChanged,
                    onExternalPayloadChanged = onExternalPayloadChanged,
                    onExternalIsHexChanged = onExternalIsHexChanged,
                    onConfirmAction = onConfirmAction,
                    onDismissAction = onDismissAction,
                    onClearRead = onClearRead,
                    onShowInspector = onShowInspectorForLastRead,
                    onSaveAsTemplate = onShowSaveTemplateDialog,
                    onMultiWriteMode = onMultiWriteMode,
                    onAddToMultiWriteList = onAddToMultiWriteList,
                    onRemoveFromMultiWriteList = onRemoveFromMultiWriteList,
                    onMoveMultiWriteItem = onMoveMultiWriteItem,
                    onClearMultiWriteList = onClearMultiWriteList,
                    onCloneMode = onCloneMode,
                    onClearCloneData = onClearCloneData,
                    onCompareMode = onCompareMode,
                    onClearCompareData = onClearCompareData,
                    urlSafetyEnabled = urlSafetyEnabled,
                    onUrlSafetyToggle = onUrlSafetyToggle,
                    expertModeEnabled = expertModeEnabled,
                    onExpertModeToggle = onExpertModeToggle
                )
            } else if (currentTab == 1) {
                TemplatesTab(
                    templates = templateList,
                    onLoad = {
                        onLoadTemplate(it)
                        currentTab = 0
                    },
                    onDelete = onDeleteTemplate,
                    onRename = onRenameTemplate
                )
            } else {
                HistoryTab(
                    items = historyItems,
                    onDeleteItem = onDeleteHistoryItem,
                    onClearAll = onClearHistory,
                    onViewDetails = onShowInspector
                )
            }

            state.inspectorItem?.let { item ->
                InspectorOverlay(
                    item = item,
                    state = state,
                    onDismiss = onHideInspector
                )
            }

            if (state.showTemplateSaveDialog) {
                SaveTemplateDialog(
                    onDismiss = onHideSaveTemplateDialog,
                    onSave = onSaveTemplate
                )
            }
        }
    }
}

@Composable
fun ScanTab(
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
    onLocationLatitudeChanged: (String) -> Unit,
    onLocationLongitudeChanged: (String) -> Unit,
    onContactNameChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onContactEmailChanged: (String) -> Unit,
    onContactOrganizationChanged: (String) -> Unit,
    onWifiSsidChanged: (String) -> Unit,
    onWifiPasswordChanged: (String) -> Unit,
    onWifiAuthTypeChanged: (String) -> Unit,
    onCalendarTitleChanged: (String) -> Unit,
    onCalendarLocationChanged: (String) -> Unit,
    onCalendarDescriptionChanged: (String) -> Unit,
    onCalendarStartChanged: (String) -> Unit,
    onCalendarEndChanged: (String) -> Unit,
    onSmartPosterTitleChanged: (String) -> Unit,
    onAarPackageNameChanged: (String) -> Unit,
    onMimeTypeChanged: (String) -> Unit,
    onMimePayloadChanged: (String) -> Unit,
    onMimeIsHexChanged: (Boolean) -> Unit,
    onExternalDomainChanged: (String) -> Unit,
    onExternalTypeChanged: (String) -> Unit,
    onExternalPayloadChanged: (String) -> Unit,
    onExternalIsHexChanged: (Boolean) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissAction: () -> Unit,
    onClearRead: () -> Unit,
    onShowInspector: () -> Unit,
    onSaveAsTemplate: () -> Unit,
    onMultiWriteMode: () -> Unit,
    onAddToMultiWriteList: () -> Unit,
    onRemoveFromMultiWriteList: (Int) -> Unit,
    onMoveMultiWriteItem: (Int, Int) -> Unit,
    onClearMultiWriteList: () -> Unit,
    onCloneMode: () -> Unit,
    onClearCloneData: () -> Unit,
    onCompareMode: () -> Unit,
    onClearCompareData: () -> Unit,
    urlSafetyEnabled: Boolean,
    onUrlSafetyToggle: (Boolean) -> Unit,
    expertModeEnabled: Boolean,
    onExpertModeToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BrandHeader()

        TopModeBanner(
            state = state,
            onConfirm = onConfirmAction,
            onDismiss = onDismissAction
        )

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

            Spacer(modifier = Modifier.height(10.dp))

            ModeButton(
                title = "MULTI-WRITE",
                selected = state.mode == NfcMode.MULTI_WRITE,
                onClick = onMultiWriteMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            ModeButton(
                title = "CLONE",
                selected = state.mode == NfcMode.CLONE,
                onClick = onCloneMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            ModeButton(
                title = "COMPARE",
                selected = state.mode == NfcMode.COMPARE,
                onClick = onCompareMode,
                modifier = Modifier.fillMaxWidth()
            )
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "LOCATION",
                        selected = state.writeData.type == NdefRecordType.LOCATION,
                        onClick = { onWriteTypeSelected(NdefRecordType.LOCATION) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "CONTACT",
                        selected = state.writeData.type == NdefRecordType.CONTACT,
                        onClick = { onWriteTypeSelected(NdefRecordType.CONTACT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "WI-FI",
                        selected = state.writeData.type == NdefRecordType.WIFI,
                        onClick = { onWriteTypeSelected(NdefRecordType.WIFI) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "CALENDAR",
                        selected = state.writeData.type == NdefRecordType.CALENDAR,
                        onClick = { onWriteTypeSelected(NdefRecordType.CALENDAR) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "POSTER",
                        selected = state.writeData.type == NdefRecordType.SMART_POSTER,
                        onClick = { onWriteTypeSelected(NdefRecordType.SMART_POSTER) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "AAR",
                        selected = state.writeData.type == NdefRecordType.AAR,
                        onClick = { onWriteTypeSelected(NdefRecordType.AAR) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "MIME",
                        selected = state.writeData.type == NdefRecordType.MIME,
                        onClick = { onWriteTypeSelected(NdefRecordType.MIME) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "EXTERNAL",
                        selected = state.writeData.type == NdefRecordType.EXTERNAL,
                        onClick = { onWriteTypeSelected(NdefRecordType.EXTERNAL) },
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
                            keyboardType = KeyboardType.Uri,
                            onValueChange = onUrlChanged
                        )
                    }

                    NdefRecordType.PHONE -> {
                        UltraTextField(
                            label = "Phone Number",
                            value = state.writeData.phoneNumber,
                            placeholder = "+441234567890",
                            keyboardType = KeyboardType.Phone,
                            onValueChange = onPhoneChanged
                        )
                    }

                    NdefRecordType.EMAIL -> {
                        UltraTextField(
                            label = "Email Address",
                            value = state.writeData.emailTo,
                            placeholder = "name@example.com",
                            keyboardType = KeyboardType.Email,
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
                            keyboardType = KeyboardType.Phone,
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

                    NdefRecordType.LOCATION -> {
                        UltraTextField(
                            label = "Latitude",
                            value = state.writeData.locationLatitude,
                            placeholder = "53.3498",
                            keyboardType = KeyboardType.Decimal,
                            onValueChange = onLocationLatitudeChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Longitude",
                            value = state.writeData.locationLongitude,
                            placeholder = "-6.2603",
                            keyboardType = KeyboardType.Decimal,
                            onValueChange = onLocationLongitudeChanged
                        )
                    }

                    NdefRecordType.CONTACT -> {
                        UltraTextField(
                            label = "Name",
                            value = state.writeData.contactName,
                            placeholder = "Cypher Shadowbourne",
                            onValueChange = onContactNameChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Phone Number",
                            value = state.writeData.contactPhone,
                            placeholder = "+441234567890",
                            keyboardType = KeyboardType.Phone,
                            onValueChange = onContactPhoneChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Email",
                            value = state.writeData.contactEmail,
                            placeholder = "name@example.com",
                            keyboardType = KeyboardType.Email,
                            onValueChange = onContactEmailChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Organization",
                            value = state.writeData.contactOrganization,
                            placeholder = "Studio Ultra",
                            onValueChange = onContactOrganizationChanged
                        )
                    }

                    NdefRecordType.WIFI -> {
                        UltraTextField(
                            label = "SSID",
                            value = state.writeData.wifiSsid,
                            placeholder = "Network Name",
                            onValueChange = onWifiSsidChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Password",
                            value = state.writeData.wifiPassword,
                            placeholder = "Password",
                            onValueChange = onWifiPasswordChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Auth Type",
                            value = state.writeData.wifiAuthType,
                            placeholder = "WPA/WPA2",
                            onValueChange = onWifiAuthTypeChanged
                        )
                    }

                    NdefRecordType.CALENDAR -> {
                        UltraTextField(
                            label = "Title",
                            value = state.writeData.calendarTitle,
                            placeholder = "Meeting",
                            onValueChange = onCalendarTitleChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Location",
                            value = state.writeData.calendarLocation,
                            placeholder = "Conference Room",
                            onValueChange = onCalendarLocationChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Start (YYYY-MM-DD HH:MM)",
                            value = state.writeData.calendarStart,
                            placeholder = "2023-12-31 23:59",
                            onValueChange = onCalendarStartChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "End (YYYY-MM-DD HH:MM)",
                            value = state.writeData.calendarEnd,
                            placeholder = "2024-01-01 01:00",
                            onValueChange = onCalendarEndChanged
                        )
                    }

                    NdefRecordType.SMART_POSTER -> {
                        UltraTextField(
                            label = "Title",
                            value = state.writeData.smartPosterTitle,
                            placeholder = "Visit Website",
                            onValueChange = onSmartPosterTitleChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "URI",
                            value = state.writeData.url,
                            placeholder = "https://example.com",
                            keyboardType = KeyboardType.Uri,
                            onValueChange = onUrlChanged
                        )
                    }

                    NdefRecordType.AAR -> {
                        UltraTextField(
                            label = "Package Name",
                            value = state.writeData.aarPackageName,
                            placeholder = "com.example.app",
                            onValueChange = onAarPackageNameChanged
                        )
                    }

                    NdefRecordType.MIME -> {
                        UltraTextField(
                            label = "MIME Type",
                            value = state.writeData.mimeType,
                            placeholder = "text/plain",
                            onValueChange = onMimeTypeChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Payload",
                            value = state.writeData.mimePayload,
                            placeholder = "Data content",
                            onValueChange = onMimePayloadChanged
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.writeData.mimeIsHex, onCheckedChange = onMimeIsHexChanged)
                            Text("Hex Mode", color = Color.White)
                        }
                    }

                    NdefRecordType.EXTERNAL -> {
                        UltraTextField(
                            label = "Domain",
                            value = state.writeData.externalDomain,
                            placeholder = "com.example",
                            onValueChange = onExternalDomainChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Type",
                            value = state.writeData.externalType,
                            placeholder = "mytype",
                            onValueChange = onExternalTypeChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Payload",
                            value = state.writeData.externalPayload,
                            placeholder = "Data content",
                            onValueChange = onExternalPayloadChanged
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.writeData.externalIsHex, onCheckedChange = onExternalIsHexChanged)
                            Text("Hex Mode", color = Color.White)
                        }
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
                    value = if (state.writeData.isValid()) {
                        "Yes"
                    } else {
                        "No"
                    }
                )

                if (state.writeData.isValid()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GradientActionButton(
                        text = "ADD TO MULTI-WRITE",
                        enabled = true,
                        onClick = onAddToMultiWriteList
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GradientActionButton(
                        text = "SAVE AS TEMPLATE",
                        enabled = true,
                        onClick = onSaveAsTemplate
                    )
                }
                if (state.mode == NfcMode.CLONE && state.cloneMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CloneCandidatePanel(
                        message = state.cloneMessage,
                        onClear = onClearCloneData
                    )
                }
                
                if (state.mode == NfcMode.COMPARE && state.compareResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ComparisonResultPanel(
                        result = state.compareResult,
                        onClear = onClearCompareData
                    )
                }
            }

            if (state.multiWriteList.isNotEmpty()) {
                PanelCard(title = "MULTI-WRITE QUEUE") {
                    state.multiWriteList.forEachIndexed { index, data ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Record ${index + 1}: ${data.type.name}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = data.describeForUi(),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onRemoveFromMultiWriteList(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    GradientActionButton(
                        text = "CLEAR QUEUE",
                        enabled = true,
                        onClick = onClearMultiWriteList
                    )
                }
            }

            PanelCard(title = "STOP NFC") {
                GradientActionButton(
                    text = "STOP NFC",
                    enabled = state.mode != NfcMode.IDLE,
                    onClick = onIdleMode
                )
            }

            PanelCard(title = "SETTINGS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "URL Safety Preview",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Preview links before opening them",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = urlSafetyEnabled,
                        onCheckedChange = onUrlSafetyToggle,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = NeonBlue,
                            checkedTrackColor = NeonBlue.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Expert Mode",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Show advanced technical details",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = expertModeEnabled,
                        onCheckedChange = onExpertModeToggle,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = NeonMagenta,
                            checkedTrackColor = NeonMagenta.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            PanelCard(title = "6. LAST TAG") {
                LabeledValue(
                    label = "CONTENT",
                    value = state.lastRead.ifBlank { "Nothing read yet." }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "DETAILS",
                    value = state.lastDetails.ifBlank { "No details yet." }
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (state.lastRead.isNotBlank()) {
                    GradientActionButton(
                        text = "VIEW DETAILS",
                        enabled = true,
                        onClick = onShowInspector
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                GradientActionButton(
                    text = "CLEAR RESULT",
                    enabled = state.lastRead.isNotBlank() || state.lastDetails.isNotBlank(),
                    onClick = onClearRead
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
    keyboardType: KeyboardType = KeyboardType.Text,
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@Composable
private fun TopModeBanner(
    state: NfcStudioUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    state.pendingAction?.let { action ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (state.feedbackType) {
                    UiFeedbackType.ERROR -> Color.Red.copy(alpha = 0.2f)
                    UiFeedbackType.WARNING -> NeonMagenta.copy(alpha = 0.2f)
                    else -> NeonBlue.copy(alpha = 0.15f)
                }
            ),
            border = BorderStroke(
                width = 1.dp,
                color = when (state.feedbackType) {
                    UiFeedbackType.ERROR -> Color.Red.copy(alpha = 0.6f)
                    UiFeedbackType.WARNING -> NeonMagenta.copy(alpha = 0.6f)
                    else -> NeonBlue.copy(alpha = 0.6f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = action.title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state.feedbackType) {
                                UiFeedbackType.ERROR -> Color.Red
                                UiFeedbackType.WARNING -> NeonMagenta
                                else -> NeonBlue
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONFIRM", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(state: NfcStudioUiState) {
    val color = when (state.statusTone) {
        StatusTone.INFO -> NeonCyan
        StatusTone.SUCCESS -> Color(0xFF00E676)
        StatusTone.WARNING -> Color(0xFFFFD600)
        StatusTone.ERROR -> NeonMagenta
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = state.status,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistoryTab(
    items: List<HistoryItem>,
    onDeleteItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onViewDetails: (HistoryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TAG HISTORY",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            if (items.isNotEmpty()) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All",
                        tint = NeonMagenta
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history yet",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.sortedByDescending { it.timestamp }.forEach { item ->
                    HistoryItemCard(
                        item = item,
                        onDelete = { onDeleteItem(item.id) },
                        onClick = { onViewDetails(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, PanelBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.recordTypes.firstOrNull() ?: "Unknown",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.previewText,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ComparisonResultPanel(
    result: com.cyphershadowbourne.nfcstudioultra.nfc.TagComparisonResult,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = (if (result.areEqual) Color(0xFF00E676) else Color(0xFFFFD600)).copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, (if (result.areEqual) Color(0xFF00E676) else Color(0xFFFFD600)).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (result.areEqual) Icons.Default.Nfc else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (result.areEqual) Color(0xFF00E676) else Color(0xFFFFD600)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COMPARISON RESULT",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (result.areEqual) Color(0xFF00E676) else Color(0xFFFFD600),
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear comparison", tint = TextMuted)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (result.areEqual) "TAGS ARE IDENTICAL" else "TAGS ARE DIFFERENT",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            if (!result.areEqual) {
                Spacer(modifier = Modifier.height(8.dp))
                result.differences.forEach { diff ->
                    Text(
                        text = "• $diff",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonMagenta
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TAG 1", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    Text("Records: ${result.tag1.recordCount}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("Types: ${result.tag1.recordTypes.joinToString()}", style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("TAG 2", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    Text("Records: ${result.tag2.recordCount}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("Types: ${result.tag2.recordTypes.joinToString()}", style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun CloneCandidatePanel(
    message: android.nfc.NdefMessage,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CLONE CANDIDATE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear clone data")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Captured: ${message.records.size} records (${message.toByteArray().size} bytes)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Ready to write to destination tag.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TemplatesTab(
    templates: List<WriteTemplate>,
    onLoad: (WriteTemplate) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit
) {
    var templateToRename by remember { mutableStateOf<WriteTemplate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SAVED TEMPLATES",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No templates saved yet.\nCreate one from the Scan tab!",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                templates.forEach { template ->
                    TemplateItemCard(
                        template = template,
                        onLoad = { onLoad(template) },
                        onDelete = { onDelete(template.id) },
                        onRename = { templateToRename = template }
                    )
                }
            }
        }
    }

    templateToRename?.let { template ->
        RenameTemplateDialog(
            currentName = template.name,
            onDismiss = { templateToRename = null },
            onRename = { newName ->
                onRename(template.id, newName)
                templateToRename = null
            }
        )
    }
}

@Composable
private fun TemplateItemCard(
    template: WriteTemplate,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PanelBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = template.data.type.name,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = "Rename",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NeonMagenta.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.data.describeForUi(),
                color = TextPrimary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            GradientActionButton(
                text = "LOAD CONFIG",
                enabled = true,
                onClick = onLoad
            )
        }
    }
}

@Composable
fun SaveTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "SAVE AS TEMPLATE",
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column {
                Text(
                    "Enter a name for this write configuration to reuse it later.",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                UltraTextField(
                    label = "Template Name",
                    value = name,
                    placeholder = "e.g. Work Website",
                    onValueChange = { name = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = DeepNavy,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RenameTemplateDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "RENAME TEMPLATE",
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            UltraTextField(
                label = "New Name",
                value = name,
                placeholder = "Enter name",
                onValueChange = { name = it }
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onRename(name) },
                enabled = name.isNotBlank() && name != currentName,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RENAME", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = DeepNavy,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun InspectorOverlay(
    item: HistoryItem,
    state: NfcStudioUiState,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                "TAG INSPECTOR",
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        containerColor = DeepNavy,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InspectorSection(title = "TAG INFO") {
                    InspectorRow(label = "ID (Hex)", value = item.tagIdHex ?: "Unknown") {
                        item.tagIdHex?.let {
                            clipboardManager.setText(AnnotatedString(it))
                        }
                    }
                    if (state.expertMode) {
                        InspectorRow(label = "Technologies", value = item.techList.joinToString(", ").ifBlank { "Unknown" })
                        InspectorRow(label = "Writable", value = if (item.isWritable) "Yes" else "No")
                        InspectorRow(label = "Max Size", value = item.sizeBytes?.let { "$it bytes" } ?: "Unknown")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                InspectorSection(title = "NDEF RECORDS (${item.records.size})") {
                    if (item.records.isEmpty()) {
                        Text("No NDEF records found.", color = TextMuted, fontSize = 14.sp)
                    } else {
                        item.records.forEachIndexed { index, record ->
                            RecordCard(
                                index = index + 1,
                                record = record,
                                expertMode = state.expertMode,
                                onCopyValue = {
                                    clipboardManager.setText(AnnotatedString(record.decodedValue))
                                },
                                onCopyPayload = {
                                    clipboardManager.setText(AnnotatedString(record.payloadHex))
                                }
                            )
                            if (index < item.records.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
                if (state.expertMode && state.logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    InspectorSection(title = "OPERATION LOGS") {
                        state.logs.forEach { log ->
                            Text(
                                text = log,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun InspectorSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun InspectorRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = NeonCyan.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextPrimary, fontSize = 14.sp)
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordCard(
    index: Int,
    record: ParsedNdefRecord,
    expertMode: Boolean,
    onCopyValue: () -> Unit,
    onCopyPayload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PanelBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RECORD #$index",
                    color = NeonMagenta,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
                if (expertMode) {
                    Text(
                        record.tnf,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (expertMode) {
                Text("TYPE: ${record.type}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = record.decodedValue,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onCopyValue,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("COPY VALUE", fontSize = 10.sp, color = NeonCyan)
                }
                if (expertMode) {
                    TextButton(
                        onClick = onCopyPayload,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("COPY PAYLOAD", fontSize = 10.sp, color = NeonCyan)
                    }
                }
            }
        }
    }
}
