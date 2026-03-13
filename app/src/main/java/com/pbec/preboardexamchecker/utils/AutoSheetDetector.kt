package com.pbec.preboardexamchecker.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class AutoDetectedSheet(
    val studentId: String,
    val answers: List<Char?>,
    val averageConfidence: Double
)

object AutoSheetDetector {

    fun detect(bitmap: Bitmap, totalItems: Int): AutoDetectedSheet {
        val source = Mat()
        Utils.bitmapToMat(bitmap, source)

        val gray = Mat()
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGBA2GRAY)

        val normalized = Mat()
        Imgproc.GaussianBlur(gray, normalized, org.opencv.core.Size(5.0, 5.0), 0.0)

        val binaryInv = Mat()
        Imgproc.adaptiveThreshold(
            normalized,
            binaryInv,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            31,
            6.0
        )

        val studentIdResult = detectStudentId(binaryInv)
        val answerResult = detectAnswers(binaryInv, totalItems)

        source.release()
        gray.release()
        normalized.release()
        binaryInv.release()

        val confidence = (studentIdResult.second + answerResult.second) / 2.0
        return AutoDetectedSheet(
            studentId = studentIdResult.first,
            answers = answerResult.first,
            averageConfidence = confidence
        )
    }

    private fun detectStudentId(binaryInv: Mat): Pair<String, Double> {
        // Tunable normalized region for a standard answer-sheet ID bubble grid.
        val region = normalizedRect(binaryInv, 0.07, 0.06, 0.42, 0.16)
        val cols = 10
        val rows = 10
        val cellW = max(1, region.width / cols)
        val cellH = max(1, region.height / rows)

        val digits = StringBuilder()
        var confidenceSum = 0.0

        for (col in 0 until cols) {
            val scores = mutableListOf<Double>()
            for (row in 0 until rows) {
                val cell = safeRect(
                    binaryInv,
                    region.x + col * cellW,
                    region.y + row * cellH,
                    (cellW * 0.9).toInt(),
                    (cellH * 0.9).toInt()
                ) ?: continue
                scores += fillRatio(binaryInv, cell)
            }

            if (scores.isEmpty()) continue

            val sorted = scores.withIndex().sortedByDescending { it.value }
            val best = sorted.first()
            val second = sorted.getOrNull(1)
            val margin = if (second != null) best.value - second.value else best.value
            confidenceSum += margin.coerceIn(0.0, 1.0)
            digits.append(best.index.toString())
        }

        val id = digits.toString().trimStart('0').ifBlank { digits.toString() }
        val confidence = if (cols == 0) 0.0 else confidenceSum / cols.toDouble()
        return id to confidence
    }

    private fun detectAnswers(binaryInv: Mat, totalItems: Int): Pair<List<Char?>, Double> {
        val region = normalizedRect(binaryInv, 0.07, 0.24, 0.86, 0.70)
        val questionColumns = 4
        val choices = charArrayOf('A', 'B', 'C', 'D')
        val questionsPerColumn = max(1, ceil(totalItems / questionColumns.toDouble()).toInt())

        val colW = max(1, region.width / questionColumns)
        val rowH = max(1, region.height / questionsPerColumn)

        val detected = MutableList<Char?>(totalItems) { null }
        var confidenceSum = 0.0
        var confidenceCount = 0

        for (q in 0 until totalItems) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn
            if (col >= questionColumns) break

            val qRect = safeRect(
                binaryInv,
                region.x + col * colW,
                region.y + row * rowH,
                colW,
                rowH
            ) ?: continue

            val optionScores = mutableListOf<Double>()
            for (optionIndex in 0 until 4) {
                val optX = qRect.x + ((optionIndex + 0.5) * qRect.width / 4.0).toInt()
                val boxSize = min((qRect.height * 0.7).toInt(), (qRect.width * 0.20).toInt()).coerceAtLeast(4)
                val cell = safeRect(
                    binaryInv,
                    optX - boxSize / 2,
                    qRect.y + (qRect.height * 0.15).toInt(),
                    boxSize,
                    boxSize
                )
                optionScores += if (cell != null) fillRatio(binaryInv, cell) else 0.0
            }

            val sorted = optionScores.withIndex().sortedByDescending { it.value }
            val best = sorted.firstOrNull()
            val second = sorted.getOrNull(1)
            if (best != null) {
                val margin = if (second != null) best.value - second.value else best.value
                val darkEnough = best.value >= 0.08
                val confidentEnough = margin >= 0.02
                if (darkEnough && confidentEnough) {
                    detected[q] = choices[best.index]
                }
                confidenceSum += margin.coerceIn(0.0, 1.0)
                confidenceCount++
            }
        }

        val confidence = if (confidenceCount == 0) 0.0 else confidenceSum / confidenceCount.toDouble()
        return detected to confidence
    }

    private fun fillRatio(binaryInv: Mat, rect: Rect): Double {
        val roi = binaryInv.submat(rect)
        val nonZero = org.opencv.core.Core.countNonZero(roi).toDouble()
        val total = (roi.rows() * roi.cols()).toDouble().coerceAtLeast(1.0)
        roi.release()
        return nonZero / total
    }

    private fun normalizedRect(mat: Mat, x: Double, y: Double, w: Double, h: Double): Rect {
        val px = (mat.width() * x).toInt()
        val py = (mat.height() * y).toInt()
        val pw = (mat.width() * w).toInt()
        val ph = (mat.height() * h).toInt()
        return safeRect(mat, px, py, pw, ph) ?: Rect(0, 0, mat.width(), mat.height())
    }

    private fun safeRect(mat: Mat, x: Int, y: Int, w: Int, h: Int): Rect? {
        val sx = max(0, x)
        val sy = max(0, y)
        val sw = min(w, mat.width() - sx)
        val sh = min(h, mat.height() - sy)
        if (sw <= 1 || sh <= 1) return null
        return Rect(sx, sy, sw, sh)
    }
}
