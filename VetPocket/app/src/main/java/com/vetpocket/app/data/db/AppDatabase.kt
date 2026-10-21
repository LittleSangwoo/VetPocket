package com.vetpocket.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PetProfileEntity::class,
        WeightRecordEntity::class,
        MedicalEventEntity::class,
        DiaryNoteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun weightDao(): WeightDao
    abstract fun medicalDao(): MedicalDao
    abstract fun diaryDao(): DiaryDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "vetpocket.db")
                .build()
    }
}
