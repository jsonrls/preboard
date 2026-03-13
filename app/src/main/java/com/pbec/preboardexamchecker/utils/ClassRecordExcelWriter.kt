package com.pbec.preboardexamchecker.utils

import android.content.Context
import android.net.Uri
import com.pbec.preboardexamchecker.ui.scan.ScanEvaluationResult
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClassRecordExcelWriter {

    private val headers = listOf(
        "Scanned At",
        "Student ID",
        "Exam Name",
        "Subject",
        "Score",
        "Total Items",
        "Percentage",
        "Answers"
    )

    fun appendResult(context: Context, uri: Uri, result: ScanEvaluationResult) {
        val workbook = context.contentResolver.openInputStream(uri)?.use { input ->
            XSSFWorkbook(input)
        } ?: XSSFWorkbook()

        val sheet = if (workbook.numberOfSheets > 0) {
            workbook.getSheetAt(0)
        } else {
            workbook.createSheet("Class Record")
        }

        if (sheet.physicalNumberOfRows == 0) {
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }
        }

        val nextRowIndex = (sheet.lastRowNum + 1).coerceAtLeast(1)
        val row = sheet.createRow(nextRowIndex)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        row.createCell(0).setCellValue(now)
        row.createCell(1).setCellValue(result.studentId)
        row.createCell(2).setCellValue(result.examName)
        row.createCell(3).setCellValue(result.subject)
        row.createCell(4).setCellValue(result.correctCount.toDouble())
        row.createCell(5).setCellValue(result.totalItems.toDouble())
        row.createCell(6).setCellValue(String.format(Locale.US, "%.2f", result.scorePercent))
        row.createCell(7).setCellValue(result.scannedAnswers)

        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            workbook.write(output)
            output.flush()
        } ?: throw IllegalStateException("Unable to open output stream for selected class record.")

        workbook.close()
    }
}
