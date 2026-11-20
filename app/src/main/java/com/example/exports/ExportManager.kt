package com.example.exports

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.database.*
import com.example.domain.AnswerValidator
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ExportManager {

    suspend fun exportFullBackupJson(db: EueeDatabase): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val subjectsArr = JSONArray()
        db.curriculumDao().getAllSubjects().forEach { s ->
            subjectsArr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("category", s.category)
                put("colorHex", s.colorHex); put("iconName", s.iconName); put("isCoreEuee", s.isCoreEuee)
            })
        }
        root.put("subjects", subjectsArr)

        val gradesArr = JSONArray()
        db.curriculumDao().getAllGrades().forEach { g ->
            gradesArr.put(JSONObject().apply { put("id", g.id); put("level", g.level); put("label", g.label) })
        }
        root.put("grades", gradesArr)

        val unitsArr = JSONArray()
        db.curriculumDao().getAllUnits().forEach { u ->
            unitsArr.put(JSONObject().apply {
                put("id", u.id); put("subjectId", u.subjectId); put("gradeId", u.gradeId)
                put("unitNumber", u.unitNumber); put("title", u.title)
            })
        }
        root.put("units", unitsArr)

        val topicsArr = JSONArray()
        db.curriculumDao().getAllTopics().forEach { t ->
            topicsArr.put(JSONObject().apply {
                put("id", t.id); put("unitId", t.unitId); put("title", t.title); put("summary", t.summary)
            })
        }
        root.put("topics", topicsArr)

        val banksArr = JSONArray()
        db.curriculumDao().getAllQuestionBanks().forEach { b ->
            banksArr.put(JSONObject().apply {
                put("id", b.id); put("name", b.name); put("description", b.description); put("sourceType", b.sourceType)
            })
        }
        root.put("questionBanks", banksArr)

        root.put("questions", questionsToJsonArray(db.questionDao().getAllQuestions(), db))

        val progressArr = JSONArray()
        db.learningProgressDao().getAllProgress().forEach { p ->
            progressArr.put(JSONObject().apply {
                put("questionId", p.questionId); put("attempts", p.attempts)
                put("correctCount", p.correctCount); put("incorrectCount", p.incorrectCount)
                put("confidence", p.confidence); put("nextReviewAt", p.nextReviewAt)
                put("lastReviewedAt", p.lastReviewedAt); put("intervalDays", p.intervalDays)
                put("easeFactor", p.easeFactor); put("masteryStatus", p.masteryStatus)
                put("avgResponseTimeMs", p.avgResponseTimeMs); put("hintsUsedTotal", p.hintsUsedTotal)
                put("consecutiveCorrect", p.consecutiveCorrect)
            })
        }
        root.put("progress", progressArr)

        val notesArr = JSONArray()
        db.notebookDao().getAllNotes().forEach { n ->
            notesArr.put(JSONObject().apply {
                put("title", n.title); put("content", n.content); put("subjectId", n.subjectId ?: JSONObject.NULL)
                put("topicId", n.topicId ?: JSONObject.NULL); put("isPinned", n.isPinned)
                put("isMistakeEntry", n.isMistakeEntry); put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
            })
        }
        root.put("notes", notesArr)

        val cardsArr = JSONArray()
        db.notebookDao().getAllFlashcards().forEach { c ->
            cardsArr.put(JSONObject().apply {
                put("front", c.front); put("back", c.back); put("hint", c.hint)
                put("subjectId", c.subjectId ?: JSONObject.NULL); put("confidence", c.confidence)
                put("intervalDays", c.intervalDays); put("nextReviewAt", c.nextReviewAt)
            })
        }
        root.put("flashcards", cardsArr)

        val tasksArr = JSONArray()
        db.plannerAndAlarmDao().getAllTasks().forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("title", t.title); put("notes", t.notes); put("subjectId", t.subjectId ?: JSONObject.NULL)
                put("scheduledDateEpochDay", t.scheduledDateEpochDay); put("startTimeMinutes", t.startTimeMinutes)
                put("durationMinutes", t.durationMinutes); put("priority", t.priority); put("isCompleted", t.isCompleted)
            })
        }
        root.put("tasks", tasksArr)

        val examsArr = JSONArray()
        db.examDao().getAllExamResults().forEach { e ->
            examsArr.put(JSONObject().apply {
                put("title", e.title); put("totalQuestions", e.totalQuestions); put("answeredCount", e.answeredCount)
                put("correctCount", e.correctCount); put("durationMinutes", e.durationMinutes)
                put("timeTakenSeconds", e.timeTakenSeconds); put("scorePercent", e.scorePercent)
                put("topicBreakdownJson", e.topicBreakdownJson); put("incorrectQuestionIdsJson", e.incorrectQuestionIdsJson)
                put("answerMapJson", e.answerMapJson); put("completedAt", e.completedAt)
            })
        }
        root.put("examResults", examsArr)

        return root.toString(2)
    }

    suspend fun restoreFullBackupJson(db: EueeDatabase, jsonText: String): Int {
        val root = JSONObject(jsonText)
        require(root.has("questions") && root.has("subjects")) { "Invalid EUEE Mastery backup file." }

        var restoredQuestions = 0
        db.withTransaction {
            val subjectsMap = db.curriculumDao().getAllSubjects().associateBy { it.name }.toMutableMap()
            val gradesMap = db.curriculumDao().getAllGrades().associateBy { it.level }.toMutableMap()
            val existingBanks = db.curriculumDao().getAllQuestionBanks()
            val defaultBankId = existingBanks.firstOrNull()?.id ?: db.curriculumDao().insertQuestionBank(
                QuestionBankEntity(name = "Restored Bank", sourceType = "IMPORTED")
            )

            // Restore subjects if missing
            val sArr = root.optJSONArray("subjects") ?: JSONArray()
            for (i in 0 until sArr.length()) {
                val obj = sArr.getJSONObject(i)
                val name = obj.getString("name")
                if (name !in subjectsMap) {
                    val id = db.curriculumDao().insertSubject(
                        SubjectEntity(
                            name = name,
                            category = obj.optString("category", "Custom"),
                            colorHex = obj.optLong("colorHex", 0xFF1A3B70),
                            iconName = obj.optString("iconName", "School"),
                            isCoreEuee = obj.optBoolean("isCoreEuee", false)
                        )
                    )
                    subjectsMap[name] = SubjectEntity(id = id, name = name, category = obj.optString("category", "Custom"), colorHex = obj.optLong("colorHex", 0xFF1A3B70), iconName = "School")
                }
            }

            // Restore questions safely (upsert by uniqueCode)
            val qArr = root.optJSONArray("questions") ?: JSONArray()
            val unitsCache = db.curriculumDao().getAllUnits().toMutableList()
            val topicsCache = db.curriculumDao().getAllTopics().toMutableList()
            val existingByCode = db.questionDao().getAllQuestions().associateBy { it.uniqueCode }

            val toInsert = mutableListOf<QuestionEntity>()
            for (i in 0 until qArr.length()) {
                val qObj = qArr.getJSONObject(i)
                val code = qObj.optString("uniqueCode").ifBlank { "REST-$i" }
                val sName = qObj.optString("subject", "General")
                val subjectId = subjectsMap[sName]?.id ?: subjectsMap.values.first().id
                val gradeLevel = qObj.optInt("grade", 12)
                val gradeId = gradesMap[gradeLevel]?.id ?: gradesMap.values.first().id

                val uTitle = qObj.optString("unit", "General Unit")
                val unit = unitsCache.firstOrNull { it.subjectId == subjectId && it.gradeId == gradeId && it.title == uTitle }
                    ?: run {
                        val uid = db.curriculumDao().insertUnit(UnitEntity(subjectId = subjectId, gradeId = gradeId, unitNumber = 1, title = uTitle))
                        UnitEntity(id = uid, subjectId = subjectId, gradeId = gradeId, unitNumber = 1, title = uTitle).also { unitsCache.add(it) }
                    }

                val tTitle = qObj.optString("topic", "General Topic")
                val topic = topicsCache.firstOrNull { it.unitId == unit.id && it.title == tTitle }
                    ?: run {
                        val tid = db.curriculumDao().insertTopic(TopicEntity(unitId = unit.id, title = tTitle))
                        TopicEntity(id = tid, unitId = unit.id, title = tTitle).also { topicsCache.add(it) }
                    }

                toInsert.add(
                    QuestionEntity(
                        id = existingByCode[code]?.id ?: 0L,
                        uniqueCode = code,
                        bankId = defaultBankId,
                        subjectId = subjectId,
                        gradeId = gradeId,
                        unitId = unit.id,
                        topicId = topic.id,
                        difficulty = qObj.optString("difficulty", "MEDIUM"),
                        questionType = qObj.optString("questionType", "MULTIPLE_CHOICE"),
                        questionText = qObj.getString("questionText"),
                        correctAnswer = qObj.getString("correctAnswer"),
                        acceptedAnswersJson = qObj.optJSONArray("acceptedAnswers")?.toString() ?: "[]",
                        optionsJson = qObj.optJSONArray("options")?.toString() ?: "[]",
                        hintsJson = qObj.optJSONArray("hints")?.toString() ?: "[]",
                        explanation = qObj.optString("explanation", ""),
                        whyCorrect = qObj.optString("whyCorrect", ""),
                        commonMistakes = qObj.optString("commonMistakes", ""),
                        conceptExplanation = qObj.optString("conceptExplanation", ""),
                        workedExample = qObj.optString("workedExample", ""),
                        memoryTip = qObj.optString("memoryTip", ""),
                        examTrap = qObj.optString("examTrap", ""),
                        numericalTolerance = qObj.optDouble("numericalTolerance", 0.01),
                        referencesText = qObj.optString("references", ""),
                        isBookmarked = qObj.optBoolean("isBookmarked", false)
                    )
                )
            }
            if (toInsert.isNotEmpty()) {
                db.questionDao().insertQuestions(toInsert)
                restoredQuestions = toInsert.size
            }

            // Restore notes
            val notesArr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until notesArr.length()) {
                val nObj = notesArr.getJSONObject(i)
                db.notebookDao().upsertNote(
                    NoteEntity(
                        title = nObj.getString("title"),
                        content = nObj.getString("content"),
                        subjectId = nObj.optLong("subjectId").takeIf { !nObj.isNull("subjectId") },
                        isPinned = nObj.optBoolean("isPinned", false),
                        isMistakeEntry = nObj.optBoolean("isMistakeEntry", false),
                        createdAt = nObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Restore flashcards
            val cardsArr = root.optJSONArray("flashcards") ?: JSONArray()
            for (i in 0 until cardsArr.length()) {
                val cObj = cardsArr.getJSONObject(i)
                db.notebookDao().upsertFlashcard(
                    FlashcardEntity(
                        front = cObj.getString("front"),
                        back = cObj.getString("back"),
                        hint = cObj.optString("hint", ""),
                        confidence = cObj.optInt("confidence", 0),
                        intervalDays = cObj.optDouble("intervalDays", 0.0),
                        nextReviewAt = cObj.optLong("nextReviewAt", 0L)
                    )
                )
            }
        }
        return restoredQuestions
    }

    suspend fun exportQuestionBankJson(db: EueeDatabase, questions: List<QuestionEntity>): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("count", questions.size)
        root.put("questions", questionsToJsonArray(questions, db))
        return root.toString(2)
    }

    suspend fun exportQuestionsCsv(db: EueeDatabase, questions: List<QuestionEntity>): String {
        val subjects = db.curriculumDao().getAllSubjects().associateBy { it.id }
        val grades = db.curriculumDao().getAllGrades().associateBy { it.id }
        val units = db.curriculumDao().getAllUnits().associateBy { it.id }
        val topics = db.curriculumDao().getAllTopics().associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("uniqueCode,subject,grade,unit,topic,difficulty,questionType,questionText,correctAnswer,options,hints,explanation")
        for (q in questions) {
            val opts = AnswerValidator.parseStringArrayJson(q.optionsJson).joinToString("|")
            val hints = AnswerValidator.parseStringArrayJson(q.hintsJson).joinToString("|")
            val row = listOf(
                q.uniqueCode,
                subjects[q.subjectId]?.name ?: "General",
                (grades[q.gradeId]?.level ?: 12).toString(),
                units[q.unitId]?.title ?: "Unit",
                topics[q.topicId]?.title ?: "Topic",
                q.difficulty,
                q.questionType,
                q.questionText,
                q.correctAnswer,
                opts,
                hints,
                q.explanation
            ).joinToString(",") { csvEscape(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    suspend fun exportNotesMarkdown(db: EueeDatabase): String {
        val subjects = db.curriculumDao().getAllSubjects().associateBy { it.id }
        val notes = db.notebookDao().getAllNotes()
        return buildString {
            appendLine("# EUEE Mastery Personal Notebook")
            appendLine()
            notes.forEach { note ->
                val sName = note.subjectId?.let { subjects[it]?.name } ?: "General"
                appendLine("## ${note.title} [$sName]")
                if (note.isPinned) appendLine("_Pinned Note_")
                appendLine()
                appendLine(note.content)
                appendLine()
                appendLine("---")
            }
        }
    }

    suspend fun exportFlashcardsJson(db: EueeDatabase): String {
        val cards = db.notebookDao().getAllFlashcards()
        val arr = JSONArray()
        cards.forEach { c ->
            arr.put(JSONObject().apply {
                put("front", c.front)
                put("back", c.back)
                put("hint", c.hint)
                put("confidence", c.confidence)
            })
        }
        return JSONObject().put("flashcards", arr).toString(2)
    }

    suspend fun exportStudyPlanCsv(db: EueeDatabase): String {
        val subjects = db.curriculumDao().getAllSubjects().associateBy { it.id }
        val tasks = db.plannerAndAlarmDao().getAllTasks()
        return buildString {
            appendLine("title,subject,epochDay,startMinutes,durationMinutes,priority,completed")
            tasks.forEach { t ->
                appendLine(
                    listOf(
                        t.title,
                        t.subjectId?.let { subjects[it]?.name } ?: "General",
                        t.scheduledDateEpochDay.toString(),
                        t.startTimeMinutes.toString(),
                        t.durationMinutes.toString(),
                        t.priority,
                        t.isCompleted.toString()
                    ).joinToString(",") { csvEscape(it) }
                )
            }
        }
    }

    suspend fun exportExamResultsJson(db: EueeDatabase): String {
        val results = db.examDao().getAllExamResults()
        val arr = JSONArray()
        results.forEach { r ->
            arr.put(JSONObject().apply {
                put("title", r.title)
                put("totalQuestions", r.totalQuestions)
                put("correctCount", r.correctCount)
                put("scorePercent", r.scorePercent)
                put("timeTakenSeconds", r.timeTakenSeconds)
                put("completedAt", r.completedAt)
            })
        }
        return JSONObject().put("examResults", arr).toString(2)
    }

    fun writePrintableExamPdf(
        outputStream: OutputStream,
        title: String,
        questions: List<QuestionEntity>
    ) {
        val doc = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 42f
            val contentWidth = (pageWidth - margin * 2).toInt()

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 11f
            }

            var pageNumber = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = margin + 20f

            canvas.drawText("EUEE MASTERY — PRINTABLE EXAM SHEET", margin, y, titlePaint)
            y += 22f
            canvas.drawText(title, margin, y, bodyPaint)
            y += 24f

            questions.forEachIndexed { idx, q ->
                val options = AnswerValidator.parseStringArrayJson(q.optionsJson)
                val blockText = buildString {
                    append("${idx + 1}. ${q.questionText}\n")
                    options.forEachIndexed { oIdx, opt ->
                        val letter = ('A' + oIdx)
                        append("   $letter) $opt\n")
                    }
                }
                val layout = StaticLayout.Builder
                    .obtain(blockText, 0, blockText.length, bodyPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.15f)
                    .build()

                if (y + layout.height > pageHeight - margin) {
                    doc.finishPage(page)
                    pageNumber++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = margin + 10f
                }
                canvas.save()
                canvas.translate(margin, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 10f
            }

            // Answer key section on a fresh page
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin + 20f
            canvas.drawText("ANSWER KEY", margin, y, titlePaint)
            y += 24f
            questions.forEachIndexed { idx, q ->
                if (y > pageHeight - margin) {
                    doc.finishPage(page)
                    pageNumber++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = margin + 10f
                }
                canvas.drawText("${idx + 1}. ${q.correctAnswer}", margin, y, bodyPaint)
                y += 16f
            }

            doc.finishPage(page)
            doc.writeTo(outputStream)
        } catch (err: IllegalStateException) {
            // Fallback for headless JVM / Robolectric environments lacking native Skia PdfDocument JNI
            writeFallbackPdf14(outputStream, title, questions)
        } catch (err: UnsatisfiedLinkError) {
            writeFallbackPdf14(outputStream, title, questions)
        } finally {
            runCatching { doc.close() }
        }
    }

    private fun writeFallbackPdf14(
        outputStream: OutputStream,
        title: String,
        questions: List<QuestionEntity>
    ) {
        fun esc(s: String): String = s
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\r", "")
            .replace("\n", " ")
            .map { if (it.code in 32..126) it else ' ' }
            .joinToString("")

        val lines = mutableListOf<String>()
        lines += "EUEE MASTERY - PRINTABLE EXAM SHEET"
        lines += title
        lines += ""
        questions.forEachIndexed { idx, q ->
            lines += "${idx + 1}. ${q.questionText}"
            AnswerValidator.parseStringArrayJson(q.optionsJson).forEachIndexed { oIdx, opt ->
                lines += "   ${'A' + oIdx}) $opt"
            }
        }
        lines += ""
        lines += "ANSWER KEY"
        questions.forEachIndexed { idx, q ->
            lines += "${idx + 1}. ${q.correctAnswer}"
        }

        val streamContent = buildString {
            append("BT\n/F1 11 Tf\n42 790 Td\n14 TL\n")
            lines.take(50).forEach { line ->
                append("(${esc(line.take(90))}) Tj T*\n")
            }
            append("ET\n")
        }
        val streamBytes = streamContent.toByteArray(Charsets.US_ASCII)
        val objs = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n",
            "4 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n${streamContent}endstream\nendobj\n",
            "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
        )
        val pdf = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objs.forEach { obj ->
            offsets += pdf.length
            pdf.append(obj)
        }
        val xrefOffset = pdf.length
        pdf.append("xref\n0 ${objs.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { off ->
            pdf.append("%010d 00000 n \n".format(off))
        }
        pdf.append("trailer\n<< /Size ${objs.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        outputStream.write(pdf.toString().toByteArray(Charsets.US_ASCII))
    }

    fun renderQuestionCardPng(question: QuestionEntity, subjectName: String): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.parseColor("#1A3B70"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(56f, 90f, width - 56f, height - 90f, 36f, 36f, cardPaint)

        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D9488")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("EUEE MASTERY • $subjectName", 96f, 164f, tagPaint)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
        }
        val textContent = buildString {
            appendLine(question.questionText)
            appendLine()
            val opts = AnswerValidator.parseStringArrayJson(question.optionsJson)
            opts.forEachIndexed { idx, opt ->
                appendLine("${'A' + idx}) $opt")
            }
        }
        val layout = StaticLayout.Builder
            .obtain(textContent, 0, textContent.length, bodyPaint, width - 192)
            .setLineSpacing(6f, 1.2f)
            .build()
        canvas.save()
        canvas.translate(96f, 220f)
        layout.draw(canvas)
        canvas.restore()

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 26f
        }
        canvas.drawText("Code: ${question.uniqueCode} • Answer: ${question.correctAnswer}", 96f, height - 130f, footerPaint)
        return bitmap
    }

    fun shareFile(context: Context, fileName: String, mimeType: String, writer: (OutputStream) -> Unit) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use(writer)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via EUEE Mastery"))
    }

    private suspend fun questionsToJsonArray(questions: List<QuestionEntity>, db: EueeDatabase): JSONArray {
        val subjects = db.curriculumDao().getAllSubjects().associateBy { it.id }
        val grades = db.curriculumDao().getAllGrades().associateBy { it.id }
        val units = db.curriculumDao().getAllUnits().associateBy { it.id }
        val topics = db.curriculumDao().getAllTopics().associateBy { it.id }

        val arr = JSONArray()
        questions.forEach { q ->
            arr.put(JSONObject().apply {
                put("uniqueCode", q.uniqueCode)
                put("subject", subjects[q.subjectId]?.name ?: "General")
                put("grade", grades[q.gradeId]?.level ?: 12)
                put("unit", units[q.unitId]?.title ?: "General Unit")
                put("topic", topics[q.topicId]?.title ?: "General Topic")
                put("difficulty", q.difficulty)
                put("questionType", q.questionType)
                put("questionText", q.questionText)
                put("correctAnswer", q.correctAnswer)
                put("acceptedAnswers", JSONArray(q.acceptedAnswersJson))
                put("options", JSONArray(q.optionsJson))
                put("hints", JSONArray(q.hintsJson))
                put("explanation", q.explanation)
                put("whyCorrect", q.whyCorrect)
                put("commonMistakes", q.commonMistakes)
                put("conceptExplanation", q.conceptExplanation)
                put("workedExample", q.workedExample)
                put("memoryTip", q.memoryTip)
                put("examTrap", q.examTrap)
                put("numericalTolerance", q.numericalTolerance)
                put("references", q.referencesText)
                put("isBookmarked", q.isBookmarked)
            })
        }
        return arr
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
