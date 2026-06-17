package com.example.myapplication2sheepfarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.myapplication2sheepfarm.R
import com.example.myapplication2sheepfarm.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()
        requestNotificationPermission()
        AlarmReceiver.scheduleAlarm(this)

        val viewModel = FarmViewModel(application)

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            MyApplication2Theme(theme = currentTheme) {
                LocalizedApp(viewModel) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            // Managed inside main app
                        }
                    ) { innerPadding ->
                        SmartSheepFarmApp(viewModel, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Smart Sheep Farm Alerts"
            val descriptionText = "Alerts for livestock vaccinations, deworming schedules, and low feed stock."
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("farm_alerts_channel_v3", name, importance).apply {
                description = descriptionText
                // Set default system notification sound explicitly
                val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}

enum class AppTab {
    DASHBOARD, LIVESTOCK, HEALTH, BREEDING_FEED, FINANCES
}

@Composable
fun SmartSheepFarmApp(viewModel: FarmViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val isOnline by viewModel.isOnline.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sync/Online Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOnline) FarmGreen.copy(alpha = 0.2f) else RedAlert.copy(alpha = 0.2f))
                        .border(
                            1.dp,
                            if (isOnline) FarmGreen.copy(alpha = 0.5f) else RedAlert.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleConnection() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) FarmGreenLight else RedAlert)
                    )
                    Text(
                        text = if (isOnline) "ONLINE" else "OFFLINE",
                        color = if (isOnline) FarmGreenLight else RedAlert,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Settings Gear Button
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }
        }

        // Auto Sync Message Banner
        AnimatedVisibility(visible = true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isOnline) FarmGreenDark.copy(alpha = 0.3f) else Color.DarkGray.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = syncMessage,
                    color = if (isOnline) FarmGreenLight else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Active Tab Screen Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                AppTab.DASHBOARD -> DashboardTab(viewModel)
                AppTab.LIVESTOCK -> LivestockTab(viewModel)
                AppTab.HEALTH -> HealthTab(viewModel)
                AppTab.BREEDING_FEED -> BreedingFeedTab(viewModel)
                AppTab.FINANCES -> FinancesTab(viewModel)
            }
        }

        // Custom Bottom Navigation Bar
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.height(72.dp)
        ) {
            val navItems = listOf(
                AppTab.DASHBOARD to Pair(R.string.tab_dashboard, "📊"),
                AppTab.LIVESTOCK to Pair(R.string.tab_livestock, "🐑"),
                AppTab.HEALTH to Pair(R.string.tab_health, "💉"),
                AppTab.BREEDING_FEED to Pair(R.string.tab_breeding_feed, "🌾"),
                AppTab.FINANCES to Pair(R.string.tab_finances, "💰")
            )

            navItems.forEach { (tab, details) ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = {
                        Text(details.second, fontSize = 20.sp)
                    },
                    label = {
                        Text(
                            text = stringResource(details.first),
                            fontSize = 10.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) FarmGreenLight else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FarmGreenLight,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = FarmGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        val currentLanguage by viewModel.currentLanguage.collectAsState()
        val currentTheme by viewModel.currentTheme.collectAsState()
        var tempLanguage by remember { mutableStateOf(currentLanguage) }
        var tempTheme by remember { mutableStateOf(currentTheme) }

        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Text(
                        text = stringResource(R.string.select_language),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    // Language options
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AppLanguage.ENGLISH to stringResource(R.string.language_english),
                            AppLanguage.HINDI to stringResource(R.string.language_hindi),
                            AppLanguage.TELUGU to stringResource(R.string.language_telugu)
                        ).forEach { (lang, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (tempLanguage == lang) FarmGreen.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (tempLanguage == lang) FarmGreen else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { tempLanguage = lang }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                if (tempLanguage == lang) {
                                    Text(text = "✓", color = FarmGreenLight, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.theme_title),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    // Theme options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AppTheme.SYSTEM to stringResource(R.string.theme_system),
                            AppTheme.LIGHT to stringResource(R.string.theme_light),
                            AppTheme.DARK to stringResource(R.string.theme_dark)
                        ).forEach { (themeOpt, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (tempTheme == themeOpt) FarmGreen.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (tempTheme == themeOpt) FarmGreen else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { tempTheme = themeOpt }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (tempTheme == themeOpt) FarmGreenLight else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (tempTheme == themeOpt) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSettingsDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.setLanguage(tempLanguage)
                                viewModel.setTheme(tempTheme)
                                showSettingsDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(stringResource(R.string.apply))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD TAB SCREEN
// ==========================================
@Composable
fun DashboardTab(viewModel: FarmViewModel) {
    val alerts by viewModel.alerts.collectAsState()
    val animals by viewModel.animals.collectAsState()
    val finances by viewModel.financialRecords.collectAsState()
    val simulatedDate by viewModel.currentDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var inputDate by remember { mutableStateOf(simulatedDate) }

    // Aggregate statistics
    val totalSheep = animals.count { it.type == AnimalType.SHEEP }
    val totalGoats = animals.count { it.type == AnimalType.GOAT }
    val adultSheep = animals.count { it.type == AnimalType.SHEEP && it.ageCategory == AgeCategory.ADULT }
    val babySheep = animals.count { it.type == AnimalType.SHEEP && it.ageCategory == AgeCategory.BABY }
    val adultGoats = animals.count { it.type == AnimalType.GOAT && it.ageCategory == AgeCategory.ADULT }
    val babyGoats = animals.count { it.type == AnimalType.GOAT && it.ageCategory == AgeCategory.BABY }

    val totalIncome = finances.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.toDouble() }.toFloat()
    val totalExpense = finances.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.toDouble() }.toFloat()
    val netFinances = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time & Simulated Controls Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = stringResource(R.string.simulated_date), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = simulatedDate,
                            color = FarmAccent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.simulated_date_desc),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f).copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                    ) {
                        Text(text = stringResource(R.string.change_date), fontSize = 12.sp)
                    }
                }
            }
        }

        // Prominent Alert Center (Central Feature)
        item {
            Text(
                text = "🔔 " + stringResource(R.string.active_alerts) + " (${alerts.size})",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_active_alerts),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                val cardColor = when (alert.type) {
                    AlertType.CRITICAL -> RedAlert.copy(alpha = 0.15f)
                    AlertType.WARNING -> OrangeAlert.copy(alpha = 0.15f)
                    AlertType.INFO -> BlueInfo.copy(alpha = 0.15f)
                }
                val borderColor = when (alert.type) {
                    AlertType.CRITICAL -> RedAlert
                    AlertType.WARNING -> OrangeAlert
                    AlertType.INFO -> BlueInfo
                }
                val prefix = when (alert.type) {
                    AlertType.CRITICAL -> "⚠️ [CRITICAL] "
                    AlertType.WARNING -> "🔸 [WARNING] "
                    AlertType.INFO -> "ℹ️ [INFO] "
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = when (alert.type) {
                                AlertType.CRITICAL -> "🚨"
                                AlertType.WARNING -> "📢"
                                AlertType.INFO -> "📅"
                            },
                            fontSize = 24.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prefix + alert.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.message,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Animal Headcounts
        item {
            Text(
                text = "📊 " + stringResource(R.string.livestock_metrics),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sheep card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.sheep_label) + " (🐑)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = totalSheep.toString(),
                                color = FarmGreenLight,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.adults_label) + ": $adultSheep", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(text = stringResource(R.string.lambs_label) + ": $babySheep", color = FarmAccent, fontSize = 11.sp)
                        }
                    }
                }

                // Goats card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.goats_label) + " (🐐)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = totalGoats.toString(),
                                color = FarmGreenLight,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.adults_label) + ": $adultGoats", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(text = stringResource(R.string.kids_label) + ": $babyGoats", color = FarmAccent, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Financial Net Stats
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = stringResource(R.string.ledger_net_balance), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = if (netFinances >= 0) "+$${String.format("%.2f", netFinances)}" else "-$${String.format("%.2f", kotlin.math.abs(netFinances))}",
                            color = if (netFinances >= 0) FarmGreenLight else RedAlert,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text("💰", fontSize = 28.sp)
                }
            }
        }

        // Annual Schedule Reference Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.annual_schedule_ref),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    viewModel.annualSchedules.forEach { sch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(sch.dateMonthDay.replace("-", "/"), color = FarmAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(sch.vaccineName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                            if (sch.dewormingRequired) {
                                Text("+ Deworming 💊", color = FarmGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Change date dialog
    if (showDatePicker) {
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.set_simulated_date),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    OutlinedTextField(
                        value = inputDate,
                        onValueChange = { inputDate = it },
                        label = { Text("Date (YYYY-MM-DD)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.setSimulatedDate(inputDate)
                                showDatePicker = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.apply))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. LIVESTOCK TAB SCREEN
// ==========================================
@Composable
fun LivestockTab(viewModel: FarmViewModel) {
    val animals by viewModel.animals.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, SHEEP, GOAT, ADULT, BABY, MALE, FEMALE
    var showAddAnimalDialog by remember { mutableStateOf(false) }

    // Filtered Animals
    val filteredAnimals = animals.filter {
        val matchesSearch = it.tagNumber.contains(searchQuery, ignoreCase = true) || it.breed.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterType) {
            "SHEEP" -> it.type == AnimalType.SHEEP
            "GOAT" -> it.type == AnimalType.GOAT
            "ADULT" -> it.ageCategory == AgeCategory.ADULT
            "BABY" -> it.ageCategory == AgeCategory.BABY
            "MALE" -> it.gender == Gender.MALE
            "FEMALE" -> it.gender == Gender.FEMALE
            else -> true
        }
        matchesSearch && matchesFilter
    }

    // Input States for adding animal
    var tagInput by remember { mutableStateOf("") }
    var typeInput by remember { mutableStateOf(AnimalType.SHEEP) }
    var breedInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf(Gender.FEMALE) }
    var ageCatInput by remember { mutableStateOf(AgeCategory.ADULT) }
    var weightInput by remember { mutableStateOf("45.0") }
    var healthInput by remember { mutableStateOf("Healthy") }
    var pPriceInput by remember { mutableStateOf("150") }
    var pDateInput by remember { mutableStateOf("2026-06-16") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Add Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FarmGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = { showAddAnimalDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreen),
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.btn_register), fontSize = 12.sp)
            }
        }

        // Horizontal filter bar
        val filters = listOf(
            "ALL" to R.string.filter_all,
            "SHEEP" to R.string.filter_sheep,
            "GOAT" to R.string.filter_goats,
            "ADULT" to R.string.filter_adults,
            "BABY" to R.string.filter_babies,
            "MALE" to R.string.filter_males,
            "FEMALE" to R.string.filter_females
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { (key, resId) ->
                val isSelected = filterType == key
                FilterChip(
                    selected = isSelected,
                    onClick = { filterType = key },
                    label = { Text(text = stringResource(resId), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmGreen,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isSelected) FarmGreenLight else MaterialTheme.colorScheme.outline)
                )
            }
        }

        // Animals List
        if (filteredAnimals.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.no_livestock_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAnimals) { animal ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar Icon Box
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FarmGreenDark.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (animal.type) {
                                        AnimalType.SHEEP -> if (animal.ageCategory == AgeCategory.ADULT) "🐑" else "🐏"
                                        AnimalType.GOAT -> "🐐"
                                    },
                                    fontSize = 24.sp
                                )
                            }

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = animal.tagNumber,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (animal.gender == Gender.MALE) "♂" else "♀",
                                        color = if (animal.gender == Gender.MALE) BlueInfo else RedAlert,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "${animal.breed} | ${animal.ageCategory} | ${animal.weight}kg",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (animal.healthStatus.contains("Healthy", ignoreCase = true))
                                                    FarmGreen.copy(alpha = 0.2f)
                                                else OrangeAlert.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = animal.healthStatus,
                                            color = if (animal.healthStatus.contains("Healthy", ignoreCase = true))
                                                FarmGreenLight
                                            else OrangeAlert,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (animal.purchasePrice > 0f) {
                                        Text(
                                            text = stringResource(R.string.purchased_for_format, animal.purchasePrice.toString()),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { viewModel.removeAnimal(animal.id) }) {
                                Text("❌", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add animal dialog
    if (showAddAnimalDialog) {
        Dialog(onDismissRequest = { showAddAnimalDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.register_new_livestock),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text(text = stringResource(R.string.tag_number_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    // Type Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.type_label), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(60.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { typeInput = AnimalType.SHEEP },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (typeInput == AnimalType.SHEEP) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (typeInput == AnimalType.SHEEP) Color.White else MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.filter_sheep))
                            }
                            Button(
                                onClick = { typeInput = AnimalType.GOAT },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (typeInput == AnimalType.GOAT) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (typeInput == AnimalType.GOAT) Color.White else MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.filter_goats))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = breedInput,
                        onValueChange = { breedInput = it },
                        label = { Text(text = stringResource(R.string.breed_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    // Gender selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.gender_label), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(60.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { genderInput = Gender.FEMALE },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (genderInput == Gender.FEMALE) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (genderInput == Gender.FEMALE) Color.White else MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.female_label))
                            }
                            Button(
                                onClick = { genderInput = Gender.MALE },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (genderInput == Gender.MALE) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (genderInput == Gender.MALE) Color.White else MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.male_label))
                            }
                        }
                    }

                    // Age category selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.age_label), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(60.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { ageCatInput = AgeCategory.ADULT },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ageCatInput == AgeCategory.ADULT) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (ageCatInput == AgeCategory.ADULT) Color.White else MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.adult_label))
                            }
                            Button(
                                onClick = { ageCatInput = AgeCategory.BABY },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ageCatInput == AgeCategory.BABY) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (ageCatInput == AgeCategory.BABY) Color.White else MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.baby_label))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text(text = stringResource(R.string.weight_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = healthInput,
                        onValueChange = { healthInput = it },
                        label = { Text(text = stringResource(R.string.health_status_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pPriceInput,
                        onValueChange = { pPriceInput = it },
                        label = { Text(text = stringResource(R.string.purchase_price_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pDateInput,
                        onValueChange = { pDateInput = it },
                        label = { Text(text = stringResource(R.string.purchase_date_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddAnimalDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                if (tagInput.isNotBlank()) {
                                    val wVal = weightInput.toFloatOrNull() ?: 40f
                                    val pVal = pPriceInput.toFloatOrNull() ?: 0f
                                    val avatar = if (typeInput == AnimalType.SHEEP) 0 else 2
                                    viewModel.addAnimal(
                                        tag = tagInput,
                                        type = typeInput,
                                        breed = breedInput.ifBlank { "Unspecified" },
                                        gender = genderInput,
                                        ageCat = ageCatInput,
                                        weight = wVal,
                                        health = healthInput,
                                        pDate = pDateInput,
                                        pPrice = pVal,
                                        avatar = avatar
                                    )
                                    // Reset fields
                                    tagInput = ""
                                    breedInput = ""
                                    showAddAnimalDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.btn_register_action))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. HEALTH TAB SCREEN (Logs & Compliance)
// ==========================================
@Composable
fun HealthTab(viewModel: FarmViewModel) {
    val animals by viewModel.animals.collectAsState()
    val vRecords by viewModel.vaccinationRecords.collectAsState()
    val dRecords by viewModel.dewormingRecords.collectAsState()
    val simulatedDate by viewModel.currentDate.collectAsState()

    var showVaccineDialog by remember { mutableStateOf(false) }
    var showDewormDialog by remember { mutableStateOf(false) }

    // Forms Inputs
    var targetAnimalId by remember { mutableStateOf(-1L) }
    var vaccineNameInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var dewormDrugInput by remember { mutableStateOf("") }

    // Dropdown selects
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Compliance stats
    val totalAnimals = animals.size
    val totalSchDates = viewModel.annualSchedules.size
    val totalRequired = totalAnimals * totalSchDates
    val totalAdministered = vRecords.size
    val compliancePct = if (totalRequired > 0) (totalAdministered.toFloat() / totalRequired.toFloat() * 100f).coerceAtMost(100f) else 100f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (animals.isNotEmpty()) {
                            targetAnimalId = animals[0].id
                            showVaccineDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.btn_log_vaccine), fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (animals.isNotEmpty()) {
                            targetAnimalId = animals[0].id
                            showDewormDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.btn_log_deworm), fontSize = 13.sp)
                }
            }
        }

        // Compliance Report Ring
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val outlineColor = MaterialTheme.colorScheme.outline
                    // Custom Canvas Compliance ring
                    Canvas(modifier = Modifier.size(70.dp)) {
                        // Background circle
                        drawCircle(
                            color = outlineColor,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        // Arc
                        drawArc(
                            color = FarmGreenLight,
                            startAngle = -90f,
                            sweepAngle = 360f * (compliancePct / 100f),
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }

                    Column {
                        Text(text = stringResource(R.string.vaccine_compliance), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = stringResource(R.string.compliance_rate_format, "${String.format("%.1f", compliancePct)}%"),
                            color = FarmGreenLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = stringResource(R.string.health_records_summary, totalAdministered, totalAnimals),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Health History Log List
        item {
            Text(
                text = "📜 " + stringResource(R.string.routine_health_logs),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (vRecords.isEmpty() && dRecords.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.no_medical_records), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Display combined list of health records
        val combinedLogs = (vRecords.map { Triple(it.dateAdministered, Pair("VACCINE", it.vaccineName), it.animalId) } +
                dRecords.map { Triple(it.dateAdministered, Pair("DEWORM", it.drugUsed), it.animalId) })
            .sortedByDescending { it.first }

        items(combinedLogs) { log ->
            val animalTag = animals.find { it.id == log.third }?.tagNumber ?: "Unknown (#${log.third})"
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val textStr = when (log.second.first) {
                            "VACCINE" -> stringResource(R.string.vaccinated_format, log.second.second)
                            "DEWORM" -> stringResource(R.string.dewormed_format, log.second.second)
                            else -> ""
                        }
                        Text(textStr, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = stringResource(R.string.target_livestock_format, animalTag), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Text(log.first, color = FarmAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Log Vaccine Dialog
    if (showVaccineDialog && animals.isNotEmpty()) {
        Dialog(onDismissRequest = { showVaccineDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = stringResource(R.string.title_log_vaccination), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    // Animal Selector Dropdown Trigger
                    var targetAnimalTag = animals.find { it.id == targetAnimalId }?.tagNumber ?: "Select Animal"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.animal_format, targetAnimalTag))
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            animals.forEach { animal ->
                                DropdownMenuItem(
                                    text = { Text(animal.tagNumber, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        targetAnimalId = animal.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = vaccineNameInput,
                        onValueChange = { vaccineNameInput = it },
                        label = { Text(text = stringResource(R.string.vaccine_name_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(text = stringResource(R.string.notes_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showVaccineDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                if (vaccineNameInput.isNotBlank()) {
                                    viewModel.logVaccination(targetAnimalId, vaccineNameInput, simulatedDate, notesInput)
                                    vaccineNameInput = ""
                                    notesInput = ""
                                    showVaccineDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.btn_log_admin))
                        }
                    }
                }
            }
        }
    }

    // Log Deworm Dialog
    if (showDewormDialog && animals.isNotEmpty()) {
        Dialog(onDismissRequest = { showDewormDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = stringResource(R.string.title_log_deworming), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    // Animal Selector
                    var targetAnimalTag = animals.find { it.id == targetAnimalId }?.tagNumber ?: "Select Animal"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.animal_format, targetAnimalTag))
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            animals.forEach { animal ->
                                DropdownMenuItem(
                                    text = { Text(animal.tagNumber, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        targetAnimalId = animal.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dewormDrugInput,
                        onValueChange = { dewormDrugInput = it },
                        label = { Text(text = stringResource(R.string.deworm_drug_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(text = stringResource(R.string.deworm_notes_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDewormDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                if (dewormDrugInput.isNotBlank()) {
                                    viewModel.logDeworming(targetAnimalId, dewormDrugInput, simulatedDate, notesInput)
                                    dewormDrugInput = ""
                                    notesInput = ""
                                    showDewormDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.btn_log_admin))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. BREEDING & FEEDING TAB SCREEN
// ==========================================
@Composable
fun BreedingFeedTab(viewModel: FarmViewModel) {
    val animals by viewModel.animals.collectAsState()
    val breedingList by viewModel.breedingRecords.collectAsState()
    val feedList by viewModel.feedInventory.collectAsState()
    val consumptionLogs by viewModel.feedConsumption.collectAsState()
    val simulatedDate by viewModel.currentDate.collectAsState()

    var showBreedingDialog by remember { mutableStateOf(false) }
    var showLambingDialog by remember { mutableStateOf(false) }
    var showFeedDialog by remember { mutableStateOf(false) }

    var selectedBreedingRecord by remember { mutableStateOf<BreedingRecord?>(null) }

    // Forms Inputs
    var femaleIdInput by remember { mutableStateOf(-1L) }
    var maleIdInput by remember { mutableStateOf(-1L) }
    var breedingNotes by remember { mutableStateOf("") }

    var deliveryDateInput by remember { mutableStateOf(simulatedDate) }
    var offspringCountInput by remember { mutableStateOf("1") }
    var deliveryNotes by remember { mutableStateOf("Born healthy") }

    var feedNameSelect by remember { mutableStateOf("") }
    var feedQtyInput by remember { mutableStateOf("10.0") }

    var femaleDropdownExp by remember { mutableStateOf(false) }
    var maleDropdownExp by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Breeding Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.breeding_schedules_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        val females = animals.filter { it.gender == Gender.FEMALE && it.ageCategory == AgeCategory.ADULT }
                        val males = animals.filter { it.gender == Gender.MALE && it.ageCategory == AgeCategory.ADULT }
                        if (females.isNotEmpty() && males.isNotEmpty()) {
                            femaleIdInput = females[0].id
                            maleIdInput = males[0].id
                            showBreedingDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                ) {
                    Text(text = stringResource(R.string.btn_log_mate), fontSize = 11.sp)
                }
            }
        }

        // Breeding Records
        if (breedingList.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.no_breeding_campaigns), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(breedingList) { record ->
                val femaleTag = animals.find { it.id == record.femaleId }?.tagNumber ?: "Female #${record.femaleId}"
                val maleTag = animals.find { it.id == record.maleId }?.tagNumber ?: "Male #${record.maleId}"

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.breeding_pairing_format, femaleTag, maleTag), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (record.status) {
                                            PregnancyStatus.PREGNANT -> OrangeAlert.copy(alpha = 0.2f)
                                            PregnancyStatus.LAMBED, PregnancyStatus.KIDDED -> FarmGreen.copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = record.status.name,
                                    color = when (record.status) {
                                        PregnancyStatus.PREGNANT -> OrangeAlert
                                        PregnancyStatus.LAMBED, PregnancyStatus.KIDDED -> FarmGreenLight
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = stringResource(R.string.breeding_date_format, record.breedingDate), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(text = stringResource(R.string.expected_delivery_format, record.expectedDeliveryDate), color = FarmAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        if (record.status == PregnancyStatus.PREGNANT) {
                            Button(
                                onClick = {
                                    selectedBreedingRecord = record
                                    showLambingDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(text = stringResource(R.string.btn_log_delivery), fontSize = 12.sp)
                            }
                        } else if (record.birthDate != null) {
                            Text(
                                text = stringResource(R.string.delivery_summary_format, record.birthDate.toString(), record.offspringCount),
                                color = FarmGreenLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Feeding Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.feed_inventory_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        if (feedList.isNotEmpty()) {
                            feedNameSelect = feedList[0].feedName
                            showFeedDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                ) {
                    Text(text = stringResource(R.string.btn_log_consumption), fontSize = 11.sp)
                }
            }
        }

        // Feed Photo Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val resId = context.resources.getIdentifier("sheep_food", "drawable", context.packageName)
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = resId),
                            contentDescription = "Sheep Feed Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🌾 High-Quality Alfalfa, Pellets & Minerals",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Feed stocks
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = stringResource(R.string.stock_levels_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    feedList.forEach { feed ->
                        val isLow = feed.quantityInStock <= feed.lowStockThreshold
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(feed.feedName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${feed.quantityInStock} ${feed.unit}",
                                    color = if (isLow) RedAlert else FarmGreenLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isLow) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RedAlert.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = stringResource(R.string.low_stock_badge), color = RedAlert, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bulk Deals Section Header
        item {
            Text(
                text = "🏷️ " + stringResource(R.string.bulk_deals_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Active Bulk Deals Catalog
        val deals = listOf(
            Triple("Alfalfa Hay", Pair(500f, 150f), Triple("alfalfa_deal", "Premium Alfalfa Hay Bulk Deal", "500kg Bulk Pack • Save 25%")),
            Triple("Concentrate Pellets", Pair(200f, 80f), Triple("pellets_deal", "Nutritional Pellets Combo Pack", "200kg Combo Pack • Save 15%")),
            Triple("Mineral Blocks", Pair(10f, 40f), Triple("salt_blocks_deal", "Himalayan Mineral Salt Block", "10 units Pack • Save 20%"))
        )

        items(deals) { (feedName, specs, details) ->
            val (qty, price) = specs
            val (drawableName, title, desc) = details

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = resId),
                            contentDescription = title,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = desc, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$${String.format(java.util.Locale.US, "%.2f", price)}", color = FarmGreenLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.purchaseFeedDeal(feedName, qty, price)
                            android.widget.Toast.makeText(context, "Purchased $title!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Buy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Feed consumption log heading
        item {
            Text(text = stringResource(R.string.consumption_logs_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        }

        if (consumptionLogs.isEmpty()) {
            item {
                Text(text = stringResource(R.string.no_consumption_logs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        } else {
            items(consumptionLogs.take(3)) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.feed_consumed_format, log.quantityConsumed, log.feedName), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        Text(log.date, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Log Breeding Campaign Dialog
    if (showBreedingDialog) {
        val females = animals.filter { it.gender == Gender.FEMALE && it.ageCategory == AgeCategory.ADULT }
        val males = animals.filter { it.gender == Gender.MALE && it.ageCategory == AgeCategory.ADULT }

        Dialog(onDismissRequest = { showBreedingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = stringResource(R.string.title_log_mate), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    // Dam Selector
                    var damTag = females.find { it.id == femaleIdInput }?.tagNumber ?: "Select Dam"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { femaleDropdownExp = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.dam_format, damTag))
                        }
                        DropdownMenu(
                            expanded = femaleDropdownExp,
                            onDismissRequest = { femaleDropdownExp = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            females.forEach { animal ->
                                DropdownMenuItem(
                                    text = { Text(animal.tagNumber, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        femaleIdInput = animal.id
                                        femaleDropdownExp = false
                                    }
                                )
                            }
                        }
                    }

                    // Sire Selector
                    var sireTag = males.find { it.id == maleIdInput }?.tagNumber ?: "Select Sire"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { maleDropdownExp = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.sire_format, sireTag))
                        }
                        DropdownMenu(
                            expanded = maleDropdownExp,
                            onDismissRequest = { maleDropdownExp = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            males.forEach { animal ->
                                DropdownMenuItem(
                                    text = { Text(animal.tagNumber, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        maleIdInput = animal.id
                                        maleDropdownExp = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = breedingNotes,
                        onValueChange = { breedingNotes = it },
                        label = { Text(text = stringResource(R.string.breeding_notes_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBreedingDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                viewModel.logBreeding(femaleIdInput, maleIdInput, simulatedDate, PregnancyStatus.PREGNANT, breedingNotes)
                                breedingNotes = ""
                                showBreedingDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.btn_record_mate))
                        }
                    }
                }
            }
        }
    }

    // Log Delivery Dialog
    if (showLambingDialog && selectedBreedingRecord != null) {
        Dialog(onDismissRequest = { showLambingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = stringResource(R.string.title_record_birth), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = deliveryDateInput,
                        onValueChange = { deliveryDateInput = it },
                        label = { Text(text = stringResource(R.string.delivery_date_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = offspringCountInput,
                        onValueChange = { offspringCountInput = it },
                        label = { Text(text = stringResource(R.string.offspring_count_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deliveryNotes,
                        onValueChange = { deliveryNotes = it },
                        label = { Text(text = stringResource(R.string.delivery_notes_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLambingDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val count = offspringCountInput.toIntOrNull() ?: 1
                                viewModel.recordDelivery(selectedBreedingRecord!!, deliveryDateInput, count, deliveryNotes)
                                offspringCountInput = "1"
                                showLambingDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.btn_record_birth))
                        }
                    }
                }
            }
        }
    }

    // Log Feed Consumption Dialog
    if (showFeedDialog) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { showFeedDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = stringResource(R.string.title_feed_consumption), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    // Feed Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.feed_select_format, feedNameSelect))
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            feedList.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.feedName, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        feedNameSelect = f.feedName
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedQtyInput,
                        onValueChange = { feedQtyInput = it },
                        label = { Text(text = stringResource(R.string.quantity_consumed_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFeedDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val qty = feedQtyInput.toFloatOrNull() ?: 1f
                                viewModel.logFeedConsumption(feedNameSelect, qty, simulatedDate)
                                showFeedDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.btn_deduct_log))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. FINANCES & REPORTS TAB SCREEN
// ==========================================
@Composable
fun FinancesTab(viewModel: FarmViewModel) {
    val finances by viewModel.financialRecords.collectAsState()
    val simulatedDate by viewModel.currentDate.collectAsState()

    var showTransactionDialog by remember { mutableStateOf(false) }

    // Forms Inputs
    var typeSelect by remember { mutableStateOf(TransactionType.EXPENSE) }
    var categorySelect by remember { mutableStateOf(TransactionCategory.FEED_EXPENSE) }
    var amountInput by remember { mutableStateOf("100.0") }
    var descInput by remember { mutableStateOf("Weekly grain bags") }

    var categoryDropdownExp by remember { mutableStateOf(false) }

    val totalIncome = finances.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.toDouble() }.toFloat()
    val totalExpense = finances.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.toDouble() }.toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Finance Header and buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.finances_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showTransactionDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                ) {
                    Text(text = stringResource(R.string.btn_add_transaction), fontSize = 11.sp)
                }
            }
        }

        // Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenDark.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, FarmGreen)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.total_income_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(
                            "$${String.format("%.2f", totalIncome)}",
                            color = FarmGreenLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expenses card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, RedAlert)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.total_expenses_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(
                            "$${String.format("%.2f", totalExpense)}",
                            color = RedAlert,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Analytics Graphic: Custom Canvas Bar Graph
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.analytics_chart_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val baselineColor = MaterialTheme.colorScheme.outline
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val maxVal = maxOf(totalIncome, totalExpense, 100f)
                        val w = size.width
                        val h = size.height

                        // Draw baseline
                        drawLine(
                            color = baselineColor,
                            start = Offset(0f, h - 20f),
                            end = Offset(w, h - 20f),
                            strokeWidth = 2f
                        )

                        // Income Bar
                        val incomeHeight = (totalIncome / maxVal) * (h - 40f)
                        drawRect(
                            brush = Brush.verticalGradient(listOf(FarmGreenLight, FarmGreen)),
                            topLeft = Offset(w * 0.18f, h - 20f - incomeHeight),
                            size = Size(w * 0.25f, incomeHeight)
                        )

                        // Expense Bar
                        val expenseHeight = (totalExpense / maxVal) * (h - 40f)
                        drawRect(
                            brush = Brush.verticalGradient(listOf(RedAlert, Color(0xFF991B1B))),
                            topLeft = Offset(w * 0.57f, h - 20f - expenseHeight),
                            size = Size(w * 0.25f, expenseHeight)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(text = stringResource(R.string.income_green_label), color = FarmGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.expenses_red_label), color = RedAlert, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Financial Ledger List
        item {
            Text(text = stringResource(R.string.ledger_history_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (finances.isEmpty()) {
            item {
                Text(text = stringResource(R.string.no_ledger_logs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        items(finances) { trans ->
            val isIncome = trans.type == TransactionType.INCOME
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(trans.description, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${trans.category.name} | ${trans.date}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Text(
                        text = if (isIncome) "+$${trans.amount}" else "-$${trans.amount}",
                        color = if (isIncome) FarmGreenLight else RedAlert,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showTransactionDialog) {
        Dialog(onDismissRequest = { showTransactionDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, FarmGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = stringResource(R.string.title_add_financial_record), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    // Type Segmented Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { typeSelect = TransactionType.EXPENSE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (typeSelect == TransactionType.EXPENSE) RedAlert else MaterialTheme.colorScheme.background, contentColor = if (typeSelect == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.expense_btn_label))
                        }
                        Button(
                            onClick = { typeSelect = TransactionType.INCOME },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (typeSelect == TransactionType.INCOME) FarmGreen else MaterialTheme.colorScheme.background, contentColor = if (typeSelect == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.income_btn_label))
                        }
                    }

                    // Category Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { categoryDropdownExp = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.category_select_format, categorySelect.name))
                        }
                        DropdownMenu(
                            expanded = categoryDropdownExp,
                            onDismissRequest = { categoryDropdownExp = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            TransactionCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        categorySelect = cat
                                        categoryDropdownExp = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text(text = stringResource(R.string.amount_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text(text = stringResource(R.string.description_label), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTransactionDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(text = stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val amt = amountInput.toFloatOrNull() ?: 0f
                                viewModel.addFinancialRecord(typeSelect, categorySelect, amt, simulatedDate, descInput)
                                descInput = ""
                                showTransactionDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreen)
                        ) {
                            Text(text = stringResource(R.string.log_ledger_label))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalizedApp(viewModel: FarmViewModel, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val localizedContext = remember(currentLanguage) {
        val locale = when (currentLanguage) {
            AppLanguage.HINDI -> java.util.Locale.forLanguageTag("hi")
            AppLanguage.TELUGU -> java.util.Locale.forLanguageTag("te")
            AppLanguage.ENGLISH -> java.util.Locale.forLanguageTag("en")
        }
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localizedContext) {
        content()
    }
}

