package com.vetpocket.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_profile WHERE id = 1")
    fun observe(): Flow<PetProfileEntity?>

    @Query("SELECT * FROM pet_profile WHERE id = 1")
    suspend fun get(): PetProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pet: PetProfileEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_records ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): WeightRecordEntity?

    @Insert
    suspend fun insert(record: WeightRecordEntity): Long

    @Delete
    suspend fun delete(record: WeightRecordEntity)
}

@Dao
interface MedicalDao {
    @Query("SELECT * FROM medical_events ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<MedicalEventEntity>>

    @Query("SELECT * FROM medical_events")
    suspend fun getAll(): List<MedicalEventEntity>

    @Insert
    suspend fun insert(event: MedicalEventEntity): Long

    @Delete
    suspend fun delete(event: MedicalEventEntity)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_notes ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DiaryNoteEntity>>

    @Insert
    suspend fun insert(note: DiaryNoteEntity): Long

    @Delete
    suspend fun delete(note: DiaryNoteEntity)
}
