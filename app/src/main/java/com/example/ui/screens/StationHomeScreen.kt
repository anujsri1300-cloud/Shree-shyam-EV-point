package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RechargeTransactionEntity
import com.example.data.local.StationConfigEntity
import com.example.data.local.StationLocationEntity
import com.example.ui.theme.CashGreen
import com.example.ui.theme.MeterDarkEnd
import com.example.ui.theme.MeterDarkStart
import com.example.ui.theme.UpiBlue
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltSuccess
import com.example.ui.viewmodel.ActiveChargingSession
import com.example.ui.viewmodel.DashboardSummary
import com.example.util.FormattingUtils

@Composable
fun StationHomeScreen(
    config: StationConfigEntity,
    locations: List<StationLocationEntity>,
    transactions: List<RechargeTransactionEntity>,
    summary: DashboardSummary,
    activeSession: ActiveChargingSession?,
    onNavigateToRecharge: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToOwner: () -> Unit,
    onToggleMeter: () -> Unit,
    onStopMeter: () -> Unit,
    onDismissSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAvailableBays = locations.sumOf { it.availableBays }
    val totalBays = locations.sumOf { it.totalBays }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StationHeroBanner(
                    businessName = config.businessName,
                    ratePerUnit = config.ratePerUnitInr,
                    stationCount = locations.size,
                    availableBays = totalAvailableBays,
                    totalBays = totalBays,
                    onEditRateClick = onNavigateToOwner
                )
            }

            item {
                AnimatedVisibility(visible = activeSession != null) {
                    activeSession?.let { session ->
                        LiveChargingMeterCard(
                            session = session,
                            onToggleMeter = onToggleMeter,
                            onStopMeter = onStopMeter,
                            onDismissSession = onDismissSession
                        )
                    }
                }
            }

            item {
                QuickActionRow(
                    onRechargeClick = onNavigateToRecharge,
                    onMapClick = onNavigateToMap,
                    onOwnerClick = onNavigateToOwner
                )
            }

            item {
                SummaryMetricsCard(summary = summary)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Recharges",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${transactions.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(onRechargeClick = onNavigateToRecharge)
                }
            } else {
                items(transactions.take(10), key = { it.id }) { tx ->
                    TransactionRowCard(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StationHeroBanner(
    businessName: String,
    ratePerUnit: Double,
    stationCount: Int,
    availableBays: Int,
    totalBays: Int,
    onEditRateClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF073B2C), Color(0xFF0E684C), Color(0xFF13485E))
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color(0x224EE0A8),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.92f, size.height * 0.18f)
                )
                drawCircle(
                    color = Color(0x1AFBBF24),
                    radius = size.minDimension * 0.35f,
                    center = Offset(size.width * 0.12f, size.height * 0.88f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color(0x26FFFFFF),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EvStation,
                                    contentDescription = null,
                                    tint = VoltAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$stationCount Locations • $availableBays/$totalBays Bays Free",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = businessName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All charging bills strictly capped at Owner rate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD2EBE0)
                        )
                    }
                }

                Surface(
                    color = Color(0x24000000),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable(onClick = onEditRateClick)
                        .testTag("hero_owner_rate_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OWNER PER-UNIT RATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA9E4CA)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = FormattingUtils.formatInr(ratePerUnit),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = " / unit (kWh)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFD2EBE0),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        Surface(
                            color = VoltAmber,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Set Owner Rate",
                                    tint = Color(0xFF2B1600),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Set Rate",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF2B1600)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChargingMeterCard(
    session: ActiveChargingSession,
    onToggleMeter: () -> Unit,
    onStopMeter: () -> Unit,
    onDismissSession: () -> Unit
) {
    val progress = if (session.targetAmountInr > 0) {
        (session.consumedAmountInr / session.targetAmountInr).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MeterDarkStart),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_charging_meter_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(MeterDarkStart, MeterDarkEnd)))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (session.isCompletedAtLimit) VoltSuccess else VoltAmber)
                        )
                        Text(
                            text = if (session.isCompletedAtLimit) {
                                "STOPPED AT PAID LIMIT"
                            } else if (session.isRunning) {
                                "LIVE CHARGING METER"
                            } else {
                                "CHARGING PAUSED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (session.isCompletedAtLimit) VoltSuccess else VoltAmber
                        )
                    }
                    Text(
                        text = "${session.receiptCode} • ${session.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB3CBD9)
                    )
                }

                Text(
                    text = "${session.stationName} • ${session.vehicleNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Amount Used",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFB3CBD9)
                        )
                        Text(
                            text = "${FormattingUtils.formatInr(session.consumedAmountInr)} / ${FormattingUtils.formatInr(session.targetAmountInr)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Units Dispensed",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFB3CBD9)
                        )
                        Text(
                            text = "${FormattingUtils.formatUnits(session.deliveredUnits)} / ${FormattingUtils.formatUnits(session.targetUnits)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (session.isCompletedAtLimit) VoltSuccess else VoltAmber,
                    trackColor = Color(0x33FFFFFF),
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "Locked Rate: ${FormattingUtils.formatInr(session.ratePerUnit)}/unit • Auto-cuts off at 100%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9AC8F0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!session.isCompletedAtLimit) {
                        FilledTonalButton(
                            onClick = onToggleMeter,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meter_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (session.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (session.isRunning) "Pause" else "Resume")
                        }
                        OutlinedButton(
                            onClick = onStopMeter,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meter_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Complete")
                        }
                    } else {
                        Button(
                            onClick = onDismissSession,
                            colors = ButtonDefaults.buttonColors(containerColor = VoltSuccess),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meter_done_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Session Completed — Clear Meter")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onRechargeClick: () -> Unit,
    onMapClick: () -> Unit,
    onOwnerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionTile(
            title = "Recharge",
            subtitle = "UPI / Cash",
            icon = Icons.Filled.Bolt,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onRechargeClick,
            modifier = Modifier
                .weight(1f)
                .testTag("home_quick_recharge")
        )
        QuickActionTile(
            title = "Locations",
            subtitle = "Google Maps",
            icon = Icons.Filled.LocationOn,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onMapClick,
            modifier = Modifier
                .weight(1f)
                .testTag("home_quick_locations")
        )
        QuickActionTile(
            title = "Owner",
            subtitle = "Set ₹/Unit",
            icon = Icons.Filled.AdminPanelSettings,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOwnerClick,
            modifier = Modifier
                .weight(1f)
                .testTag("home_quick_owner")
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryMetricsCard(summary: DashboardSummary) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Collection Overview",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = FormattingUtils.formatUnits(summary.totalUnitsKwh),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniBox(
                    label = "Total Revenue",
                    value = FormattingUtils.formatInr(summary.totalRevenueInr),
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "UPI (${summary.upiCount})",
                    value = FormattingUtils.formatInr(summary.upiRevenueInr),
                    accent = UpiBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "Cash (${summary.cashCount})",
                    value = FormattingUtils.formatInr(summary.cashRevenueInr),
                    accent = CashGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(tx: RechargeTransactionEntity) {
    val isUpi = tx.paymentMethod == "UPI"
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isUpi) UpiBlue.copy(alpha = 0.12f) else CashGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isUpi) Icons.Filled.QrCode2 else Icons.Filled.Payments,
                            contentDescription = tx.paymentMethod,
                            tint = if (isUpi) UpiBlue else CashGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${tx.vehicleNumber} • ${tx.paymentMethod}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tx.stationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tx.receiptCode} • ${FormattingUtils.formatTimestamp(tx.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormattingUtils.formatInr(tx.amountInr),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${FormattingUtils.formatUnits(tx.unitsPurchased)} @ ₹${tx.ratePerUnitSnapshot}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tx.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tx.status == "COMPLETED") VoltSuccess else VoltAmber
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard(onRechargeClick: () -> Unit) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "No Recharges Recorded Yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Start a UPI or Cash recharge using the Owner's per-unit rate to see live meter telemetry and receipts here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRechargeClick,
                modifier = Modifier.testTag("empty_state_recharge_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Recharge (UPI / Cash)")
            }
        }
    }
}
