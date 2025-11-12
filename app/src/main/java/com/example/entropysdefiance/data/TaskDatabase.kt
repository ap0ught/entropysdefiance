package com.example.entropysdefiance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class], version = 4)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to existing table
                db.execSQL("ALTER TABLE tasks ADD COLUMN autoId INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN createdAt TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN isCompleted INTEGER")

                // Update existing rows with default values
                db.execSQL("UPDATE tasks SET autoId = 0 WHERE autoId IS NULL")
                db.execSQL("UPDATE tasks SET isCompleted = 0 WHERE isCompleted IS NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create a new table with the correct primary key structure
                db.execSQL("CREATE TABLE tasks_new (id TEXT NOT NULL, title TEXT NOT NULL, timeHorizon TEXT NOT NULL, autoId INTEGER PRIMARY KEY AUTOINCREMENT, createdAt TEXT, isCompleted INTEGER NOT NULL)")

                // Copy data from old table to new table
                db.execSQL("INSERT INTO tasks_new (id, title, timeHorizon, autoId, createdAt, isCompleted) SELECT id, title, timeHorizon, autoId, createdAt, isCompleted FROM tasks")

                // Drop the old table
                db.execSQL("DROP TABLE tasks")

                // Rename the new table to the original name
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns for deadline tasks
                db.execSQL("ALTER TABLE tasks ADD COLUMN isDeadlineTask INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN deadline TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN daysNeeded INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN doDate TEXT")
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 