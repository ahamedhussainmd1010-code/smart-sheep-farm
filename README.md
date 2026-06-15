# Smart Sheep Farm 🐑

![Smart Sheep Farm - Real Sheep](real_sheep.png)

Smart Sheep Farm is a premium, high-fidelity native Android application built with **Kotlin** and **Jetpack Compose**. It is designed specifically for sheep and goat farmers to manage their livestock, vaccinations, deworming, breeding, feeding, and financial records with intelligence and ease.

The application is engineered with an **offline-first architecture**, utilizing local SQLite storage to ensure farmers can access and log critical data in remote locations with zero network connectivity.

---

## 📱 Features & Highlights

### 1. 🔔 Dashboard & Compliance Alert Center
* **Dynamic Vaccination Schedule**: Automatically tracks the 9 essential annual livestock vaccinations (Feb, Mar, Apr, May, Jul, Aug, Sep, Oct, Nov).
* **Smart Reminders**: Displays critical warnings (7-day heads-up, 1-day warning, and same-day alerts).
* **Missed Vaccination Alerts**: Automatically flags and logs past scheduled vaccinations where animal compliance records are missing (within a $\pm$7 day window).
* **Deworming Compliance Alerts**: Flags scheduled vaccinations that require follow-up deworming (e.g., February, April, July, September, November events) if deworming compliance is not logged within 14 days post-vaccination.
* **Simulated Date Tool**: A developer panel allowing the simulation of any date (e.g. July 5, October 5) to instantly check dynamic notification triggers and test scheduling logic.

### 2. 🐑 Livestock Registration & Management
* Register animals with Tag ID, type (Sheep vs. Goat), breed, gender, age category (Adult vs. Baby/Lamb), weight, health status, and purchase details.
* Quick filtering options to view lists by category (Adults, Babies, Males, Females, Sheep-only, Goats-only).
* Dynamic search by Tag ID and breed.

### 3. 💉 Comprehensive Health Tracker
* Log vaccination and deworming treatments bound directly to individual livestock.
* **Compliance Rate Charts**: High-fidelity, custom **Canvas-drawn** charts displaying the real-time vaccination compliance rate.
* Detailed historical health log logs.

### 4. 🌾 Breeding & Feeding Operations
* **Breeding Mates Tracker**: Log Dam and Sire details with breeding date. The app automatically calculates the expected delivery date based on a ~150-day gestation period.
* **Automated Birthing Logs**: Record lambing/kidding events. The app **automatically registers the new babies** directly into the livestock database.
* **Feed Inventory Management**: Track Alfalfa, concentrate pellets, and mineral block stocks. Displays warnings and "LOW STOCK" badges.
* **Feed Consumption**: Logs daily feed consumption and automatically updates stock levels.

### 5. 💰 Finance & Cash Flow Ledger
* Track income (wool sales, livestock sales) and expenses (feed, veterinary, labor, utility bills).
* **Interactive Financial Analytics**: Custom **Canvas-drawn bar charts** showing total income vs. total expenses.
* Complete interactive historical ledger.

---

## 🛠️ Architecture & Tech Stack

* **Language**: Kotlin 1.9+
* **UI Framework**: Jetpack Compose (Material Design 3)
* **Local Storage**: Android Native SQLite (`SQLiteOpenHelper`)
* **Theme**: Forest Emerald Farm palette (Emerald Greens, Straw Yellows, Sleek Slate Cards)
* **SDK Version**: Target & Compile SDK 37 (to satisfy the latest `androidx.core` library requirements)

---

## 📂 Project Structure

```
app/src/main/java/com/example/myapplication2sheepfarm/
│
├── Models.kt             # Data models, Enums, and structures
├── DatabaseHelper.kt     # SQLite database open helper and CRUD operations
├── FarmViewModel.kt      # State management, sync simulation, and alert engine
├── MainActivity.kt       # Dynamic 5-tab main UI, dialog forms, and Canvas reports
└── ui/theme/             # Typography, Color palettes, and Material 3 Dark theme
```

---

## 📦 Compilation & Execution

To compile and assemble the debug application package, run the following Gradle wrapper command:

```bash
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
