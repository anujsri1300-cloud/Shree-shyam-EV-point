package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.RechargeTransactionEntity
import com.example.data.local.StationConfigEntity
import com.example.data.local.StationLocationEntity
import com.example.data.repository.ChargingRepository
import com.example.ui.viewmodel.DashboardSummary
import com.example.util.FormattingUtils
import java.util.Locale

@Composable
fun OwnerPanelScreen(
    config: StationConfigEntity,
    locations: List<StationLocationEntity>,
    transactions: List<RechargeTransactionEntity>,
    summary: DashboardSummary,
    isOwnerUnlocked: Boolean,
    userLocation: Pair<Double, Double>?,
    onUnlockOwner: (String) -> Boolean,
    onLockOwner: () -> Unit,
    onUpdateRate: (Double) -> Unit,
    onSaveSettings: (
        businessName: String,
        ratePerUnitInr: Double,
        upiId: String,
        ownerPhone: String,
        ownerPin: String,
        maxUnitsPerSession: Double
    ) -> Unit,
    onSaveLocation: (StationLocationEntity) -> Unit,
    onDeleteLocation: (Int) -> Unit,
    onDeleteTransaction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOwnerUnlocked) {
        OwnerPinGate(
            onUnlock = onUnlockOwner,
            modifier = modifier
        )
        return
    }

    var rateText by remember(config.ratePerUnitInr) {
        mutableStateOf(String.format(Locale.US, "%.2f", config.ratePerUnitInr))
    }
    var businessName by remember(config.businessName) { mutableStateOf(config.businessName) }
    var upiId by remember(config.upiId) { mutableStateOf(config.upiId) }
    var ownerPhone by remember(config.ownerPhone) { mutableStateOf(config.ownerPhone) }
    var ownerPin by remember(config.ownerPin) { mutableStateOf(config.ownerPin) }
    var maxUnitsText by remember(config.maxUnitsPerSession) {
        mutableStateOf(String.format(Locale.US, "%.0f", config.maxUnitsPerSession))
    }

    var ledgerFilter by remember { mutableStateOf("ALL") }
    var editingLocation by remember { mutableStateOf<StationLocationEntity?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val parsedRate = (rateText.toDoubleOrNull() ?: config.ratePerUnitInr).coerceIn(1.0, 999.0)
    val unitsPer100 = ChargingRepository.calculateUnitsForAmount(100.0, parsedRate)
    val costFor10Units = ChargingRepository.calculateAmountForUnits(10.0, parsedRate)

    val filteredTransactions = remember(transactions, ledgerFilter) {
        when (ledgerFilter) {
            "UPI" -> transactions.filter { it.paymentMethod == "UPI" }
            "CASH" -> transactions.filter { it.paymentMethod == "CASH" }
            else -> transactions
        }
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Owner Control Panel",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Set per-unit rate, UPI ID & station locations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onLockOwner,
                        modifier = Modifier.testTag("owner_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Lock Owner Mode",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock")
                    }
                }
            }

            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Per-Unit Rate Setting (₹ / kWh)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    val next = (parsedRate - 0.5).coerceAtLeast(1.0)
                                    rateText = String.format(Locale.US, "%.2f", next)
                                },
                                modifier = Modifier.testTag("rate_decrement_btn")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease rate")
                            }

                            OutlinedTextField(
                                value = rateText,
                                onValueChange = {
                                    rateText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                },
                                label = { Text("Rate per Unit (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("owner_rate_input")
                            )

                            FilledIconButton(
                                onClick = {
                                    val next = (parsedRate + 0.5).coerceAtMost(999.0)
                                    rateText = String.format(Locale.US, "%.2f", next)
                                },
                                modifier = Modifier.testTag("rate_increment_btn")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase rate")
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("10.00", "12.00", "14.00", "16.00", "18.00", "20.00")) { preset ->
                                FilterChip(
                                    selected = rateText == preset,
                                    onClick = { rateText = preset },
                                    label = { Text("₹$preset/u") }
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Preview: ₹100.00 = ${FormattingUtils.formatUnits(unitsPer100)}  |  10 Units = ${FormattingUtils.formatInr(costFor10Units)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Button(
                            onClick = { onUpdateRate(parsedRate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("apply_owner_rate_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Enforce ₹${String.format(Locale.US, "%.2f", parsedRate)} / Unit")
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
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Business & UPI Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("Owner UPI ID (for QR & UPI Pay)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_upi_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = maxUnitsText,
                                onValueChange = {
                                    maxUnitsText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                },
                                label = { Text("Max Units/Session") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ownerPin,
                                onValueChange = {
                                    ownerPin = it.filter { ch -> ch.isDigit() }.take(6)
                                },
                                label = { Text("Owner PIN (4+ digits)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = ownerPhone,
                            onValueChange = { ownerPhone = it },
                            label = { Text("Owner Helpline Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilledTonalButton(
                            onClick = {
                                onSaveSettings(
                                    businessName,
                                    parsedRate,
                                    upiId,
                                    ownerPhone,
                                    ownerPin,
                                    maxUnitsText.toDoubleOrNull() ?: config.maxUnitsPerSession
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_owner_settings_btn")
                        ) {
                            Text("Save Business & UPI Settings")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Station Locations (${locations.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    FilledTonalButton(
                        onClick = {
                            editingLocation = null
                            showLocationDialog = true
                        },
                        modifier = Modifier.testTag("add_station_location_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Location")
                    }
                }
            }

            items(locations, key = { "loc_${it.id}" }) { loc ->
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = loc.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = loc.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "GPS: %.4f, %.4f • %d/%d bays",
                                    loc.latitude,
                                    loc.longitude,
                                    loc.availableBays,
                                    loc.totalBays
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    editingLocation = loc
                                    showLocationDialog = true
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit station")
                            }
                            if (locations.size > 1) {
                                IconButton(onClick = { onDeleteLocation(loc.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete station",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "UPI & Cash Collection Ledger",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Total: ${FormattingUtils.formatInr(summary.totalRevenueInr)} • UPI: ${FormattingUtils.formatInr(summary.upiRevenueInr)} • Cash: ${FormattingUtils.formatInr(summary.cashRevenueInr)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL", "UPI", "CASH").forEach { filter ->
                            FilterChip(
                                selected = ledgerFilter == filter,
                                onClick = { ledgerFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }
                }
            }

            items(filteredTransactions, key = { "ledger_${it.id}" }) { tx ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TransactionRowCard(tx = tx)
                    }
                    IconButton(onClick = { onDeleteTransaction(tx.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete transaction",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showLocationDialog) {
        StationLocationEditorDialog(
            initial = editingLocation,
            userLocation = userLocation,
            onDismiss = { showLocationDialog = false },
            onSave = { loc ->
                onSaveLocation(loc)
                showLocationDialog = false
            }
        )
    }
}

@Composable
private fun OwnerPinGate(
    onUnlock: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Owner Access",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "Owner Rate & Settings",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Enter Owner PIN to change the per-unit electricity rate, UPI ID, or station locations. (Default PIN: 1234)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it.filter { ch -> ch.isDigit() }.take(6)
                        errorMsg = null
                    },
                    label = { Text("Owner PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_pin_input")
                )

                Button(
                    onClick = {
                        if (!onUnlock(pinInput)) {
                            errorMsg = "Incorrect Owner PIN. Default is 1234."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("owner_unlock_button")
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Owner Panel")
                }
            }
        }
    }
}

@Composable
private fun StationLocationEditorDialog(
    initial: StationLocationEntity?,
    userLocation: Pair<Double, Double>?,
    onDismiss: () -> Unit,
    onSave: (StationLocationEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var latText by remember {
        mutableStateOf(
            initial?.latitude?.toString()
                ?: userLocation?.first?.toString()
                ?: "27.3673"
        )
    }
    var lngText by remember {
        mutableStateOf(
            initial?.longitude?.toString()
                ?: userLocation?.second?.toString()
                ?: "75.4027"
        )
    }
    var totalBaysText by remember { mutableStateOf((initial?.totalBays ?: 4).toString()) }
    var availableBaysText by remember { mutableStateOf((initial?.availableBays ?: 4).toString()) }
    var chargerType by remember { mutableStateOf(initial?.chargerType ?: "DC Fast 30kW + AC Type-2") }
    var isPrimary by remember { mutableStateOf(initial?.isPrimary ?: false) }

    LaunchedEffect(initial) {
        if (initial != null) {
            name = initial.name
            address = initial.address
            latText = initial.latitude.toString()
            lngText = initial.longitude.toString()
            totalBaysText = initial.totalBays.toString()
            availableBaysText = initial.availableBays.toString()
            chargerType = initial.chargerType
            isPrimary = initial.isPrimary
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Add Station Location" else "Edit Station Location")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("Station 3 — City Center") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (userLocation != null) {
                    TextButton(
                        onClick = {
                            latText = String.format(Locale.US, "%.6f", userLocation.first)
                            lngText = String.format(Locale.US, "%.6f", userLocation.second)
                        }
                    ) {
                        Text("Use Current GPS Coordinates")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalBaysText,
                        onValueChange = { totalBaysText = it.filter(Char::isDigit) },
                        label = { Text("Total Bays") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = availableBaysText,
                        onValueChange = { availableBaysText = it.filter(Char::isDigit) },
                        label = { Text("Free Bays") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = chargerType,
                    onValueChange = { chargerType = it },
                    label = { Text("Charger Spec") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Primary Hub", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: 27.3673
                    val lng = lngText.toDoubleOrNull() ?: 75.4027
                    val total = (totalBaysText.toIntOrNull() ?: 4).coerceAtLeast(1)
                    val free = (availableBaysText.toIntOrNull() ?: total).coerceIn(0, total)
                    onSave(
                        StationLocationEntity(
                            id = initial?.id ?: 0,
                            name = name.trim().ifBlank { "Shree Shyam Branch" },
                            address = address.trim().ifBlank { "Main Highway Road" },
                            latitude = lat,
                            longitude = lng,
                            totalBays = total,
                            availableBays = free,
                            chargerType = chargerType.trim().ifBlank { "DC Fast + AC" },
                            isPrimary = isPrimary,
                            isActive = true
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
