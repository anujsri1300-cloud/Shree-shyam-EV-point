package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.local.StationLocationEntity
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltSuccess
import com.example.util.FormattingUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@Composable
fun LocationsMapScreen(
    locations: List<StationLocationEntity>,
    selectedStationId: Int?,
    userLocation: Pair<Double, Double>?,
    ratePerUnit: Double,
    onSelectStation: (Int) -> Unit,
    onUserLocationFound: (Double, Double) -> Unit,
    onRechargeAtStation: (Int) -> Unit,
    onStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeLocation = remember(locations, selectedStationId) {
        locations.firstOrNull { it.id == selectedStationId } ?: locations.firstOrNull()
    }
    var hasLocationPermission by remember {
        mutableStateOf(hasAnyLocationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchCurrentLocation(
                context = context,
                onSuccess = { lat, lng ->
                    onUserLocationFound(lat, lng)
                    onStatusMessage("Current location updated")
                },
                onError = {
                    onStatusMessage("Unable to fetch GPS fix right now")
                }
            )
        } else {
            onStatusMessage("Location permission denied; showing station coordinates")
        }
    }

    val hasConfiguredMapsKey = remember {
        val key = BuildConfig.MAPS_API_KEY
        key.isNotBlank() && key != "MAPS_API_KEY_DEFAULT_VALUE"
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MapViewportCard(
                    locations = locations,
                    activeLocation = activeLocation,
                    hasLocationPermission = hasLocationPermission,
                    hasConfiguredMapsKey = hasConfiguredMapsKey,
                    onSelectStation = onSelectStation,
                    onLocateMe = {
                        if (hasLocationPermission) {
                            fetchCurrentLocation(context, onUserLocationFound) {
                                onStatusMessage("Unable to fetch GPS fix right now")
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenExternalMaps = { loc -> openInGoogleMaps(context, loc, onStatusMessage) },
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                )
                StationListColumn(
                    locations = locations,
                    activeStationId = activeLocation?.id,
                    userLocation = userLocation,
                    ratePerUnit = ratePerUnit,
                    onSelectStation = onSelectStation,
                    onOpenExternalMaps = { loc -> openInGoogleMaps(context, loc, onStatusMessage) },
                    onRechargeAtStation = onRechargeAtStation,
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Station Locations",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Google Maps • ${locations.size} Shree Shyam Hubs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (hasLocationPermission) {
                                fetchCurrentLocation(context, onUserLocationFound) {
                                    onStatusMessage("Unable to fetch GPS fix right now")
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("locate_me_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Locate Me",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Locate Me")
                    }
                }

                if (locations.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(locations, key = { it.id }) { loc ->
                            FilterChip(
                                selected = loc.id == activeLocation?.id,
                                onClick = { onSelectStation(loc.id) },
                                label = {
                                    Text(
                                        text = loc.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
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

                MapViewportCard(
                    locations = locations,
                    activeLocation = activeLocation,
                    hasLocationPermission = hasLocationPermission,
                    hasConfiguredMapsKey = hasConfiguredMapsKey,
                    onSelectStation = onSelectStation,
                    onLocateMe = {
                        if (hasLocationPermission) {
                            fetchCurrentLocation(context, onUserLocationFound) {
                                onStatusMessage("Unable to fetch GPS fix right now")
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenExternalMaps = { loc -> openInGoogleMaps(context, loc, onStatusMessage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                StationListColumn(
                    locations = locations,
                    activeStationId = activeLocation?.id,
                    userLocation = userLocation,
                    ratePerUnit = ratePerUnit,
                    onSelectStation = onSelectStation,
                    onOpenExternalMaps = { loc -> openInGoogleMaps(context, loc, onStatusMessage) },
                    onRechargeAtStation = onRechargeAtStation,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MapViewportCard(
    locations: List<StationLocationEntity>,
    activeLocation: StationLocationEntity?,
    hasLocationPermission: Boolean,
    hasConfiguredMapsKey: Boolean,
    onSelectStation: (Int) -> Unit,
    onLocateMe: () -> Unit,
    onOpenExternalMaps: (StationLocationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = modifier.testTag("google_map_container")
    ) {
        if (hasConfiguredMapsKey && activeLocation != null) {
            val defaultLatLng = LatLng(activeLocation.latitude, activeLocation.longitude)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(defaultLatLng, 13f)
            }

            LaunchedEffect(activeLocation.id, activeLocation.latitude, activeLocation.longitude) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(activeLocation.latitude, activeLocation.longitude),
                        13.5f
                    )
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false
                    )
                ) {
                    locations.forEach { loc ->
                        Marker(
                            state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                            title = loc.name,
                            snippet = "${loc.availableBays}/${loc.totalBays} bays free • ${loc.chargerType}",
                            onClick = {
                                onSelectStation(loc.id)
                                false
                            }
                        )
                    }
                }
            }
        } else {
            StationMapPreviewFallback(
                locations = locations,
                activeLocation = activeLocation,
                onSelectStation = onSelectStation,
                onLocateMe = onLocateMe,
                onOpenExternalMaps = onOpenExternalMaps
            )
        }
    }
}

@Composable
private fun StationMapPreviewFallback(
    locations: List<StationLocationEntity>,
    activeLocation: StationLocationEntity?,
    onSelectStation: (Int) -> Unit,
    onLocateMe: () -> Unit,
    onOpenExternalMaps: (StationLocationEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0E2922), Color(0xFF15384B))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Add MAPS_API_KEY in Secrets for live tiles",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD6EFE4),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    color = VoltAmber,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onLocateMe)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Locate Me",
                        tint = Color(0xFF231300),
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                locations.take(3).forEach { loc ->
                    val selected = loc.id == activeLocation?.id
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xCC1B2B34),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectStation(loc.id) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = loc.name,
                                tint = if (selected) VoltAmber else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = loc.name.substringBefore("—").trim(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (activeLocation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeLocation.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.4f° N, %.4f° E",
                                activeLocation.latitude,
                                activeLocation.longitude
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9AC8F0)
                        )
                    }
                    Button(
                        onClick = { onOpenExternalMaps(activeLocation) },
                        modifier = Modifier.testTag("open_google_maps_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Navigate")
                    }
                }
            }
        }
    }
}

@Composable
private fun StationListColumn(
    locations: List<StationLocationEntity>,
    activeStationId: Int?,
    userLocation: Pair<Double, Double>?,
    ratePerUnit: Double,
    onSelectStation: (Int) -> Unit,
    onOpenExternalMaps: (StationLocationEntity) -> Unit,
    onRechargeAtStation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(locations, key = { it.id }) { loc ->
            val isSelected = loc.id == activeStationId
            val distanceText = userLocation?.let { (uLat, uLng) ->
                val km = FormattingUtils.distanceKm(uLat, uLng, loc.latitude, loc.longitude)
                String.format(Locale.US, "%.1f km away", km)
            }

            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectStation(loc.id) }
                    .testTag("station_card_${loc.id}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EvStation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = loc.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = VoltSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${loc.availableBays}/${loc.totalBays} Free",
                                style = MaterialTheme.typography.labelMedium,
                                color = VoltSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = loc.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = loc.chargerType,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = distanceText ?: "Rate: ${FormattingUtils.formatInr(ratePerUnit)}/u",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpenExternalMaps(loc) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Directions,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Maps")
                        }
                        Button(
                            onClick = { onRechargeAtStation(loc.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recharge")
                        }
                    }
                }
            }
        }
    }
}

private fun hasAnyLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: Context,
    onSuccess: (Double, Double) -> Unit,
    onError: () -> Unit
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    onError()
                }
            }
            .addOnFailureListener {
                onError()
            }
    } catch (_: Exception) {
        onError()
    }
}

private fun openInGoogleMaps(
    context: Context,
    loc: StationLocationEntity,
    onStatusMessage: (String) -> Unit
) {
    val encodedLabel = Uri.encode(loc.name)
    val geoUri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}($encodedLabel)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: Exception) {
            onStatusMessage("No Maps application found on this device")
        }
    }
}
