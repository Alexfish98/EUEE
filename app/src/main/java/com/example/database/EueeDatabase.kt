package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        GradeEntity::class,
        UnitEntity::class,
        TopicEntity::class,
        QuestionBankEntity::class,
        QuestionEntity::class,
        QuestionProgressEntity::class,
        AttemptEntity::class,
        StudySessionEntity::class,
        NoteEntity::class,
        FlashcardEntity::class,
        ExamResultEntity::class,
        InProgressExamEntity::class,
        StudyTaskEntity::class,
        AlarmEntity::class,
        AlarmHistoryEntity::class,
        FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EueeDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
    abstract fun questionDao(): QuestionDao
    abstract fun learningProgressDao(): LearningProgressDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun notebookDao(): NotebookDao
    abstract fun examDao(): ExamDao
    abstract fun plannerAndAlarmDao(): PlannerAndAlarmDao

    companion object {
        const val DATABASE_NAME = "euee_mastery.db"

        // Prepared migration scaffold for future schema evolutions
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Future schema migration path without destructive fallback
            }
        }

        @Volatile
        private var INSTANCE: EueeDatabase? = null

        fun getInstance(context: Context): EueeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): EueeDatabase {
            return Room.databaseBuilder(
                appContext,
                EueeDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            SeedDataProvider.seedIfEmpty(getInstance(appContext))
                        }
                    }
                })
                .build()
        }
    }
}
