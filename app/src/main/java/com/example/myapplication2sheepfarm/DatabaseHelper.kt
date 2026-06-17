package com.example.myapplication2sheepfarm

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "smart_sheep_farm.db"
        private const val DATABASE_VERSION = 3

        // Table Names
        private const val TABLE_ANIMALS = "animals"
        private const val TABLE_VACCINATIONS = "vaccinations"
        private const val TABLE_DEWORMINGS = "dewormings"
        private const val TABLE_BREEDINGS = "breedings"
        private const val TABLE_FEED_INVENTORY = "feed_inventory"
        private const val TABLE_FEED_SCHEDULES = "feed_schedules"
        private const val TABLE_FEED_CONSUMPTION = "feed_consumption"
        private const val TABLE_FINANCES = "finances"
        private const val TABLE_USERS = "users"

        // Common Column
        private const val KEY_ID = "id"

        // Users Columns
        private const val KEY_USER_NAME = "username"
        private const val KEY_USER_PASSWORD_HASH = "password_hash"
        private const val KEY_USER_EMAIL = "email"
        private const val KEY_USER_PHONE = "phone"



        // Animals Columns
        private const val KEY_ANIMAL_TAG = "tag_number"
        private const val KEY_ANIMAL_TYPE = "type" // SHEEP, GOAT
        private const val KEY_ANIMAL_BREED = "breed"
        private const val KEY_ANIMAL_GENDER = "gender" // MALE, FEMALE
        private const val KEY_ANIMAL_AGE_CAT = "age_category" // ADULT, BABY
        private const val KEY_ANIMAL_WEIGHT = "weight"
        private const val KEY_ANIMAL_HEALTH = "health_status"
        private const val KEY_ANIMAL_PURCHASE_DATE = "purchase_date"
        private const val KEY_ANIMAL_PURCHASE_PRICE = "purchase_price"
        private const val KEY_ANIMAL_AVATAR = "avatar_id"

        // Vaccination/Deworming Record Columns
        private const val KEY_RECORD_ANIMAL_ID = "animal_id"
        private const val KEY_VACCINE_NAME = "vaccine_name"
        private const val KEY_DATE_ADMINISTERED = "date_administered"
        private const val KEY_NOTES = "notes"
        private const val KEY_DRUG_USED = "drug_used"

        // Breeding Columns
        private const val KEY_BREEDING_FEMALE_ID = "female_id"
        private const val KEY_BREEDING_MALE_ID = "male_id"
        private const val KEY_BREEDING_DATE = "breeding_date"
        private const val KEY_BREEDING_EXPECTED_DELIVERY = "expected_delivery_date"
        private const val KEY_BREEDING_STATUS = "status" // BREEDING, PREGNANT, LAMBED, KIDDED, FAILED
        private const val KEY_BREEDING_BIRTH_DATE = "birth_date"
        private const val KEY_BREEDING_OFFSPRING_COUNT = "offspring_count"

        // Feed Inventory Columns
        private const val KEY_FEED_NAME = "feed_name"
        private const val KEY_FEED_QTY_IN_STOCK = "qty_in_stock"
        private const val KEY_FEED_UNIT = "unit"
        private const val KEY_FEED_LOW_THRESHOLD = "low_threshold"

        // Feed Schedule Columns
        private const val KEY_FS_FEED_NAME = "feed_name"
        private const val KEY_FS_TIME_OF_DAY = "time_of_day"
        private const val KEY_FS_QTY = "quantity"
        private const val KEY_FS_TARGET_GROUP = "target_group"

        // Feed Consumption Columns
        private const val KEY_FC_DATE = "consumption_date"
        private const val KEY_FC_FEED_NAME = "feed_name"
        private const val KEY_FC_QTY = "qty_consumed"

        // Finances Columns
        private const val KEY_FIN_TYPE = "type" // INCOME, EXPENSE
        private const val KEY_FIN_CATEGORY = "category"
        private const val KEY_FIN_AMOUNT = "amount"
        private const val KEY_FIN_DATE = "date"
        private const val KEY_FIN_DESC = "description"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Animals Table
        val createAnimalsTable = ("CREATE TABLE " + TABLE_ANIMALS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ANIMAL_TAG + " TEXT UNIQUE,"
                + KEY_ANIMAL_TYPE + " TEXT,"
                + KEY_ANIMAL_BREED + " TEXT,"
                + KEY_ANIMAL_GENDER + " TEXT,"
                + KEY_ANIMAL_AGE_CAT + " TEXT,"
                + KEY_ANIMAL_WEIGHT + " REAL,"
                + KEY_ANIMAL_HEALTH + " TEXT,"
                + KEY_ANIMAL_PURCHASE_DATE + " TEXT,"
                + KEY_ANIMAL_PURCHASE_PRICE + " REAL,"
                + KEY_ANIMAL_AVATAR + " INTEGER" + ")")
        db.execSQL(createAnimalsTable)

        // Create Vaccinations Table
        val createVaccinationsTable = ("CREATE TABLE " + TABLE_VACCINATIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RECORD_ANIMAL_ID + " INTEGER,"
                + KEY_VACCINE_NAME + " TEXT,"
                + KEY_DATE_ADMINISTERED + " TEXT,"
                + KEY_NOTES + " TEXT" + ")")
        db.execSQL(createVaccinationsTable)

        // Create Dewormings Table
        val createDewormingsTable = ("CREATE TABLE " + TABLE_DEWORMINGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RECORD_ANIMAL_ID + " INTEGER,"
                + KEY_DRUG_USED + " TEXT,"
                + KEY_DATE_ADMINISTERED + " TEXT,"
                + KEY_NOTES + " TEXT" + ")")
        db.execSQL(createDewormingsTable)

        // Create Breedings Table
        val createBreedingsTable = ("CREATE TABLE " + TABLE_BREEDINGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BREEDING_FEMALE_ID + " INTEGER,"
                + KEY_BREEDING_MALE_ID + " INTEGER,"
                + KEY_BREEDING_DATE + " TEXT,"
                + KEY_BREEDING_EXPECTED_DELIVERY + " TEXT,"
                + KEY_BREEDING_STATUS + " TEXT,"
                + KEY_BREEDING_BIRTH_DATE + " TEXT,"
                + KEY_BREEDING_OFFSPRING_COUNT + " INTEGER,"
                + KEY_NOTES + " TEXT" + ")")
        db.execSQL(createBreedingsTable)

        // Create Feed Inventory Table
        val createFeedInventoryTable = ("CREATE TABLE " + TABLE_FEED_INVENTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FEED_NAME + " TEXT UNIQUE,"
                + KEY_FEED_QTY_IN_STOCK + " REAL,"
                + KEY_FEED_UNIT + " TEXT,"
                + KEY_FEED_LOW_THRESHOLD + " REAL" + ")")
        db.execSQL(createFeedInventoryTable)

        // Create Feed Schedules Table
        val createFeedSchedulesTable = ("CREATE TABLE " + TABLE_FEED_SCHEDULES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FS_FEED_NAME + " TEXT,"
                + KEY_FS_TIME_OF_DAY + " TEXT,"
                + KEY_FS_QTY + " REAL,"
                + KEY_FS_TARGET_GROUP + " TEXT" + ")")
        db.execSQL(createFeedSchedulesTable)

        // Create Feed Consumption Table
        val createFeedConsumptionTable = ("CREATE TABLE " + TABLE_FEED_CONSUMPTION + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FC_DATE + " TEXT,"
                + KEY_FC_FEED_NAME + " TEXT,"
                + KEY_FC_QTY + " REAL" + ")")
        db.execSQL(createFeedConsumptionTable)

        // Create Finances Table
        val createFinancesTable = ("CREATE TABLE " + TABLE_FINANCES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FIN_TYPE + " TEXT,"
                + KEY_FIN_CATEGORY + " TEXT,"
                + KEY_FIN_AMOUNT + " REAL,"
                + KEY_FIN_DATE + " TEXT,"
                + KEY_FIN_DESC + " TEXT" + ")")
        db.execSQL(createFinancesTable)

        // Create Users Table
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_NAME + " TEXT UNIQUE,"
                + KEY_USER_PASSWORD_HASH + " TEXT,"
                + KEY_USER_EMAIL + " TEXT,"
                + KEY_USER_PHONE + " TEXT" + ")")
        db.execSQL(createUsersTable)

        // Seed data
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ANIMALS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VACCINATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DEWORMINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BREEDINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FEED_INVENTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FEED_SCHEDULES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FEED_CONSUMPTION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FINANCES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }


    private fun seedInitialData(db: SQLiteDatabase) {
        // 1. Seed Animals
        val animals = listOf(
            ContentValues().apply {
                put(KEY_ANIMAL_TAG, "SH-001")
                put(KEY_ANIMAL_TYPE, AnimalType.SHEEP.name)
                put(KEY_ANIMAL_BREED, "Merino")
                put(KEY_ANIMAL_GENDER, Gender.FEMALE.name)
                put(KEY_ANIMAL_AGE_CAT, AgeCategory.ADULT.name)
                put(KEY_ANIMAL_WEIGHT, 68.5f)
                put(KEY_ANIMAL_HEALTH, "Healthy")
                put(KEY_ANIMAL_PURCHASE_DATE, "2025-03-10")
                put(KEY_ANIMAL_PURCHASE_PRICE, 150f)
                put(KEY_ANIMAL_AVATAR, 0)
            },
            ContentValues().apply {
                put(KEY_ANIMAL_TAG, "SH-002")
                put(KEY_ANIMAL_TYPE, AnimalType.SHEEP.name)
                put(KEY_ANIMAL_BREED, "Dorper")
                put(KEY_ANIMAL_GENDER, Gender.MALE.name)
                put(KEY_ANIMAL_AGE_CAT, AgeCategory.ADULT.name)
                put(KEY_ANIMAL_WEIGHT, 85.0f)
                put(KEY_ANIMAL_HEALTH, "Healthy")
                put(KEY_ANIMAL_PURCHASE_DATE, "2025-05-12")
                put(KEY_ANIMAL_PURCHASE_PRICE, 220f)
                put(KEY_ANIMAL_AVATAR, 1)
            },
            ContentValues().apply {
                put(KEY_ANIMAL_TAG, "SH-003")
                put(KEY_ANIMAL_TYPE, AnimalType.SHEEP.name)
                put(KEY_ANIMAL_BREED, "Merino")
                put(KEY_ANIMAL_GENDER, Gender.FEMALE.name)
                put(KEY_ANIMAL_AGE_CAT, AgeCategory.BABY.name)
                put(KEY_ANIMAL_WEIGHT, 12.4f)
                put(KEY_ANIMAL_HEALTH, "Healthy")
                put(KEY_ANIMAL_PURCHASE_DATE, "2026-04-20")
                put(KEY_ANIMAL_PURCHASE_PRICE, 0f) // Born on farm
                put(KEY_ANIMAL_AVATAR, 0)
            },
            ContentValues().apply {
                put(KEY_ANIMAL_TAG, "GT-001")
                put(KEY_ANIMAL_TYPE, AnimalType.GOAT.name)
                put(KEY_ANIMAL_BREED, "Boer")
                put(KEY_ANIMAL_GENDER, Gender.FEMALE.name)
                put(KEY_ANIMAL_AGE_CAT, AgeCategory.ADULT.name)
                put(KEY_ANIMAL_WEIGHT, 54.2f)
                put(KEY_ANIMAL_HEALTH, "Healthy")
                put(KEY_ANIMAL_PURCHASE_DATE, "2025-06-01")
                put(KEY_ANIMAL_PURCHASE_PRICE, 180f)
                put(KEY_ANIMAL_AVATAR, 2)
            },
            ContentValues().apply {
                put(KEY_ANIMAL_TAG, "GT-002")
                put(KEY_ANIMAL_TYPE, AnimalType.GOAT.name)
                put(KEY_ANIMAL_BREED, "Saaneen")
                put(KEY_ANIMAL_GENDER, Gender.FEMALE.name)
                put(KEY_ANIMAL_AGE_CAT, AgeCategory.ADULT.name)
                put(KEY_ANIMAL_WEIGHT, 48.0f)
                put(KEY_ANIMAL_HEALTH, "Requires Deworming")
                put(KEY_ANIMAL_PURCHASE_DATE, "2025-08-15")
                put(KEY_ANIMAL_PURCHASE_PRICE, 140f)
                put(KEY_ANIMAL_AVATAR, 3)
            },
            ContentValues().apply {
                put(KEY_ANIMAL_TAG, "GT-003")
                put(KEY_ANIMAL_TYPE, AnimalType.GOAT.name)
                put(KEY_ANIMAL_BREED, "Boer")
                put(KEY_ANIMAL_GENDER, Gender.MALE.name)
                put(KEY_ANIMAL_AGE_CAT, AgeCategory.BABY.name)
                put(KEY_ANIMAL_WEIGHT, 8.5f)
                put(KEY_ANIMAL_HEALTH, "Healthy")
                put(KEY_ANIMAL_PURCHASE_DATE, "2026-05-01")
                put(KEY_ANIMAL_PURCHASE_PRICE, 0f) // Born on farm
                put(KEY_ANIMAL_AVATAR, 2)
            }
        )
        animals.forEach { db.insert(TABLE_ANIMALS, null, it) }

        // 2. Seed Feed Inventory
        val feeds = listOf(
            ContentValues().apply {
                put(KEY_FEED_NAME, "Alfalfa Hay")
                put(KEY_FEED_QTY_IN_STOCK, 500f)
                put(KEY_FEED_UNIT, "kg")
                put(KEY_FEED_LOW_THRESHOLD, 100f)
            },
            ContentValues().apply {
                put(KEY_FEED_NAME, "Concentrate Feed pellets")
                put(KEY_FEED_QTY_IN_STOCK, 80f) // Low Stock
                put(KEY_FEED_UNIT, "kg")
                put(KEY_FEED_LOW_THRESHOLD, 150f)
            },
            ContentValues().apply {
                put(KEY_FEED_NAME, "Mineral Block")
                put(KEY_FEED_QTY_IN_STOCK, 5f)
                put(KEY_FEED_UNIT, "blocks")
                put(KEY_FEED_LOW_THRESHOLD, 2f)
            }
        )
        feeds.forEach { db.insert(TABLE_FEED_INVENTORY, null, it) }

        // 3. Seed Feed Schedule
        val feedSchedules = listOf(
            ContentValues().apply {
                put(KEY_FS_FEED_NAME, "Alfalfa Hay")
                put(KEY_FS_TIME_OF_DAY, "07:30 AM")
                put(KEY_FS_QTY, 1.5f)
                put(KEY_FS_TARGET_GROUP, "Ewes & Does Group A")
            },
            ContentValues().apply {
                put(KEY_FS_FEED_NAME, "Concentrate Feed pellets")
                put(KEY_FS_TIME_OF_DAY, "04:30 PM")
                put(KEY_FS_QTY, 0.5f)
                put(KEY_FS_TARGET_GROUP, "Weaned Lambs")
            }
        )
        feedSchedules.forEach { db.insert(TABLE_FEED_SCHEDULES, null, it) }

        // 4. Seed Finances (Expenses & Incomes)
        val finances = listOf(
            ContentValues().apply {
                put(KEY_FIN_TYPE, TransactionType.EXPENSE.name)
                put(KEY_FIN_CATEGORY, TransactionCategory.FEED_EXPENSE.name)
                put(KEY_FIN_AMOUNT, 450f)
                put(KEY_FIN_DATE, "2026-05-10")
                put(KEY_FIN_DESC, "Bulk Alfalfa Purchase")
            },
            ContentValues().apply {
                put(KEY_FIN_TYPE, TransactionType.EXPENSE.name)
                put(KEY_FIN_CATEGORY, TransactionCategory.VET_EXPENSE.name)
                put(KEY_FIN_AMOUNT, 120f)
                put(KEY_FIN_DATE, "2026-05-20")
                put(KEY_FIN_DESC, "Veterinary routine checkup")
            },
            ContentValues().apply {
                put(KEY_FIN_TYPE, TransactionType.INCOME.name)
                put(KEY_FIN_CATEGORY, TransactionCategory.ANIMAL_SALE.name)
                put(KEY_FIN_AMOUNT, 380f)
                put(KEY_FIN_DATE, "2026-06-02")
                put(KEY_FIN_DESC, "Sold 2 Ram Lambs")
            },
            ContentValues().apply {
                put(KEY_FIN_TYPE, TransactionType.INCOME.name)
                put(KEY_FIN_CATEGORY, TransactionCategory.WOOL_SALE.name)
                put(KEY_FIN_AMOUNT, 240f)
                put(KEY_FIN_DATE, "2026-06-12")
                put(KEY_FIN_DESC, "Sheared wool bundle sale")
            },
            ContentValues().apply {
                put(KEY_FIN_TYPE, TransactionType.EXPENSE.name)
                put(KEY_FIN_CATEGORY, TransactionCategory.LABOR_EXPENSE.name)
                put(KEY_FIN_AMOUNT, 300f)
                put(KEY_FIN_DATE, "2026-06-14")
                put(KEY_FIN_DESC, "Weekly helper wages")
            }
        )
        finances.forEach { db.insert(TABLE_FINANCES, null, it) }

        // 5. Seed Breeding Records
        val breedings = listOf(
            ContentValues().apply {
                put(KEY_BREEDING_FEMALE_ID, 1L) // SH-001
                put(KEY_BREEDING_MALE_ID, 2L) // SH-002
                put(KEY_BREEDING_DATE, "2025-11-20")
                put(KEY_BREEDING_EXPECTED_DELIVERY, "2026-04-19")
                put(KEY_BREEDING_STATUS, PregnancyStatus.LAMBED.name)
                put(KEY_BREEDING_BIRTH_DATE, "2026-04-20")
                put(KEY_BREEDING_OFFSPRING_COUNT, 1)
                put(KEY_NOTES, "Healthy female lamb SH-003 born")
            },
            ContentValues().apply {
                put(KEY_BREEDING_FEMALE_ID, 4L) // GT-001 Boer Female
                put(KEY_BREEDING_MALE_ID, 6L) // GT-003 Boer Male (seeded as baby but represents buck)
                put(KEY_BREEDING_DATE, "2026-02-05")
                put(KEY_BREEDING_EXPECTED_DELIVERY, "2026-07-05")
                put(KEY_BREEDING_STATUS, PregnancyStatus.PREGNANT.name)
                putNull(KEY_BREEDING_BIRTH_DATE)
                put(KEY_BREEDING_OFFSPRING_COUNT, 0)
                put(KEY_NOTES, "Confirm pregnancy via ultrasound")
            }
        )
        breedings.forEach { db.insert(TABLE_BREEDINGS, null, it) }

        // 6. Seed some historical vaccinations
        val pastVaccines = listOf(
            ContentValues().apply {
                put(KEY_RECORD_ANIMAL_ID, 1L)
                put(KEY_VACCINE_NAME, "FMD Vaccine")
                put(KEY_DATE_ADMINISTERED, "2026-02-05")
                put(KEY_NOTES, "Routine Feb dose")
            },
            ContentValues().apply {
                put(KEY_RECORD_ANIMAL_ID, 4L)
                put(KEY_VACCINE_NAME, "Sheep & Goat Pox")
                put(KEY_DATE_ADMINISTERED, "2026-03-05")
                put(KEY_NOTES, "Annual pox dose")
            }
        )
        pastVaccines.forEach { db.insert(TABLE_VACCINATIONS, null, it) }

        // 7. Seed historical deworming
        val pastDewormings = listOf(
            ContentValues().apply {
                put(KEY_RECORD_ANIMAL_ID, 1L)
                put(KEY_DRUG_USED, "Albendazole")
                put(KEY_DATE_ADMINISTERED, "2026-02-05")
                put(KEY_NOTES, "Post-FMD vaccination deworming")
            }
        )
        pastDewormings.forEach { db.insert(TABLE_DEWORMINGS, null, it) }

        // 8. Seed default admin user
        val adminUser = ContentValues().apply {
            put(KEY_USER_NAME, "admin")
            put(KEY_USER_PASSWORD_HASH, hashPassword("admin123"))
            put(KEY_USER_EMAIL, "admin@sheepfarm.com")
            put(KEY_USER_PHONE, "+91 98765 43210")
        }
        db.insert(TABLE_USERS, null, adminUser)
    }


    // ==========================================
    // LIVESTOCK CRUD OPERATIONS
    // ==========================================

    fun getAllAnimals(): List<Animal> {
        val list = mutableListOf<Animal>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ANIMALS ORDER BY $KEY_ANIMAL_TAG ASC", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID))
                val tag = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_TAG))
                val type = AnimalType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_TYPE)))
                val breed = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_BREED))
                val gender = Gender.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_GENDER)))
                val ageCat = AgeCategory.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_AGE_CAT)))
                val weight = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_ANIMAL_WEIGHT))
                val health = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_HEALTH))
                val pDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ANIMAL_PURCHASE_DATE))
                val pPrice = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_ANIMAL_PURCHASE_PRICE))
                val avatar = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ANIMAL_AVATAR))

                list.add(Animal(id, tag, type, breed, gender, ageCat, weight, health, pDate, pPrice, avatar))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertAnimal(animal: Animal): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ANIMAL_TAG, animal.tagNumber)
            put(KEY_ANIMAL_TYPE, animal.type.name)
            put(KEY_ANIMAL_BREED, animal.breed)
            put(KEY_ANIMAL_GENDER, animal.gender.name)
            put(KEY_ANIMAL_AGE_CAT, animal.ageCategory.name)
            put(KEY_ANIMAL_WEIGHT, animal.weight)
            put(KEY_ANIMAL_HEALTH, animal.healthStatus)
            put(KEY_ANIMAL_PURCHASE_DATE, animal.purchaseDate)
            put(KEY_ANIMAL_PURCHASE_PRICE, animal.purchasePrice)
            put(KEY_ANIMAL_AVATAR, animal.avatarId)
        }
        return db.insert(TABLE_ANIMALS, null, values)
    }

    fun updateAnimal(animal: Animal): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ANIMAL_TAG, animal.tagNumber)
            put(KEY_ANIMAL_TYPE, animal.type.name)
            put(KEY_ANIMAL_BREED, animal.breed)
            put(KEY_ANIMAL_GENDER, animal.gender.name)
            put(KEY_ANIMAL_AGE_CAT, animal.ageCategory.name)
            put(KEY_ANIMAL_WEIGHT, animal.weight)
            put(KEY_ANIMAL_HEALTH, animal.healthStatus)
            put(KEY_ANIMAL_PURCHASE_DATE, animal.purchaseDate)
            put(KEY_ANIMAL_PURCHASE_PRICE, animal.purchasePrice)
            put(KEY_ANIMAL_AVATAR, animal.avatarId)
        }
        return db.update(TABLE_ANIMALS, values, "$KEY_ID = ?", arrayOf(animal.id.toString()))
    }

    fun deleteAnimal(id: Long): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_ANIMALS, "$KEY_ID = ?", arrayOf(id.toString()))
    }

    // ==========================================
    // HEALTH OPERATIONS (Vaccinations & Deworming)
    // ==========================================

    fun getAllVaccinationRecords(): List<VaccinationRecord> {
        val list = mutableListOf<VaccinationRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_VACCINATIONS ORDER BY $KEY_DATE_ADMINISTERED DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    VaccinationRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        animalId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_RECORD_ANIMAL_ID)),
                        vaccineName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VACCINE_NAME)),
                        dateAdministered = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE_ADMINISTERED)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertVaccinationRecord(record: VaccinationRecord): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_RECORD_ANIMAL_ID, record.animalId)
            put(KEY_VACCINE_NAME, record.vaccineName)
            put(KEY_DATE_ADMINISTERED, record.dateAdministered)
            put(KEY_NOTES, record.notes)
        }
        return db.insert(TABLE_VACCINATIONS, null, values)
    }

    fun getAllDewormingRecords(): List<DewormingRecord> {
        val list = mutableListOf<DewormingRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEWORMINGS ORDER BY $KEY_DATE_ADMINISTERED DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    DewormingRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        animalId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_RECORD_ANIMAL_ID)),
                        drugUsed = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DRUG_USED)),
                        dateAdministered = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE_ADMINISTERED)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertDewormingRecord(record: DewormingRecord): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_RECORD_ANIMAL_ID, record.animalId)
            put(KEY_DRUG_USED, record.drugUsed)
            put(KEY_DATE_ADMINISTERED, record.dateAdministered)
            put(KEY_NOTES, record.notes)
        }
        return db.insert(TABLE_DEWORMINGS, null, values)
    }

    // ==========================================
    // BREEDING OPERATIONS
    // ==========================================

    fun getAllBreedingRecords(): List<BreedingRecord> {
        val list = mutableListOf<BreedingRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BREEDINGS ORDER BY $KEY_BREEDING_DATE DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    BreedingRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        femaleId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BREEDING_FEMALE_ID)),
                        maleId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BREEDING_MALE_ID)),
                        breedingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BREEDING_DATE)),
                        expectedDeliveryDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BREEDING_EXPECTED_DELIVERY)),
                        status = PregnancyStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BREEDING_STATUS))),
                        birthDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BREEDING_BIRTH_DATE)),
                        offspringCount = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BREEDING_OFFSPRING_COUNT)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTES))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertBreedingRecord(record: BreedingRecord): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_BREEDING_FEMALE_ID, record.femaleId)
            put(KEY_BREEDING_MALE_ID, record.maleId)
            put(KEY_BREEDING_DATE, record.breedingDate)
            put(KEY_BREEDING_EXPECTED_DELIVERY, record.expectedDeliveryDate)
            put(KEY_BREEDING_STATUS, record.status.name)
            if (record.birthDate != null) put(KEY_BREEDING_BIRTH_DATE, record.birthDate) else putNull(KEY_BREEDING_BIRTH_DATE)
            put(KEY_BREEDING_OFFSPRING_COUNT, record.offspringCount)
            put(KEY_NOTES, record.notes)
        }
        return db.insert(TABLE_BREEDINGS, null, values)
    }

    fun updateBreedingRecord(record: BreedingRecord): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_BREEDING_FEMALE_ID, record.femaleId)
            put(KEY_BREEDING_MALE_ID, record.maleId)
            put(KEY_BREEDING_DATE, record.breedingDate)
            put(KEY_BREEDING_EXPECTED_DELIVERY, record.expectedDeliveryDate)
            put(KEY_BREEDING_STATUS, record.status.name)
            if (record.birthDate != null) put(KEY_BREEDING_BIRTH_DATE, record.birthDate) else putNull(KEY_BREEDING_BIRTH_DATE)
            put(KEY_BREEDING_OFFSPRING_COUNT, record.offspringCount)
            put(KEY_NOTES, record.notes)
        }
        return db.update(TABLE_BREEDINGS, values, "$KEY_ID = ?", arrayOf(record.id.toString()))
    }

    // ==========================================
    // FEEDING OPERATIONS
    // ==========================================

    fun getAllFeedInventory(): List<FeedInventory> {
        val list = mutableListOf<FeedInventory>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FEED_INVENTORY ORDER BY $KEY_FEED_NAME ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    FeedInventory(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        feedName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FEED_NAME)),
                        quantityInStock = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_FEED_QTY_IN_STOCK)),
                        unit = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FEED_UNIT)),
                        lowStockThreshold = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_FEED_LOW_THRESHOLD))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateFeedStock(feedName: String, newQty: Float): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_FEED_QTY_IN_STOCK, newQty)
        }
        return db.update(TABLE_FEED_INVENTORY, values, "$KEY_FEED_NAME = ?", arrayOf(feedName))
    }

    fun insertFeedInventory(feed: FeedInventory): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_FEED_NAME, feed.feedName)
            put(KEY_FEED_QTY_IN_STOCK, feed.quantityInStock)
            put(KEY_FEED_UNIT, feed.unit)
            put(KEY_FEED_LOW_THRESHOLD, feed.lowStockThreshold)
        }
        return db.insert(TABLE_FEED_INVENTORY, null, values)
    }

    fun getAllFeedSchedules(): List<FeedSchedule> {
        val list = mutableListOf<FeedSchedule>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FEED_SCHEDULES", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    FeedSchedule(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        feedName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FS_FEED_NAME)),
                        timeOfDay = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FS_TIME_OF_DAY)),
                        quantity = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_FS_QTY)),
                        targetGroup = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FS_TARGET_GROUP))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertFeedSchedule(schedule: FeedSchedule): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_FS_FEED_NAME, schedule.feedName)
            put(KEY_FS_TIME_OF_DAY, schedule.timeOfDay)
            put(KEY_FS_QTY, schedule.quantity)
            put(KEY_FS_TARGET_GROUP, schedule.targetGroup)
        }
        return db.insert(TABLE_FEED_SCHEDULES, null, values)
    }

    fun getAllFeedConsumption(): List<FeedConsumption> {
        val list = mutableListOf<FeedConsumption>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FEED_CONSUMPTION ORDER BY $KEY_FC_DATE DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    FeedConsumption(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FC_DATE)),
                        feedName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FC_FEED_NAME)),
                        quantityConsumed = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_FC_QTY))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertFeedConsumption(consumption: FeedConsumption): Long {
        val db = this.writableDatabase
        // Deduct from stock
        val currentStock = getFeedStockQuantity(consumption.feedName)
        val newStock = (currentStock - consumption.quantityConsumed).coerceAtLeast(0f)
        updateFeedStock(consumption.feedName, newStock)

        val values = ContentValues().apply {
            put(KEY_FC_DATE, consumption.date)
            put(KEY_FC_FEED_NAME, consumption.feedName)
            put(KEY_FC_QTY, consumption.quantityConsumed)
        }
        return db.insert(TABLE_FEED_CONSUMPTION, null, values)
    }

    private fun getFeedStockQuantity(feedName: String): Float {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $KEY_FEED_QTY_IN_STOCK FROM $TABLE_FEED_INVENTORY WHERE $KEY_FEED_NAME = ?", arrayOf(feedName))
        var qty = 0f
        if (cursor.moveToFirst()) {
            qty = cursor.getFloat(0)
        }
        cursor.close()
        return qty
    }

    // ==========================================
    // FINANCIAL OPERATIONS
    // ==========================================

    fun getAllFinancialRecords(): List<FinancialRecord> {
        val list = mutableListOf<FinancialRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FINANCES ORDER BY $KEY_FIN_DATE DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    FinancialRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        type = TransactionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIN_TYPE))),
                        category = TransactionCategory.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIN_CATEGORY))),
                        amount = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_FIN_AMOUNT)),
                        date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIN_DATE)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIN_DESC))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertFinancialRecord(record: FinancialRecord): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_FIN_TYPE, record.type.name)
            put(KEY_FIN_CATEGORY, record.category.name)
            put(KEY_FIN_AMOUNT, record.amount)
            put(KEY_FIN_DATE, record.date)
            put(KEY_FIN_DESC, record.description)
        }
        return db.insert(TABLE_FINANCES, null, values)
    }

    // ==========================================
    // USER AUTHENTICATION OPERATIONS
    // ==========================================

    private fun hashPassword(password: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            val hexString = StringBuilder()
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (ex: Exception) {
            password
        }
    }

    fun registerUser(username: String, email: String, phone: String, password: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_NAME, username.trim())
            put(KEY_USER_EMAIL, email.trim())
            put(KEY_USER_PHONE, phone.trim())
            put(KEY_USER_PASSWORD_HASH, hashPassword(password))
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun authenticateUser(username: String, password: String): User? {
        val db = this.readableDatabase
        val passwordHash = hashPassword(password)
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $KEY_USER_NAME = ? AND $KEY_USER_PASSWORD_HASH = ?",
            arrayOf(username.trim(), passwordHash)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD_HASH)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE))
            )
        }
        cursor.close()
        return user
    }

    fun getUserByUsername(username: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $KEY_USER_NAME = ?",
            arrayOf(username.trim())
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD_HASH)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE))
            )
        }
        cursor.close()
        return user
    }

    fun isUserExists(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_USERS WHERE $KEY_USER_NAME = ?", arrayOf(username.trim()))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}


