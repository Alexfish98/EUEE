package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarms.AlarmScheduler
import com.example.core.model.AlarmCategory
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.*
import com.example.exports.ExportManager
import com.example.focus.DndManager
import com.example.imports.ImportParser
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EueeSystemIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: EueeDatabase
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var repo: EueeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EueeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = PreferencesRepository(context)
        repo = EueeRepository(db, prefsRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `seeded curriculum covers all 6 EUEE subjects and Grades 9 to 12`() = runTest {
        repo.ensureSeeded()
        val subjects = repo.observeSubjects().first()
        val grades = repo.observeGrades().first()
        val count = repo.observeTotalQuestionCount().first()

        assertTrue(subjects.map { it.name }.containsAll(listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Aptitude")))
        assertEquals(listOf(9, 10, 11, 12), grades.map { it.level })
        assertTrue(count >= 35)
    }

    @Test
    fun `attempt submission updates progress, survives DB query, and logs mistakes into notebook`() = runTest {
        repo.ensureSeeded()
        val q = db.questionDao().getAllQuestions().first()

        // Bookmark question
        repo.setBookmarked(q.id, true)
        val bookmarked = repo.getQuestionById(q.id)!!
        assertTrue(bookmarked.isBookmarked)

        // Submit incorrect answer -> should record progress & add mistake note to notebook
        repo.recordAttempt(
            question = bookmarked,
            userAnswer = "WRONG_CHOICE",
            isCorrect = false,
            responseTimeMs = 4500L,
            hintsUsed = 1,
            confidenceRating = 2
        )

        val progress = db.learningProgressDao().getProgressForQuestion(q.id)
        assertNotNull(progress)
        assertEquals(1, progress!!.attempts)
        assertEquals(1, progress.incorrectCount)

        val notes = repo.observeNotes().first()
        assertTrue(notes.any { it.isMistakeEntry && it.linkedQuestionId == q.id })
    }

    @Test
    fun `import, backup export, PDF export, and restore round-trip succeed offline`() = runTest {
        repo.ensureSeeded()
        val initialCount = db.questionDao().getTotalQuestionCount()

        val parsed = ImportParser.parse(ImportParser.DOCUMENTED_JSON_SCHEMA, emptySet())
        assertEquals(1, parsed.validItems.size)
        val subjects = db.curriculumDao().getAllSubjects()
        val grades = db.curriculumDao().getAllGrades()
        val units = db.curriculumDao().getAllUnits()
        val topics = db.curriculumDao().getAllTopics()
        val entity = ImportParser.toQuestionEntity(
            parsed.validItems.first(),
            bankId = 1L,
            subjectId = subjects.first().id,
            gradeId = grades.first().id,
            unitId = units.first().id,
            topicId = topics.first().id
        )
        repo.batchImportQuestions(listOf(entity))
        assertEquals(initialCount + 1, db.questionDao().getTotalQuestionCount())

        // Export full JSON backup
        val backupJson = ExportManager.exportFullBackupJson(db)
        assertTrue(backupJson.contains("EUEE-CHEM-12-101"))

        // Export printable PDF exam sheet
        val pdfOut = ByteArrayOutputStream()
        ExportManager.writePrintableExamPdf(pdfOut, "Test EUEE Paper", db.questionDao().getAllQuestions())
        assertTrue(pdfOut.toByteArray().isNotEmpty())

        // Restore into a fresh database
        val db2 = Room.inMemoryDatabaseBuilder(context, EueeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SeedDataProvider.seedIfEmpty(db2)
        val restoredCount = ExportManager.restoreFullBackupJson(db2, backupJson)
        assertEquals(initialCount + 1, restoredCount)
        db2.close()
    }

    @Test
    fun `native alarm scheduler, notification channels, and DND status work cleanly`() {
        NotificationHelper.createChannels(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAm = shadowOf(am)

        val alarm = AlarmEntity(
            id = 7L,
            label = "Physics Drill",
            hour = 7,
            minute = 15,
            daysOfWeekMask = 0b0111110,
            isRepeating = true,
            isEnabled = true,
            category = AlarmCategory.SUBJECT_ALARM.name
        )
        val triggerMs = AlarmScheduler.computeNextTriggerMillis(7, 15, true, alarm.daysOfWeekMask, ZonedDateTime.now())
        assertTrue(triggerMs > System.currentTimeMillis())

        val result = AlarmScheduler.scheduleAlarm(context, alarm)
        assertTrue(result.scheduled)
        assertNotNull(shadowAm.nextScheduledAlarm)

        val dndStatus = DndManager.getStatus(context)
        assertNotNull(dndStatus)
    }
}
