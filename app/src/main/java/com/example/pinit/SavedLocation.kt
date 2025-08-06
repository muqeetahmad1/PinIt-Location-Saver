package com.example.pinit

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
