package com.example.myapplication2sheepfarm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    // Current Virtual / System Date for demonstration purposes (allows testing schedule changes)
    private val _currentDate = MutableStateFlow("2026-06-16")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppTheme.SYSTEM)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    // Authentication States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _signupSuccess = MutableStateFlow<Boolean>(false)
    val signupSuccess: StateFlow<Boolean> = _signupSuccess.asStateFlow()

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
        val username = prefs.getString("logged_in_user", null)
        if (username != null) {
            viewModelScope.launch {
                val user = dbHelper.getUserByUsername(username)
                if (user != null) {
                    _currentUser.value = user
                } else {
                    prefs.edit().remove("logged_in_user").apply()
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (username.isBlank() || password.isBlank()) {
                _loginError.value = "Username and password cannot be empty"
                return@launch
            }
            val user = dbHelper.authenticateUser(username, password)
            if (user != null) {
                _currentUser.value = user
                val context = getApplication<Application>().applicationContext
                val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("logged_in_user", user.username).apply()

                // Trigger login notification mentioning user and email
                val emailDisplay = if (user.email.isNotBlank()) user.email else "default admin email"
                AlarmReceiver.postSystemNotification(
                    context,
                    "login_alert_${System.currentTimeMillis()}",
                    "Successful Login Alert",
                    "New login session established for ${user.username} ($emailDisplay)."
                )
            } else {
                _loginError.value = "Invalid username or password"
            }
        }
    }

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
            _signupSuccess.value = false
            if (username.isBlank() || password.isBlank()) {
                _loginError.value = "Username and password cannot be empty"
                return@launch
            }
            if (password.length < 6) {
                _loginError.value = "Password must be at least 6 characters"
                return@launch
            }
            if (dbHelper.isUserExists(username)) {
                _loginError.value = "Username already exists"
                return@launch
            }
            val result = dbHelper.registerUser(username, email, password)
            if (result != -1L) {
                _signupSuccess.value = true
                // Trigger welcome email dispatch notification
                val context = getApplication<Application>().applicationContext
                val targetEmail = if (email.isNotBlank()) email else "your registered email"
                AlarmReceiver.postSystemNotification(
                    context,
                    "signup_verify_${System.currentTimeMillis()}",
                    "Welcome Email Dispatched!",
                    "A confirmation notification email has been dispatched to $targetEmail."
                )
            } else {
                _loginError.value = "Failed to create account. Please try again."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
        _signupSuccess.value = false
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("farm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("logged_in_user").apply()
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun resetSignupSuccess() {
        _signupSuccess.value = false
    }


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

    // Change current simulated date to test schedule alerts
    fun setSimulatedDate(newDate: String) {
        // Validate date format YYYY-MM-DD
        try {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            df.parse(newDate)
            _currentDate.value = newDate
            // Clear notified alerts on date change so user can re-trigger notifications for testing!
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

    // Dynamic Alert Generator based on vaccination schedule and logs
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

        // Loop through the 9 scheduled vaccination events
        annualSchedules.forEach { schedule ->
            val targetDateStr = "$currentYear-${schedule.dateMonthDay}"
            val targetDate = sdf.parse(targetDateStr) ?: return@forEach

            val diffMs = targetDate.time - todayDate.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            // 1. Reminders before date
            if (diffDays == 7) {
                alertList.add(
                    FarmAlert(
                        id = "rem_7_${schedule.dateMonthDay}",
                        title = "Vaccination Reminder (In 7 Days)",
                        message = "${schedule.vaccineName} for ${schedule.targetDisease} is scheduled on $targetDateStr.",
                        type = AlertType.INFO,
                        dateStr = targetDateStr
                    )
                )
            } else if (diffDays == 1) {
                alertList.add(
                    FarmAlert(
                        id = "rem_1_${schedule.dateMonthDay}",
                        title = "Vaccination Alert (Tomorrow)",
                        message = "${schedule.vaccineName} is due tomorrow ($targetDateStr). Prepare livestock records.",
                        type = AlertType.WARNING,
                        dateStr = targetDateStr
                    )
                )
            } else if (diffDays == 0) {
                alertList.add(
                    FarmAlert(
                        id = "rem_0_${schedule.dateMonthDay}",
                        title = "Vaccination Day!",
                        message = "${schedule.vaccineName} is due TODAY ($targetDateStr). Remember to log details after administration.",
                        type = AlertType.CRITICAL,
                        dateStr = targetDateStr
                    )
                )
            }

            // 2. Alerts for Missed Vaccinations
            // If the schedule date is in the past (more than 1 day ago)
            if (diffDays < 0 && animalsList.isNotEmpty()) {
                // Check if any vaccination record matches this vaccineName administered within targetDate ± 7 days
                val dateStart = Calendar.getInstance().apply {
                    time = targetDate
                    add(Calendar.DAY_OF_YEAR, -5)
                }.time
                val dateEnd = Calendar.getInstance().apply {
                    time = targetDate
                    add(Calendar.DAY_OF_YEAR, 10)
                }.time

                // Calculate percentage of animals vaccinated
                val vaccinatedCount = animalsList.count { animal ->
                    vRecords.any { rec ->
                        rec.animalId == animal.id &&
                                rec.vaccineName.contains(schedule.vaccineName.split(" ")[0], ignoreCase = true) &&
                                isDateWithinRange(rec.dateAdministered, dateStart, dateEnd)
                    }
                }

                if (vaccinatedCount < animalsList.size) {
                    val missedCount = animalsList.size - vaccinatedCount
                    alertList.add(
                        FarmAlert(
                            id = "miss_${schedule.dateMonthDay}",
                            title = "Missed Vaccination",
                            message = "${schedule.vaccineName} scheduled on $targetDateStr has pending records ($vaccinatedCount/${animalsList.size} administered). $missedCount animals missed.",
                            type = AlertType.CRITICAL,
                            dateStr = targetDateStr
                        )
                    )
                }
            }

            // 3. Alerts for Pending Deworming
            // If schedule requires deworming and we are on or past the vaccination date
            if (schedule.dewormingRequired && diffDays <= 0 && animalsList.isNotEmpty()) {
                val dateDewormStart = targetDate // Deworming scheduled on or after vaccine date
                val dateDewormEnd = Calendar.getInstance().apply {
                    time = targetDate
                    add(Calendar.DAY_OF_YEAR, 14) // Within 14 days after vaccine
                }.time

                val dewormedCount = animalsList.count { animal ->
                    dRecords.any { rec ->
                        rec.animalId == animal.id &&
                                isDateWithinRange(rec.dateAdministered, dateDewormStart, dateDewormEnd)
                    }
                }

                if (dewormedCount < animalsList.size) {
                    val pendingDeworm = animalsList.size - dewormedCount
                    // Only show alert if it is on or after the scheduled date
                    alertList.add(
                        FarmAlert(
                            id = "deworm_${schedule.dateMonthDay}",
                            title = "Pending Deworming Alert",
                            message = "Deworming activity scheduled for ${schedule.vaccineName} ($targetDateStr) is pending for $pendingDeworm animals.",
                            type = AlertType.WARNING,
                            dateStr = targetDateStr
                        )
                    )
                }
            }
        }

        // 4. Low stock feed alerts
        _feedInventory.value.forEach { feed ->
            if (feed.quantityInStock <= feed.lowStockThreshold) {
                alertList.add(
                    FarmAlert(
                        id = "feed_low_${feed.id}",
                        title = "Low Feed Stock Alert",
                        message = "${feed.feedName} has only ${feed.quantityInStock}${feed.unit} left (Threshold: ${feed.lowStockThreshold}${feed.unit}).",
                        type = AlertType.WARNING,
                        dateStr = curDateStr
                    )
                )
            }
        }

        _alerts.value = alertList

        // Trigger system tray notifications for newly created alerts
        val notifiedIds = getNotifiedAlertIds().toMutableSet()
        var updated = false

        alertList.forEach { alert ->
            if (!notifiedIds.contains(alert.id)) {
                postSystemNotification(alert.id, alert.title, alert.message)
                notifiedIds.add(alert.id)
                updated = true
            }
        }

        if (updated) {
            saveNotifiedAlertIds(notifiedIds)
        }
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
                Log.w("FarmViewModel", "Missing POST_NOTIFICATIONS permission. Skipping notification for alert $id")
                return
            }
        }

        try {
            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                id.hashCode(),
                intent,
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
            Log.i("FarmViewModel", "Successfully posted system notification for alert ID: $id")
        } catch (e: SecurityException) {
            Log.e("FarmViewModel", "SecurityException posting notification: ${e.message}")
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
            val animal = Animal(
                tagNumber = tag,
                type = type,
                breed = breed,
                gender = gender,
                ageCategory = ageCat,
                weight = weight,
                healthStatus = health,
                purchaseDate = pDate,
                purchasePrice = pPrice,
                avatarId = avatar
            )
            dbHelper.insertAnimal(animal)
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
            dbHelper.insertVaccinationRecord(
                VaccinationRecord(animalId = animalId, vaccineName = vaccine, dateAdministered = date, notes = notes)
            )
            loadData()
        }
    }

    fun logDeworming(animalId: Long, drug: String, date: String, notes: String) {
        viewModelScope.launch {
            dbHelper.insertDewormingRecord(
                DewormingRecord(animalId = animalId, drugUsed = drug, dateAdministered = date, notes = notes)
            )
            loadData()
        }
    }

    // ==========================================
    // BREEDING OPERATIONS
    // ==========================================
    fun logBreeding(femaleId: Long, maleId: Long, date: String, status: PregnancyStatus, notes: String) {
        viewModelScope.launch {
            // Estimate delivery date: sheep/goats gestation is approx 150 days (5 months)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val breedDate = sdf.parse(date) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = breedDate
                add(Calendar.DAY_OF_YEAR, 150)
            }
            val expectedDel = sdf.format(cal.time)

            dbHelper.insertBreedingRecord(
                BreedingRecord(
                    femaleId = femaleId,
                    maleId = maleId,
                    breedingDate = date,
                    expectedDeliveryDate = expectedDel,
                    status = status,
                    notes = notes
                )
            )
            loadData()
        }
    }

    fun recordDelivery(record: BreedingRecord, deliveryDate: String, offspringCount: Int, notes: String) {
        viewModelScope.launch {
            // Update breeding record status
            val status = if (animals.value.find { it.id == record.femaleId }?.type == AnimalType.SHEEP) PregnancyStatus.LAMBED else PregnancyStatus.KIDDED
            val updatedRecord = record.copy(
                status = status,
                birthDate = deliveryDate,
                offspringCount = offspringCount,
                notes = notes
            )
            dbHelper.updateBreedingRecord(updatedRecord)

            // Register offspring in Animals table
            val mother = animals.value.find { it.id == record.femaleId }
            if (mother != null) {
                for (i in 1..offspringCount) {
                    val offspringTag = "${if (mother.type == AnimalType.SHEEP) "SH" else "GT"}-B${record.id}-$i"
                    dbHelper.insertAnimal(
                        Animal(
                            tagNumber = offspringTag,
                            type = mother.type,
                            breed = mother.breed,
                            gender = if (i % 2 == 0) Gender.FEMALE else Gender.MALE,
                            ageCategory = AgeCategory.BABY.name.let { AgeCategory.BABY },
                            weight = 3.5f,
                            healthStatus = "Healthy",
                            purchaseDate = deliveryDate,
                            purchasePrice = 0f,
                            avatarId = if (mother.type == AnimalType.SHEEP) 0 else 2
                        )
                    )
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
            dbHelper.insertFeedInventory(
                FeedInventory(feedName = feedName, quantityInStock = qty, unit = unit, lowStockThreshold = lowThreshold)
            )
            loadData()
        }
    }

    fun logFeedConsumption(feedName: String, qty: Float, date: String) {
        viewModelScope.launch {
            dbHelper.insertFeedConsumption(
                FeedConsumption(date = date, feedName = feedName, quantityConsumed = qty)
            )
            loadData()
        }
    }

    fun addFeedSchedule(feedName: String, time: String, qty: Float, group: String) {
        viewModelScope.launch {
            dbHelper.insertFeedSchedule(
                FeedSchedule(feedName = feedName, timeOfDay = time, quantity = qty, targetGroup = group)
            )
            loadData()
        }
    }

    fun purchaseFeedDeal(feedName: String, qty: Float, price: Float) {
        viewModelScope.launch {
            val desc = "Purchased $feedName Bulk Deal"
            dbHelper.insertFinancialRecord(
                FinancialRecord(
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FEED_EXPENSE,
                    amount = price,
                    date = _currentDate.value,
                    description = desc
                )
            )

            val inventory = dbHelper.getAllFeedInventory()
            val currentItem = inventory.find { it.feedName.equals(feedName, ignoreCase = true) }
            if (currentItem != null) {
                val newStock = currentItem.quantityInStock + qty
                dbHelper.updateFeedStock(currentItem.feedName, newStock)
            } else {
                dbHelper.insertFeedInventory(
                    FeedInventory(
                        feedName = feedName,
                        quantityInStock = qty,
                        unit = if (feedName.contains("Block", ignoreCase = true)) "pcs" else "kg",
                        lowStockThreshold = if (feedName.contains("Block", ignoreCase = true)) 2f else 50f
                    )
                )
            }
            loadData()
        }
    }

    // ==========================================
    // FINANCIAL OPERATIONS
    // ==========================================
    fun addFinancialRecord(type: TransactionType, category: TransactionCategory, amount: Float, date: String, desc: String) {
        viewModelScope.launch {
            dbHelper.insertFinancialRecord(
                FinancialRecord(type = type, category = category, amount = amount, date = date, description = desc)
            )
            loadData()
        }
    }
}
