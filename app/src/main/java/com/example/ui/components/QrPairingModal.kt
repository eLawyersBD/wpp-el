package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LinkedSession
import java.util.UUID

@Composable
fun QrPairingModal(
    currentSession: LinkedSession?,
    onDismiss: () -> Unit,
    onPairSuccess: (businessName: String, phoneNumber: String, qrToken: String, deviceName: String) -> Unit
) {
    var businessName by remember { mutableStateOf(currentSession?.businessName ?: "Apex Global Commerce") }
    var phoneNumber by remember { mutableStateOf(currentSession?.phoneNumber ?: "+1 (555) 019-8842") }
    var deviceName by remember { mutableStateOf(currentSession?.deviceName ?: "Android Companion Terminal") }
    var qrToken by remember {
        mutableStateOf(
            currentSession?.qrToken ?: "WA-BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
        )
    }
    // Default to the CameraX-based QR code scanner component
    var useCameraScanner by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_line"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Link WhatsApp Business via QR",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_qr_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close QR Modal"
                        )
                    }
                }

                Text(
                    text = "Scan the WhatsApp Business multi-device QR code with the CameraX scanner or use the interactive QR matrix handshake below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Switcher: CameraX Live QR Scanner vs Interactive QR Matrix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = useCameraScanner,
                        onClick = { useCameraScanner = true },
                        label = { Text("CameraX QR Scanner") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_camerax_scanner_chip")
                    )
                    FilterChip(
                        selected = !useCameraScanner,
                        onClick = { useCameraScanner = false },
                        label = { Text("Interactive QR Matrix") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_interactive_matrix_chip")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (useCameraScanner) {
                    CameraXQrScanner(
                        onQrCodeScanned = { scannedToken ->
                            qrToken = scannedToken
                            onPairSuccess(businessName, phoneNumber, scannedToken, deviceName)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrMatrixCanvas(
                            seed = qrToken,
                            laserProgress = laserProgress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "SESSION TOKEN: $qrToken",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("WhatsApp Business Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_business_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("WhatsApp Business Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Linked Companion Device Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            qrToken = "WA-BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh QR")
                    }

                    Button(
                        onClick = {
                            onPairSuccess(businessName, phoneNumber, qrToken, deviceName)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("confirm_qr_pair_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan & Link Live",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QrMatrixCanvas(
    seed: String,
    laserProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gridSize = 15
        val cellW = size.width / gridSize
        val cellH = size.height / gridSize
        val darkColor = Color(0xFF042F2E)
        val accentColor = Color(0xFF0B6E4F)

        fun isFinderPattern(r: Int, c: Int): Boolean {
            val topLeft = r in 0..4 && c in 0..4
            val topRight = r in 0..4 && c in (gridSize - 5) until gridSize
            val bottomLeft = r in (gridSize - 5) until gridSize && c in 0..4
            return topLeft || topRight || bottomLeft
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (isFinderPattern(r, c)) {
                    val localR = if (r >= gridSize - 5) r - (gridSize - 5) else r
                    val localC = if (c >= gridSize - 5) c - (gridSize - 5) else c
                    val isBorder = localR == 0 || localR == 4 || localC == 0 || localC == 4
                    val isCenter = localR == 2 && localC == 2
                    if (isBorder || isCenter) {
                        drawRect(
                            color = if (isCenter) accentColor else darkColor,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.92f, cellH * 0.92f)
                        )
                    }
                } else {
                    val hash = (seed.hashCode() xor (r * 31 + c * 17))
                    if (hash and 1 == 0) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.88f, cellH * 0.88f)
                        )
                    }
                }
            }
        }

        val laserY = size.height * laserProgress
        drawLine(
            color = Color(0xFF10B981),
            start = Offset(0f, laserY),
            end = Offset(size.width, laserY),
            strokeWidth = 4.dp.toPx()
        )
    }
}
