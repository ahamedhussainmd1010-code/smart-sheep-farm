package com.example.myapplication2sheepfarm

enum class AnimalType {
    SHEEP, GOAT
}

enum class AgeCategory {
    ADULT, BABY
}

enum class Gender {
    MALE, FEMALE
}

enum class PregnancyStatus {
    NONE, BREEDING, PREGNANT, LAMBED, KIDDED, FAILED
}

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionCategory {
    ANIMAL_SALE, WOOL_SALE, FEED_EXPENSE, VET_EXPENSE, LABOR_EXPENSE
}

data class Animal(
    val id: Long = 0L,
    val tagNumber: String,
    val type: AnimalType,
    val breed: String,
    val gender: Gender,
    val ageCategory: AgeCategory,
    val weight: Float,
    val healthStatus: String,
    val purchaseDate: String, // YYYY-MM-DD
    val purchasePrice: Float,
    val avatarId: Int // Index for local mock icon/photo representation
)

data class VaccinationSchedule(
    val dateMonthDay: String, // MM-DD
    val vaccineName: String,
    val targetDisease: String,
    val dewormingRequired: Boolean
)

data class VaccinationRecord(
    val id: Long = 0L,
    val animalId: Long,
    val vaccineName: String,
    val dateAdministered: String, // YYYY-MM-DD
    val notes: String
)

data class DewormingRecord(
    val id: Long = 0L,
    val animalId: Long,
    val drugUsed: String,
    val dateAdministered: String, // YYYY-MM-DD
    val notes: String
)

data class BreedingRecord(
    val id: Long = 0L,
    val femaleId: Long,
    val maleId: Long,
    val breedingDate: String, // YYYY-MM-DD
    val expectedDeliveryDate: String, // YYYY-MM-DD
    val status: PregnancyStatus,
    val birthDate: String? = null, // YYYY-MM-DD
    val offspringCount: Int = 0,
    val notes: String
)

data class FeedInventory(
    val id: Long = 0L,
    val feedName: String,
    val quantityInStock: Float,
    val unit: String,
    val lowStockThreshold: Float
)

data class FeedSchedule(
    val id: Long = 0L,
    val feedName: String,
    val timeOfDay: String, // e.g. "08:00 AM"
    val quantity: Float,
    val targetGroup: String // e.g. "Pregnant Ewes", "Weaned Lambs"
)

data class FeedConsumption(
    val id: Long = 0L,
    val date: String, // YYYY-MM-DD
    val feedName: String,
    val quantityConsumed: Float
)

data class FinancialRecord(
    val id: Long = 0L,
    val type: TransactionType,
    val category: TransactionCategory,
    val amount: Float,
    val date: String, // YYYY-MM-DD
    val description: String
)
