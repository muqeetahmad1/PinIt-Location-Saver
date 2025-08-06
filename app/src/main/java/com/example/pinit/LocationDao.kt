package com.example.pinit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete


@Dao
interface LocationDao {

    @Insert
    suspend fun insertLocation(location: SavedLocation)

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<SavedLocation>
    @Delete
    fun deleteLocation(location: SavedLocation)

}
