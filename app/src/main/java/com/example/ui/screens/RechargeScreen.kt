package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.RechargeTransactionEntity
import com.example.data.local.StationConfigEntity
import com.example.data.local.StationLocationEntity
import com.example.data.repository.ChargingRepository
import com.example.ui.theme.CashGreen
import com.example.ui.theme.UpiBlue
import com.example.ui.theme.VoltSuccess
import com.example.util.FormattingUtils
import com.example.util.QrCodeGenerator

@Composable
fun RechargeScreen(
    config: StationConfigEntity,
    locations: List<StationLocationEntity>,
    selectedStationId: Int?,
    latestReceipt: RechargeTransactionEntity?,
    onSelectStation: (Int) -> Unit,
    onSubmitRecharge: (
        station: StationLocationEntity,
        vehicleNumber: String,
        customerName: String,
        units: Double,
        amountInr: Double,
        paymentMethod: String,
        cashTenderedInr: Double?,
        upiReference: String?
    ) -> Unit,
    onDismissReceipt: () -> Unit,
    onGoToMeter: () -> Unit,
    onStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeStation = remember(locations, selectedStationId) {
        locations.firstOrNull { it.id == selectedStationId } ?: locations.firstOrNull()
    }

    var vehicleNumber by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf("AMOUNT") } // "AMOUNT" or "UNITS"
    var amountInput by remember { mutableStateOf("200") }
    var unitsInput by remember { mutableStateOf("10") }
    var paymentMethod by remember { mutableStateOf("UPI") } // "UPI" or "CASH"
    var upiRefInput by remember { mutableStateOf("") }
    var cashTenderedInput by remember { mutableStateOf("200") }

    val rate = config.ratePerUnitInr
    val maxRupeesAtRate = ChargingRepository.calculateAmountForUnits(config.maxUnitsPerSession, rate)

    val computedAmount: Double
    val computedUnits: Double
    if (inputMode == "AMOUNT") {
        val rawAmount = amountInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val cappedAmount = rawAmount.coerceAtMost(maxRupeesAtRate)
        computedAmount = ChargingRepository.roundTwoDecimals(cappedAmount)
        computedUnits = ChargingRepository.calculateUnitsForAmount(computedAmount, rate)
    } else {
        val rawUnits = unitsInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val cappedUnits = rawUnits.coerceAtMost(config.maxUnitsPerSession)
        computedUnits = ChargingRepository.roundTwoDecimals(cappedUnits)
        computedAmount = ChargingRepository.calculateAmountForUnits(computedUnits, rate)
    }

    val upiUri = remember(config.upiId, config.businessName, computedAmount) {
        ChargingRepository.buildUpiDeepLink(
            upiId = config.upiId,
            payeeName = config.businessName,
            amountInr = computedAmount.coerceAtLeast(1.0),
            note = "EV Charging at ${config.businessName}"
        )
    }
    val upiQrBitmap = remember(upiUri) {
        QrCodeGenerator.generateQrBitmap(upiUri, sizePx = 420)
    }

    val cashTendered = cashTenderedInput.toDoubleOrNull() ?: computedAmount
    val cashChange = (cashTendered - computedAmount).coerceAtLeast(0.0)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "EV Recharge & Billing",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Pay via UPI or Cash • Auto-capped at Owner rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Rate locked by Owner",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Owner Rate Applied: ${FormattingUtils.formatInr(rate)} / unit",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Max limit per session: ${FormattingUtils.formatUnits(config.maxUnitsPerSession)} (${FormattingUtils.formatInr(maxRupeesAtRate)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Station & Vehicle",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (locations.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(locations, key = { it.id }) { loc ->
                                    FilterChip(
                                        selected = loc.id == activeStation?.id,
                                        onClick = { onSelectStation(loc.id) },
                                        label = { Text(loc.name.substringBefore("—").trim()) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.EvStation,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = vehicleNumber,
                                onValueChange = { vehicleNumber = it },
                                label = { Text("Vehicle No.") },
                                placeholder = { Text("RJ14 EV 4521") },
                                leadingIcon = {
                                    Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("recharge_vehicle_input")
                            )
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Driver (Opt)") },
                                placeholder = { Text("Name") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Recharge Limit (At Owner Rate)",
                            style = MaterialTheme.typography.titleMedium
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = inputMode == "AMOUNT",
                                onClick = { inputMode = "AMOUNT" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("mode_amount_btn")
                            ) {
                                Text("By Amount (₹)")
                            }
                            SegmentedButton(
                                selected = inputMode == "UNITS",
                                onClick = { inputMode = "UNITS" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("mode_units_btn")
                            ) {
                                Text("By Units (kWh)")
                            }
                        }

                        if (inputMode == "AMOUNT") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("50", "100", "200", "500", "1000")) { preset ->
                                    FilterChip(
                                        selected = amountInput == preset,
                                        onClick = {
                                            amountInput = preset
                                            cashTenderedInput = preset
                                        },
                                        label = { Text("₹$preset") }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = {
                                    amountInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    cashTenderedInput = amountInput
                                },
                                label = { Text("Recharge Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recharge_amount_input")
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("2", "5", "10", "15", "25")) { preset ->
                                    FilterChip(
                                        selected = unitsInput == preset,
                                        onClick = { unitsInput = preset },
                                        label = { Text("$preset kWh") }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = unitsInput,
                                onValueChange = {
                                    unitsInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                                },
                                label = { Text("Units to Charge (kWh)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recharge_units_input")
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
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
                                Column {
                                    Text(
                                        text = "Payable Total",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormattingUtils.formatInr(computedAmount),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Units Allowed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormattingUtils.formatUnits(computedUnits),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "3. Payment Method (UPI or Cash)",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PaymentOptionTile(
                                title = "UPI Payment",
                                subtitle = "Scan QR / GPay / PhonePe",
                                icon = Icons.Filled.QrCode2,
                                accentColor = UpiBlue,
                                selected = paymentMethod == "UPI",
                                onClick = { paymentMethod = "UPI" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_upi_option")
                            )
                            PaymentOptionTile(
                                title = "Cash Counter",
                                subtitle = "Collect cash & return change",
                                icon = Icons.Filled.Payments,
                                accentColor = CashGreen,
                                selected = paymentMethod == "CASH",
                                onClick = {
                                    paymentMethod = "CASH"
                                    cashTenderedInput = String.format(java.util.Locale.US, "%.0f", computedAmount)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_cash_option")
                            )
                        }

                        if (paymentMethod == "UPI") {
                            Surface(
                                color = UpiBlue.copy(alpha = 0.06f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Scan to Pay ${FormattingUtils.formatInr(computedAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = UpiBlue
                                    )
                                    if (upiQrBitmap != null) {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(14.dp),
                                            shadowElevation = 2.dp
                                        ) {
                                            Image(
                                                bitmap = upiQrBitmap,
                                                contentDescription = "UPI Payment QR Code",
                                                modifier = Modifier
                                                    .size(180.dp)
                                                    .padding(10.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "UPI ID: ${config.upiId}",
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    FilledTonalButton(
                                        onClick = {
                                            if (computedAmount <= 0.0) {
                                                onStatusMessage("Enter a valid amount first")
                                                return@FilledTonalButton
                                            }
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                                                context.startActivity(intent)
                                            } catch (_: ActivityNotFoundException) {
                                                onStatusMessage("No UPI app installed on this device; scan QR or confirm receipt below")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("launch_upi_app_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pay via UPI App (${FormattingUtils.formatInr(computedAmount)})")
                                    }

                                    OutlinedTextField(
                                        value = upiRefInput,
                                        onValueChange = { upiRefInput = it },
                                        label = { Text("UPI UTR / Ref No. (Optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val station = activeStation
                                            if (station == null) {
                                                onStatusMessage("Please add a station location first")
                                                return@Button
                                            }
                                            onSubmitRecharge(
                                                station,
                                                vehicleNumber,
                                                customerName,
                                                computedUnits,
                                                computedAmount,
                                                "UPI",
                                                null,
                                                upiRefInput
                                            )
                                        },
                                        enabled = computedAmount > 0.0 && activeStation != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = UpiBlue),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("confirm_upi_button")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirm UPI Paid & Start Charging")
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = CashGreen.copy(alpha = 0.07f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Cash Collection Counter",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CashGreen
                                    )

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val exact = String.format(java.util.Locale.US, "%.0f", computedAmount)
                                        items(listOf(exact, "100", "200", "500", "1000", "2000").distinct()) { cashPreset ->
                                            FilterChip(
                                                selected = cashTenderedInput == cashPreset,
                                                onClick = { cashTenderedInput = cashPreset },
                                                label = { Text(if (cashPreset == exact) "Exact ₹$cashPreset" else "₹$cashPreset") }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = cashTenderedInput,
                                        onValueChange = {
                                            cashTenderedInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                                        },
                                        label = { Text("Cash Received from Driver (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cash_tendered_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Change to Return:",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = FormattingUtils.formatInr(cashChange),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CashGreen
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val station = activeStation
                                            if (station == null) {
                                                onStatusMessage("Please add a station location first")
                                                return@Button
                                            }
                                            if (cashTendered < computedAmount) {
                                                onStatusMessage("Cash received is less than ${FormattingUtils.formatInr(computedAmount)}")
                                                return@Button
                                            }
                                            onSubmitRecharge(
                                                station,
                                                vehicleNumber,
                                                customerName,
                                                computedUnits,
                                                computedAmount,
                                                "CASH",
                                                cashTendered,
                                                null
                                            )
                                        },
                                        enabled = computedAmount > 0.0 && activeStation != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("confirm_cash_button")
                                    ) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Collect Cash & Start Charging")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (latestReceipt != null) {
        ReceiptDialog(
            businessName = config.businessName,
            receipt = latestReceipt,
            onDismiss = onDismissReceipt,
            onViewLiveMeter = {
                onDismissReceipt()
                onGoToMeter()
            }
        )
    }
}

@Composable
private fun PaymentOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReceiptDialog(
    businessName: String,
    receipt: RechargeTransactionEntity,
    onDismiss: () -> Unit,
    onViewLiveMeter: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Payment Confirmed",
                tint = VoltSuccess,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = "Recharge Confirmed",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = businessName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                ReceiptLine("Receipt ID", receipt.receiptCode)
                ReceiptLine("Station", receipt.stationName)
                ReceiptLine("Vehicle", receipt.vehicleNumber)
                ReceiptLine("Payment Mode", receipt.paymentMethod)
                ReceiptLine("Owner Rate", "${FormattingUtils.formatInr(receipt.ratePerUnitSnapshot)}/unit")
                ReceiptLine("Units Limit", FormattingUtils.formatUnits(receipt.unitsPurchased))
                ReceiptLine("Amount Paid", FormattingUtils.formatInr(receipt.amountInr))
                if (receipt.paymentMethod == "CASH" && receipt.changeReturnedInr != null) {
                    ReceiptLine("Cash Change", FormattingUtils.formatInr(receipt.changeReturnedInr))
                }
                if (!receipt.upiReference.isNullOrBlank()) {
                    ReceiptLine("UPI Ref", receipt.upiReference)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onViewLiveMeter,
                modifier = Modifier.testTag("receipt_view_meter_btn")
            ) {
                Text("View Live Meter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
