package com.example.myapplication2sheepfarm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AlertType {
    INFO, WARNING, CRITICAL
}

enum class AppLanguage {
    ENGLISH, HINDI, TELUGU
}

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

data class FarmAlert(
    val id: String,
    val title: String,
    val message: String,
    val type: AlertType,
    val dateStr: String
)

// Auth screen states
enum class AuthScreenState {
    LOGIN,        // Enter email or phone to request OTP
    OTP_VERIFY,   // Enter OTP code
    REGISTER      // Create new account (full name + email + phone)
}

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper(application)

    // State Flows for database records
    private val _animals = MutableStateFlow<List<Animal>>(emptyList())
    val animals: StateFlow<List<Animal>> = _animals.asStateFlow()

    private val _vaccinationRecords = MutableStateFlow<List<VaccinationRecord>>(emptyList())
    val vaccinationRecords: StateFlow<List<VaccinationRecord>> = _vaccinationRecords.asStateFlow()

    private val _dewormingRecords = MutableStateFlow<List<DewormingRecord>>(emptyList())
    val dewormingRecords: StateFlow<List<DewormingRecord>> = _dewormingRecords.asStateFlow()

    private val _breedingRecords = MutableStateFlow<List<BreedingRecord>>(emptyList())
    val breedingRecords: StateFlow<List<BreedingRecord>> = _breedingRecords.asStateFlow()

    private val _feedInventory = MutableStateFlow<List<FeedInventory>>(emptyList())
    val feedInventory: StateFlow<List<FeedInventory>> = _feedInventory.asStateFlow()

    private val _feedSchedules = MutableStateFlow<List<FeedSchedule>>(emptyList())
    val feedSchedules: StateFlow<List<FeedSchedule>> = _feedSchedules.asStateFlow()

    private val _feedConsumption = MutableStateFlow<List<FeedConsumption>>(emptyList())
    val feedConsumption: StateFlow<List<FeedConsumption>> = _feedConsumption.asStateFlow()

    private val _financialRecords = MutableStateFlow<List<FinancialRecord>>(emptyList())
    val financialRecords: StateFlow<List<FinancialRecord>> = _financialRecords.asStateFlow()

    // Active Alerts State Flow
    private val _alerts = MutableStateFlow<List<FarmAlert>>(emptyList())
    val alerts: StateFlow<List<FarmAlert>> = _alerts.asStateFlow()

    // Current Virtual / System Date for demonstration purposes
    private val _currentDate = MutableStateFlow("2026-06-16")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppTheme.SYSTEM)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    // ==========================================
    // AUTHENTICATION STATE (OTP-based)
    // ==========================================

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Which auth screen to show
    private val _authScreenState = MutableStateFlow(AuthScreenState.LOGIN)
    val authScreenState: StateFlow<AuthScreenState> = _authScreenState.asStateFlow()

    // OTP session — stored in-memory only (not persisted)
    private var _activeOtpSession: OtpSession? = null

    // Whether an OTP has been sent and we are waiting for user entry
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    // The identifier (email or phone) that the OTP was sent to
    private val _otpIdentifier = MutableStateFlow("")
    val otpIdentifier: StateFlow<String> = _otpIdentifier.asStateFlow()

    // OTP type (EMAIL or PHONE)
    private val _otpType = MutableStateFlow(OtpType.EMAIL)
    val otpType: StateFlow<OtpType> = _otpType.asStateFlow()

    // Countdown in seconds (300 = 5 min)
    private val _otpCountdown = MutableStateFlow(0)
    val otpCountdown: StateFlow<Int> = _otpCountdown.asStateFlow()

    // Whether resend button is enabled (after 60s or expiry)
    private val _resendEnabled = MutableStateFlow(false)
    val resendEnabled: StateFlow<Boolean> = _resendEnabled.asStateFlow()

    // Error message for auth flows
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Success message
    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess: StateFlow<String?> = _authSuccess.asStateFlow()

    // Pending registration info (held during OTP verify for new user)
    private var _pendingRegistration: Triple<String, String, String>? = null // fullName, email, phone

    // Whether we are in the middle of a registration OTP flow (vs login OTP flow)
    private val _isRegistrationFlow = MutableStateFlow(false)
    val isRegistrationFlow: StateFlow<Boolean> = _isRegistrationFlow.asStateFlow()

    private var countdownJob: Job? = null

    // Connection Sync State simulation
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncMessage = MutableStateFlow("Database synced locally (Offline Mode ready)")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // List of annual schedules
    val annualSchedules = listOf(
        VaccinationSchedule("02-05", "FMD Vaccination", "Foot and Mouth Disease", true),
        VaccinationSchedule("03-05", "Sheep & Goat Pox Vaccination", "Capripoxvirus (Pox)", false),
        VaccinationSchedule("04-05", "HS Vaccination", "Hemorrhagic Septicemia", true),
        VaccinationSchedule("05-05", "ET + TT Vaccination", "Enterotoxemia + Tetanus Toxoid", false),
        VaccinationSchedule("07-05", "Bluetongue Vaccination", "Bluetongue Virus", true),
        VaccinationSchedule("08-05", "FMD Vaccination", "Foot and Mouth Disease Boost", false),
        VaccinationSchedule("09-05", "PPR Vaccination", "Peste des Petits Ruminants", true),
        VaccinationSchedule("10-05", "HS Vaccination", "Hemorrhagic Septicemia", false),
        VaccinationSchedule("11-05", "ET + TT Vaccination", "Enterotoxemia + Tetanus Toxoid Boost", true)
    )

    init {
        loadLanguagePreference()
        loadThemePreference()
        loadSimulatedDate()
        loadUserSession()
        loadData()
    }

    private fun loadUserSession() {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        val email = prefs.getString("logged_in_user_email", null)
        if (email != null) {
            viewModelScope.launch {
                val user = dbHelper.getUserByEmail(email)
                if (user != null) {
                    _currentUser.value = user
                } else {
                    prefs.edit().remove("logged_in_user_email").apply()
                }
            }
        }
    }

    // ==========================================
    // AUTH SCREEN NAVIGATION
    // ==========================================

    fun showLoginScreen() {
        _authScreenState.value = AuthScreenState.LOGIN
        _authError.value = null
        _authSuccess.value = null
        _otpSent.value = false
        cancelCountdown()
    }

    fun showRegisterScreen() {
        _authScreenState.value = AuthScreenState.REGISTER
        _authError.value = null
        _authSuccess.value = null
        _otpSent.value = false
        cancelCountdown()
    }

    // ==========================================
    // OTP: SEND (LOGIN)
    // ==========================================

    /**
     * Called when the user presses "Send OTP" on the login screen.
     * Looks up the user by email or phone, then sends OTP if account found.
     */
    fun sendLoginOtp(identifier: String, type: OtpType) {
        viewModelScope.launch {
            _authError.value = null
            val trimmed = identifier.trim()
            if (trimmed.isBlank()) {
                _authError.value = if (type == OtpType.EMAIL) "Please enter your email address" else "Please enter your phone number"
                return@launch
            }
            if (type == OtpType.EMAIL && !isValidEmail(trimmed)) {
                _authError.value = "Please enter a valid email address"
                return@launch
            }

            val user = if (type == OtpType.EMAIL) dbHelper.getUserByEmail(trimmed) else dbHelper.getUserByPhone(trimmed)
            if (user == null) {
                _authError.value = "No account found with this ${if (type == OtpType.EMAIL) "email" else "phone number"}. Please create an account."
                return@launch
            }

            _isRegistrationFlow.value = false
            generateAndSendOtp(trimmed, type)
        }
    }

    // ==========================================
    // OTP: SEND (REGISTRATION)
    // ==========================================

    /**
     * Called when user fills in registration form and taps "Send OTP".
     * Validates form, checks for duplicates, sends OTP.
     */
    fun sendRegistrationOtp(fullName: String, email: String, phone: String, otpViaType: OtpType) {
        viewModelScope.launch {
            _authError.value = null
            val name = fullName.trim()
            val mail = email.trim()
            val ph = phone.trim()

            if (name.isBlank()) { _authError.value = "Full name is required"; return@launch }
            if (mail.isBlank()) { _authError.value = "Email address is required"; return@launch }
            if (!isValidEmail(mail)) { _authError.value = "Please enter a valid email address"; return@launch }
            if (ph.isBlank()) { _authError.value = "Phone number is required"; return@launch }
            if (ph.length < 10) { _authError.value = "Please enter a valid phone number (min 10 digits)"; return@launch }

            if (dbHelper.isEmailRegistered(mail)) { _authError.value = "This email is already registered. Please sign in."; return@launch }
            if (dbHelper.isPhoneRegistered(ph)) { _authError.value = "This phone number is already registered. Please sign in."; return@launch }

            _pendingRegistration = Triple(name, mail, ph)
            _isRegistrationFlow.value = true

            val identifier = if (otpViaType == OtpType.EMAIL) mail else ph
            generateAndSendOtp(identifier, otpViaType)
        }
    }

    // ==========================================
    // OTP: GENERATE & NOTIFY
    // ==========================================

    private fun generateAndSendOtp(identifier: String, type: OtpType) {
        val otp = (100000..999999).random().toString()
        val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000L) // 5 minutes

        _activeOtpSession = OtpSession(
            identifier = identifier,
            otpCode = otp,
            expiresAt = expiresAt,
            type = type
        )

        _otpIdentifier.value = identifier
        _otpType.value = type
        _otpSent.value = true
        _resendEnabled.value = false
        _authError.value = null
        _authScreenState.value = AuthScreenState.OTP_VERIFY

        // Post OTP as notification (simulates email/SMS delivery)
        val medium = if (type == OtpType.EMAIL) "Email" else "SMS"
        val maskedId = maskIdentifier(identifier, type)
        postSystemNotification(
            id = "otp_${System.currentTimeMillis()}",
            title = "🔐 Your Smart Sheep Farm OTP",
            message = "Your OTP for login is: $otp\n\nSent to: $maskedId ($medium)\nThis OTP expires in 5 minutes.\n\n(In a real app this would be sent via email/SMS)"
        )

        // Start countdown
        startCountdown()
    }

    // ==========================================
    // OTP: VERIFY
    // ==========================================

    /**
     * Verifies the OTP entered by the user.
     */
    fun verifyOtp(enteredCode: String) {
        viewModelScope.launch {
            _authError.value = null
            val session = _activeOtpSession
            if (session == null) {
                _authError.value = "No OTP session active. Please request a new OTP."
                return@launch
            }
            if (System.currentTimeMillis() > session.expiresAt) {
                _authError.value = "OTP has expired. Please request a new OTP."
                _activeOtpSession = null
                _resendEnabled.value = true
                return@launch
            }
            if (enteredCode.trim() != session.otpCode) {
                _authError.value = "Incorrect OTP. Please try again."
                return@launch
            }

            // OTP is correct!
            cancelCountdown()
            _activeOtpSession = null

            if (_isRegistrationFlow.value) {
                // Complete registration
                val pending = _pendingRegistration
                if (pending != null) {
                    val (fullName, email, phone) = pending
                    val result = dbHelper.registerUser(fullName, email, phone)
                    if (result != -1L) {
                        val newUser = dbHelper.getUserByEmail(email)
                        if (newUser != null) {
                            loginUser(newUser)
                            _authSuccess.value = "Account created successfully! Welcome, ${newUser.fullName}."
                        }
                        _pendingRegistration = null
                    } else {
                        _authError.value = "Failed to create account. Please try again."
                    }
                }
            } else {
                // Complete login — find user from identifier
                val user = if (session.type == OtpType.EMAIL) {
                    dbHelper.getUserByEmail(session.identifier)
                } else {
                    dbHelper.getUserByPhone(session.identifier)
                }
                if (user != null) {
                    loginUser(user)
                    _authSuccess.value = "Welcome back, ${user.fullName}!"
                } else {
                    _authError.value = "Could not find account. Please try again."
                }
            }
        }
    }

    private fun loginUser(user: User) {
        _currentUser.value = user
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("logged_in_user_email", user.email).apply()

        postSystemNotification(
            id = "login_${System.currentTimeMillis()}",
            title = "✅ Login Successful",
            message = "Welcome back, ${user.fullName}! You have logged in to Smart Sheep Farm."
        )
    }

    // ==========================================
    // OTP: RESEND
    // ==========================================

    fun resendOtp() {
        val session = _activeOtpSession
        if (session != null) {
            generateAndSendOtp(session.identifier, session.type)
        } else {
            val identifier = _otpIdentifier.value
            val type = _otpType.value
            if (identifier.isNotBlank()) {
                generateAndSendOtp(identifier, type)
            }
        }
    }

    // ==========================================
    // COUNTDOWN TIMER
    // ==========================================

    private fun startCountdown() {
        cancelCountdown()
        _otpCountdown.value = 300 // 5 minutes in seconds
        countdownJob = viewModelScope.launch {
            var seconds = 300
            while (seconds > 0) {
                delay(1000L)
                seconds--
                _otpCountdown.value = seconds
                // Enable resend button after 60 seconds
                if (seconds == 240) {
                    _resendEnabled.value = true
                }
            }
            // Timer expired
            _resendEnabled.value = true
            _authError.value = "OTP has expired. Please tap Resend OTP."
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    fun logout() {
        _currentUser.value = null
        _authError.value = null
        _authSuccess.value = null
        _otpSent.value = false
        _activeOtpSession = null
        _pendingRegistration = null
        _isRegistrationFlow.value = false
        _authScreenState.value = AuthScreenState.LOGIN
        cancelCountdown()
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("logged_in_user_email").apply()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearAuthSuccess() {
        _authSuccess.value = null
    }

    // ==========================================
    // UTILITY
    // ==========================================

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun maskIdentifier(identifier: String, type: OtpType): String {
        return if (type == OtpType.EMAIL) {
            val parts = identifier.split("@")
            if (parts.size == 2) {
                val name = parts[0]
                val masked = name.take(2) + "*".repeat((name.length - 2).coerceAtLeast(1)) + "@" + parts[1]
                masked
            } else identifier
        } else {
            // Mask phone: show last 4 digits
            if (identifier.length > 4) {
                "*".repeat(identifier.length - 4) + identifier.takeLast(4)
            } else identifier
        }
    }

    // ==========================================
    // LANGUAGE / THEME
    // ==========================================

    private fun loadLanguagePreference() {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        val langStr = prefs.getString("selected_language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        _currentLanguage.value = try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", lang.name).apply()
    }

    private fun loadThemePreference() {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        val themeStr = prefs.getString("selected_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        _currentTheme.value = try {
            AppTheme.valueOf(themeStr)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("selected_theme", theme.name).apply()
    }

    fun loadData() {
        viewModelScope.launch {
            _animals.value = dbHelper.getAllAnimals()
            _vaccinationRecords.value = dbHelper.getAllVaccinationRecords()
            _dewormingRecords.value = dbHelper.getAllDewormingRecords()
            _breedingRecords.value = dbHelper.getAllBreedingRecords()
            _feedInventory.value = dbHelper.getAllFeedInventory()
            _feedSchedules.value = dbHelper.getAllFeedSchedules()
            _feedConsumption.value = dbHelper.getAllFeedConsumption()
            _financialRecords.value = dbHelper.getAllFinancialRecords()

            generateAlerts()
        }
    }

    // Toggle simulated internet state
    fun toggleConnection() {
        _isOnline.value = !_isOnline.value
        if (_isOnline.value) {
            _syncMessage.value = "Data synchronized successfully with server!"
        } else {
            _syncMessage.value = "Offline mode active. Saving data locally."
        }
    }

    private fun loadSimulatedDate() {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        _currentDate.value = prefs.getString("simulated_date", "2026-06-16") ?: "2026-06-16"
    }

    fun setSimulatedDate(newDate: String) {
        try {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            df.parse(newDate)
            _currentDate.value = newDate
            val context = getApplication<Application>().applicationContext
            context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("simulated_date", newDate)
                .remove("notified_alerts")
                .apply()
            generateAlerts()
        } catch (e: Exception) {
            Log.e("FarmViewModel", "Invalid date format: $newDate")
        }
    }

    private fun generateAlerts() {
        val alertList = mutableListOf<FarmAlert>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val curDateStr = _currentDate.value

        val todayDate: Date = try {
            sdf.parse(curDateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val calToday = Calendar.getInstance()
        calToday.time = todayDate
        val currentYear = calToday.get(Calendar.YEAR)

        val animalsList = _animals.value
        val vRecords = _vaccinationRecords.value
        val dRecords = _dewormingRecords.value

        annualSchedules.forEach { schedule ->
            val targetDateStr = "$currentYear-${schedule.dateMonthDay}"
            val targetDate = sdf.parse(targetDateStr) ?: return@forEach

            val diffMs = targetDate.time - todayDate.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            if (diffDays == 7) {
                alertList.add(FarmAlert(
                    id = "rem_7_${schedule.dateMonthDay}",
                    title = "Vaccination Reminder (In 7 Days)",
                    message = "${schedule.vaccineName} for ${schedule.targetDisease} is scheduled on $targetDateStr.",
                    type = AlertType.INFO, dateStr = targetDateStr
                ))
            } else if (diffDays == 1) {
                alertList.add(FarmAlert(
                    id = "rem_1_${schedule.dateMonthDay}",
                    title = "Vaccination Alert (Tomorrow)",
                    message = "${schedule.vaccineName} is due tomorrow ($targetDateStr). Prepare livestock records.",
                    type = AlertType.WARNING, dateStr = targetDateStr
                ))
            } else if (diffDays == 0) {
                alertList.add(FarmAlert(
                    id = "rem_0_${schedule.dateMonthDay}",
                    title = "Vaccination Day!",
                    message = "${schedule.vaccineName} is due TODAY ($targetDateStr). Remember to log details after administration.",
                    type = AlertType.CRITICAL, dateStr = targetDateStr
                ))
            }

            if (diffDays < 0 && animalsList.isNotEmpty()) {
                val dateStart = Calendar.getInstance().apply { time = targetDate; add(Calendar.DAY_OF_YEAR, -5) }.time
                val dateEnd = Calendar.getInstance().apply { time = targetDate; add(Calendar.DAY_OF_YEAR, 10) }.time
                val vaccinatedCount = animalsList.count { animal ->
                    vRecords.any { rec ->
                        rec.animalId == animal.id &&
                                rec.vaccineName.contains(schedule.vaccineName.split(" ")[0], ignoreCase = true) &&
                                isDateWithinRange(rec.dateAdministered, dateStart, dateEnd)
                    }
                }
                if (vaccinatedCount < animalsList.size) {
                    val missedCount = animalsList.size - vaccinatedCount
                    alertList.add(FarmAlert(
                        id = "miss_${schedule.dateMonthDay}",
                        title = "Missed Vaccination",
                        message = "${schedule.vaccineName} scheduled on $targetDateStr has pending records ($vaccinatedCount/${animalsList.size} administered). $missedCount animals missed.",
                        type = AlertType.CRITICAL, dateStr = targetDateStr
                    ))
                }
            }

            if (schedule.dewormingRequired && diffDays <= 0 && animalsList.isNotEmpty()) {
                val dateDewormStart = targetDate
                val dateDewormEnd = Calendar.getInstance().apply { time = targetDate; add(Calendar.DAY_OF_YEAR, 14) }.time
                val dewormedCount = animalsList.count { animal ->
                    dRecords.any { rec ->
                        rec.animalId == animal.id && isDateWithinRange(rec.dateAdministered, dateDewormStart, dateDewormEnd)
                    }
                }
                if (dewormedCount < animalsList.size) {
                    val pendingDeworm = animalsList.size - dewormedCount
                    alertList.add(FarmAlert(
                        id = "deworm_${schedule.dateMonthDay}",
                        title = "Pending Deworming Alert",
                        message = "Deworming activity scheduled for ${schedule.vaccineName} ($targetDateStr) is pending for $pendingDeworm animals.",
                        type = AlertType.WARNING, dateStr = targetDateStr
                    ))
                }
            }
        }

        _feedInventory.value.forEach { feed ->
            if (feed.quantityInStock <= feed.lowStockThreshold) {
                alertList.add(FarmAlert(
                    id = "feed_low_${feed.id}",
                    title = "Low Feed Stock Alert",
                    message = "${feed.feedName} has only ${feed.quantityInStock}${feed.unit} left (Threshold: ${feed.lowStockThreshold}${feed.unit}).",
                    type = AlertType.WARNING, dateStr = curDateStr
                ))
            }
        }

        _alerts.value = alertList

        val notifiedIds = getNotifiedAlertIds().toMutableSet()
        var updated = false
        alertList.forEach { alert ->
            if (!notifiedIds.contains(alert.id)) {
                postSystemNotification(alert.id, alert.title, alert.message)
                notifiedIds.add(alert.id)
                updated = true
            }
        }
        if (updated) saveNotifiedAlertIds(notifiedIds)
    }

    private fun getNotifiedAlertIds(): Set<String> {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getStringSet("notified_alerts", emptySet()) ?: emptySet()
    }

    private fun saveNotifiedAlertIds(ids: Set<String>) {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("notified_alerts", ids).apply()
    }

    private fun postSystemNotification(id: String, title: String, message: String) {
        val context = getApplication<Application>().applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("FarmViewModel", "Missing POST_NOTIFICATIONS permission. Skipping notification for $id")
                return
            }
        }
        try {
            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, id.hashCode(), intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val builder = androidx.core.app.NotificationCompat.Builder(context, "farm_alerts_channel_v3")
                .setSmallIcon(context.resources.getIdentifier("ic_launcher_foreground", "drawable", context.packageName).let { if (it != 0) it else android.R.drawable.ic_dialog_info })
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
            notificationManager.notify(id.hashCode(), builder.build())
        } catch (e: Exception) {
            Log.e("FarmViewModel", "Error posting notification: ${e.message}")
        }
    }

    private fun isDateWithinRange(dateToCheckStr: String, start: Date, end: Date): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateToCheckStr)
            date != null && !date.before(start) && !date.after(end)
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // ANIMAL OPERATIONS
    // ==========================================
    fun addAnimal(tag: String, type: AnimalType, breed: String, gender: Gender, ageCat: AgeCategory, weight: Float, health: String, pDate: String, pPrice: Float, avatar: Int) {
        viewModelScope.launch {
            dbHelper.insertAnimal(Animal(tagNumber = tag, type = type, breed = breed, gender = gender, ageCategory = ageCat, weight = weight, healthStatus = health, purchaseDate = pDate, purchasePrice = pPrice, avatarId = avatar))
            loadData()
        }
    }

    fun removeAnimal(id: Long) {
        viewModelScope.launch {
            dbHelper.deleteAnimal(id)
            loadData()
        }
    }

    // ==========================================
    // HEALTH LOG OPERATIONS
    // ==========================================
    fun logVaccination(animalId: Long, vaccine: String, date: String, notes: String) {
        viewModelScope.launch {
            dbHelper.insertVaccinationRecord(VaccinationRecord(animalId = animalId, vaccineName = vaccine, dateAdministered = date, notes = notes))
            loadData()
        }
    }

    fun logDeworming(animalId: Long, drug: String, date: String, notes: String) {
        viewModelScope.launch {
            dbHelper.insertDewormingRecord(DewormingRecord(animalId = animalId, drugUsed = drug, dateAdministered = date, notes = notes))
            loadData()
        }
    }

    // ==========================================
    // BREEDING OPERATIONS
    // ==========================================
    fun logBreeding(femaleId: Long, maleId: Long, date: String, status: PregnancyStatus, notes: String) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val breedDate = sdf.parse(date) ?: Date()
            val cal = Calendar.getInstance().apply { time = breedDate; add(Calendar.DAY_OF_YEAR, 150) }
            val expectedDel = sdf.format(cal.time)
            dbHelper.insertBreedingRecord(BreedingRecord(femaleId = femaleId, maleId = maleId, breedingDate = date, expectedDeliveryDate = expectedDel, status = status, notes = notes))
            loadData()
        }
    }

    fun recordDelivery(record: BreedingRecord, deliveryDate: String, offspringCount: Int, notes: String) {
        viewModelScope.launch {
            val status = if (animals.value.find { it.id == record.femaleId }?.type == AnimalType.SHEEP) PregnancyStatus.LAMBED else PregnancyStatus.KIDDED
            val updatedRecord = record.copy(status = status, birthDate = deliveryDate, offspringCount = offspringCount, notes = notes)
            dbHelper.updateBreedingRecord(updatedRecord)
            val mother = animals.value.find { it.id == record.femaleId }
            if (mother != null) {
                for (i in 1..offspringCount) {
                    val offspringTag = "${if (mother.type == AnimalType.SHEEP) "SH" else "GT"}-B${record.id}-$i"
                    dbHelper.insertAnimal(Animal(tagNumber = offspringTag, type = mother.type, breed = mother.breed, gender = if (i % 2 == 0) Gender.FEMALE else Gender.MALE, ageCategory = AgeCategory.BABY, weight = 3.5f, healthStatus = "Healthy", purchaseDate = deliveryDate, purchasePrice = 0f, avatarId = if (mother.type == AnimalType.SHEEP) 0 else 2))
                }
            }
            loadData()
        }
    }

    // ==========================================
    // FEEDING OPERATIONS
    // ==========================================
    fun addFeedInventory(feedName: String, qty: Float, unit: String, lowThreshold: Float) {
        viewModelScope.launch {
            dbHelper.insertFeedInventory(FeedInventory(feedName = feedName, quantityInStock = qty, unit = unit, lowStockThreshold = lowThreshold))
            loadData()
        }
    }

    fun logFeedConsumption(feedName: String, qty: Float, date: String) {
        viewModelScope.launch {
            dbHelper.insertFeedConsumption(FeedConsumption(date = date, feedName = feedName, quantityConsumed = qty))
            loadData()
        }
    }

    fun addFeedSchedule(feedName: String, time: String, qty: Float, group: String) {
        viewModelScope.launch {
            dbHelper.insertFeedSchedule(FeedSchedule(feedName = feedName, timeOfDay = time, quantity = qty, targetGroup = group))
            loadData()
        }
    }

    fun purchaseFeedDeal(feedName: String, qty: Float, price: Float) {
        viewModelScope.launch {
            dbHelper.insertFinancialRecord(FinancialRecord(type = TransactionType.EXPENSE, category = TransactionCategory.FEED_EXPENSE, amount = price, date = _currentDate.value, description = "Purchased $feedName Bulk Deal"))
            val inventory = dbHelper.getAllFeedInventory()
            val currentItem = inventory.find { it.feedName.equals(feedName, ignoreCase = true) }
            if (currentItem != null) {
                dbHelper.updateFeedStock(currentItem.feedName, currentItem.quantityInStock + qty)
            } else {
                dbHelper.insertFeedInventory(FeedInventory(feedName = feedName, quantityInStock = qty, unit = if (feedName.contains("Block", ignoreCase = true)) "pcs" else "kg", lowStockThreshold = if (feedName.contains("Block", ignoreCase = true)) 2f else 50f))
            }
            loadData()
        }
    }

    // ==========================================
    // FINANCIAL OPERATIONS
    // ==========================================
    fun addFinancialRecord(type: TransactionType, category: TransactionCategory, amount: Float, date: String, desc: String) {
        viewModelScope.launch {
            dbHelper.insertFinancialRecord(FinancialRecord(type = type, category = category, amount = amount, date = date, description = desc))
            loadData()
        }
    }
}
