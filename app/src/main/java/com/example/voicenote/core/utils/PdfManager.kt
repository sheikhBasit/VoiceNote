package com.example.voicenote.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.Task
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfManager(private val context: Context) {

    private val titlePaint = Paint().apply {
        textSize = 24f
        isFakeBoldText = true
        color = Color.BLACK
    }
    private val subTitlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = Color.DKGRAY
    }
    private val bodyPaint = Paint().apply {
        textSize = 12f
        color = Color.BLACK
    }
    private val metaPaint = Paint().apply {
        textSize = 10f
        color = Color.GRAY
    }

    private val margin = 50f
    private val maxWidth = 500f
    private val pageHeight = 842f
    private val bottomMargin = 50f

    fun generateNotePdf(note: Note, tasks: List<Task>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var currentY = 50f

        // Title
        currentY = drawLine("Meeting Recap: ${note.title}", margin, currentY, titlePaint, canvas)
        currentY += 10f

        // Metadata
        val dateStr = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.timestamp))
        currentY = drawLine("Date: $dateStr", margin, currentY, metaPaint, canvas)
        currentY = drawLine("Priority: ${note.priority}", margin, currentY, metaPaint, canvas)
        currentY = drawLine("Role ID: ${note.userId}", margin, currentY, metaPaint, canvas)
        currentY += 20f

        // Summary
        currentY = drawLine("Executive Summary", margin, currentY, subTitlePaint, canvas)
        currentY += 5f
        val summaryResult = drawWrappedText(note.summary, margin, currentY, maxWidth, canvas, bodyPaint, pdfDocument, pageInfo, currentPage)
        currentPage = summaryResult.first
        canvas = currentPage.canvas
        currentY = summaryResult.second + 20f

        // Tasks
        if (tasks.isNotEmpty()) {
            if (currentY > pageHeight - 100f) {
                pdfDocument.finishPage(currentPage)
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = 50f
            }
            currentY = drawLine("Action Items", margin, currentY, subTitlePaint, canvas)
            currentY += 5f
            
            tasks.forEach { task ->
                val status = if (task.isDone) "☑" else "☐"
                val deadline = task.deadline?.let { " (Due: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))})" } ?: ""
                val taskText = "$status ${task.description}$deadline"
                
                val taskResult = drawWrappedText(taskText, margin + 10f, currentY, maxWidth - 10f, canvas, bodyPaint, pdfDocument, pageInfo, currentPage)
                currentPage = taskResult.first
                canvas = currentPage.canvas
                currentY = taskResult.second + 5f
            }
            currentY += 15f
        }

        // Transcript
        if (note.transcript.isNotEmpty()) {
            if (currentY > pageHeight - 100f) {
                pdfDocument.finishPage(currentPage)
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = 50f
            }
            currentY = drawLine("Full Transcript", margin, currentY, subTitlePaint, canvas)
            currentY += 5f
            val transcriptResult = drawWrappedText(note.transcript, margin, currentY, maxWidth, canvas, bodyPaint, pdfDocument, pageInfo, currentPage)
            currentPage = transcriptResult.first
            currentY = transcriptResult.second
        }

        pdfDocument.finishPage(currentPage)

        val fileName = "Recap_${note.title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            sharePdf(file)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawLine(text: String, x: Float, y: Float, paint: Paint, canvas: Canvas): Float {
        canvas.drawText(text, x, y, paint)
        return y + paint.textSize + 5f
    }

    private fun drawWrappedText(
        text: String, 
        x: Float, 
        y: Float, 
        maxWidth: Float, 
        canvas: Canvas, 
        paint: Paint,
        pdfDocument: PdfDocument,
        pageInfo: PdfDocument.PageInfo,
        initialPage: PdfDocument.Page
    ): Pair<PdfDocument.Page, Float> {
        var currentCanvas = canvas
        var currentPage = initialPage
        var currentY = y
        val words = text.split(" ")
        var line = StringBuilder()

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) <= maxWidth) {
                line.append(if (line.isEmpty()) "" else " ").append(word)
            } else {
                currentCanvas.drawText(line.toString(), x, currentY, paint)
                line = StringBuilder(word)
                currentY += paint.textSize + 5f
                
                if (currentY > pageHeight - bottomMargin) {
                    pdfDocument.finishPage(currentPage)
                    currentPage = pdfDocument.startPage(pageInfo)
                    currentCanvas = currentPage.canvas
                    currentY = 50f
                }
            }
        }
        if (line.isNotEmpty()) {
            currentCanvas.drawText(line.toString(), x, currentY, paint)
            currentY += paint.textSize + 5f
        }
        return Pair(currentPage, currentY)
    }

    private fun sharePdf(file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Use the full qualified name to avoid resolution issues inside apply block
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Meeting Recap PDF"))
    }
}
