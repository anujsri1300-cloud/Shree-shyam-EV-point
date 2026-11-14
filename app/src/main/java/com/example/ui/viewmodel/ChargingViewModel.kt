package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.RechargeTransactionEntity
import com.example.data.local.StationConfigEntity
import com.example.data.local.StationLocationEntity
import com.example.data.repository.ChargingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveChargingSession(
    val transactionId: Int,
    val receiptCode: String,
    val stationName: String,
    val vehicleNumber: String,
    val paymentMethod: String,
    val ratePerUnit: Double,
    val targetUnits: Double,
    val targetAmountInr: Double,
    val deliveredUnits: Double = 0.0,
    val consumedAmountInr: Double = 0.0,
    val isRunning: Boolean = true,
    val isCompletedAtLimit: Boolean = false
)

data class DashboardSummary(
    val totalRevenueInr: Double = 0.0,
    val upiRevenueInr: Double = 0.0,
    val cashRevenueInr: Double = 0.0,
    val totalUnitsKwh: Double = 0.0,
    val upiCount: Int = 0,
    val cashCount: Int = 0
)

class ChargingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChargingRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ChargingRepository(
            configDao = db.stationConfigDao(),
            locationDao = db.stationLocationDao(),
            transactionDao = db.transactionDao()
        )
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    val stationConfig: StateFlow<StationConfigEntity> = repository.stationConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StationConfigEntity())

    val locations: StateFlow<List<StationLocationEntity>> = repository.stationLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<RechargeTransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<DashboardSummary> = transactions
        .combine(MutableStateFlow(Unit)) { list, _ ->
            var totalRev = 0.0
            var upiRev = 0.0
            var cashRev = 0.0
            var totalUnits = 0.0
            var upiCount = 0
            var cashCount = 0
            for (tx in list) {
                totalRev += tx.amountInr
                totalUnits += tx.unitsPurchased
                if (tx.paymentMethod == "UPI") {
                    upiRev += tx.amountInr
                    upiCount++
                } else {
                    cashRev += tx.amountInr
                    cashCount++
                }
            }
            DashboardSummary(
                totalRevenueInr = ChargingRepository.roundTwoDecimals(totalRev),
                upiRevenueInr = ChargingRepository.roundTwoDecimals(upiRev),
                cashRevenueInr = ChargingRepository.roundTwoDecimals(cashRev),
                totalUnitsKwh = ChargingRepository.roundTwoDecimals(totalUnits),
                upiCount = upiCount,
                cashCount = cashCount
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private val _selectedStationId = MutableStateFlow<Int?>(null)
    val selectedStationId: StateFlow<Int?> = _selectedStationId.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _isOwnerUnlocked = MutableStateFlow(false)
    val isOwnerUnlocked: StateFlow<Boolean> = _isOwnerUnlocked.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveChargingSession?>(null)
    val activeSession: StateFlow<ActiveChargingSession?> = _activeSession.asStateFlow()

    private val _latestReceipt = MutableStateFlow<RechargeTransactionEntity?>(null)
    val latestReceipt: StateFlow<RechargeTransactionEntity?> = _latestReceipt.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var meterJob: Job? = null

    fun selectStation(stationId: Int) {
        _selectedStationId.value = stationId
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = latitude to longitude
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun unlockOwner(enteredPin: String): Boolean {
        val currentPin = stationConfig.value.ownerPin
        return if (enteredPin == currentPin) {
            _isOwnerUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockOwner() {
        _isOwnerUnlocked.value = false
    }

    fun updatePerUnitRate(newRate: Double) {
        if (newRate <= 0.0) {
            _statusMessage.value = "Per-unit rate must be greater than ₹0"
            return
        }
        viewModelScope.launch {
            repository.updateOwnerRate(newRate)
            _statusMessage.value = "Owner rate updated to ₹${String.format(java.util.Locale.US, "%.2f", newRate)}/unit"
        }
    }

    fun saveOwnerSettings(
        businessName: String,
        ratePerUnitInr: Double,
        upiId: String,
        ownerPhone: String,
        ownerPin: String,
        maxUnitsPerSession: Double
    ) {
        viewModelScope.launch {
            repository.updateOwnerSettings(
                businessName = businessName,
                ratePerUnitInr = ratePerUnitInr,
                upiId = upiId,
                ownerPhone = ownerPhone,
                ownerPin = ownerPin,
                maxUnitsPerSession = maxUnitsPerSession
            )
            _statusMessage.value = "Owner settings & per-unit rate saved"
        }
    }

    fun saveStationLocation(location: StationLocationEntity) {
        viewModelScope.launch {
            repository.saveLocation(location)
            _statusMessage.value = "Saved location: ${location.name}"
        }
    }

    fun deleteStationLocation(id: Int) {
        viewModelScope.launch {
            repository.deleteLocation(id)
            _statusMessage.value = "Station location removed"
        }
    }

    fun processRecharge(
        station: StationLocationEntity,
        vehicleNumber: String,
        customerName: String,
        units: Double,
        amountInr: Double,
        paymentMethod: String,
        cashTenderedInr: Double? = null,
        upiReference: String? = null
    ) {
        val rate = stationConfig.value.ratePerUnitInr
        val enforcedAmount = ChargingRepository.calculateAmountForUnits(units, rate)
        val finalAmount = if (amountInr > 0) ChargingRepository.roundTwoDecimals(amountInr) else enforcedAmount
        val finalUnits = ChargingRepository.calculateUnitsForAmount(finalAmount, rate)

        if (finalAmount <= 0.0 || finalUnits <= 0.0) {
            _statusMessage.value = "Enter a valid recharge amount or unit count"
            return
        }

        viewModelScope.launch {
            val tx = repository.recordRecharge(
                station = station,
                vehicleNumber = vehicleNumber,
                customerName = customerName,
                ratePerUnit = rate,
                units = finalUnits,
                amountInr = finalAmount,
                paymentMethod = paymentMethod,
                cashTenderedInr = cashTenderedInr,
                upiReference = upiReference,
                initialStatus = "CHARGING"
            )
            _latestReceipt.value = tx
            startChargingMeter(tx)
        }
    }

    fun dismissReceipt() {
        _latestReceipt.value = null
    }

    fun startChargingMeter(tx: RechargeTransactionEntity) {
        meterJob?.cancel()
        _activeSession.value = ActiveChargingSession(
            transactionId = tx.id,
            receiptCode = tx.receiptCode,
            stationName = tx.stationName,
            vehicleNumber = tx.vehicleNumber,
            paymentMethod = tx.paymentMethod,
            ratePerUnit = tx.ratePerUnitSnapshot,
            targetUnits = tx.unitsPurchased,
            targetAmountInr = tx.amountInr,
            deliveredUnits = 0.0,
            consumedAmountInr = 0.0,
            isRunning = true,
            isCompletedAtLimit = false
        )
        runMeterLoop()
    }

    fun toggleMeterRunning() {
        val current = _activeSession.value ?: return
        if (current.isCompletedAtLimit) return
        val nextRunning = !current.isRunning
        _activeSession.value = current.copy(isRunning = nextRunning)
        if (nextRunning) {
            runMeterLoop()
        } else {
            meterJob?.cancel()
        }
    }

    fun stopAndCompleteSession() {
        meterJob?.cancel()
        val current = _activeSession.value ?: return
        viewModelScope.launch {
            repository.markTransactionStatus(current.transactionId, "COMPLETED")
        }
        _activeSession.value = current.copy(
            isRunning = false,
            isCompletedAtLimit = true
        )
    }

    fun clearCompletedSession() {
        meterJob?.cancel()
        _activeSession.value = null
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    private fun runMeterLoop() {
        meterJob?.cancel()
        meterJob = viewModelScope.launch {
            while (true) {
                delay(400L)
                val session = _activeSession.value ?: break
                if (!session.isRunning || session.isCompletedAtLimit) break

                val stepUnits = (session.targetUnits / 25.0).coerceAtLeast(0.05)
                val nextUnits = (session.deliveredUnits + stepUnits).coerceAtMost(session.targetUnits)
                val nextAmount = ChargingRepository.roundTwoDecimals(
                    (nextUnits * session.ratePerUnit).coerceAtMost(session.targetAmountInr)
                )
                val reachedLimit = nextUnits >= session.targetUnits - 0.001 ||
                    nextAmount >= session.targetAmountInr - 0.01

                if (reachedLimit) {
                    _activeSession.value = session.copy(
                        deliveredUnits = session.targetUnits,
                        consumedAmountInr = session.targetAmountInr,
                        isRunning = false,
                        isCompletedAtLimit = true
                    )
                    repository.markTransactionStatus(session.transactionId, "COMPLETED")
                    break
                } else {
                    _activeSession.value = session.copy(
                        deliveredUnits = ChargingRepository.roundTwoDecimals(nextUnits),
                        consumedAmountInr = nextAmount
                    )
                }
            }
        }
    }
}
