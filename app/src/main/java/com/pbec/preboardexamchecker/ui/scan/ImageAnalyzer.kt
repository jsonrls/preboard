package com.pbec.preboardexamchecker.ui.scan

import android.content.Context
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pbec.preboardexamchecker.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.features2d.SimpleBlobDetector
import org.opencv.features2d.SimpleBlobDetector_Params
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

@ExperimentalGetImage
class ImageAnalyzer(
    private val viewModel: ScanViewModel,
    private val applicationContext: Context
) : ImageAnalysis.Analyzer {

    private val _expectedRectWidthDp = AtomicReference(0f)
    private val _expectedRectHeightDp = AtomicReference(0f)
    private val _screenWidthPx = AtomicReference(0f)
    private val _cameraPreviewActualHeightPx = AtomicReference(0f)
    private val _density = AtomicReference(0f)
    private val _guideOffsetX_Dp = AtomicReference(0f)
    private val _guideOffsetY_Dp = AtomicReference(0f)
    private val _guideSizeDp = AtomicReference(0f)

    private val analyzerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var blobDetector: SimpleBlobDetector? = null
    private var lastFrameWidth: Int = 0
    private var lastFrameHeight: Int = 0

    private val PYTHON_REF_WIDTH_PX = 2600.0
    private val PYTHON_REF_HEIGHT_PX = 1700.0
    private val PYTHON_BASE_EXPECTED_AREA_PX = 2013.76

    private val DEBUG_SAVE_IMAGES = true
    private var frameCounter = 0

    init {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
            Log.d("ImageAnalyzer", "OpenCV library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("ImageAnalyzer", "Failed to load OpenCV native library: ${e.message}")
        }
    }

    fun updateExpectedRectDimensions(
        expectedRectWidthDp: Float,
        expectedRectHeightDp: Float,
        screenWidthPx: Float,
        cameraPreviewActualHeightPx: Float,
        density: Float,
        guideOffsetX_Dp: Float,
        guideOffsetY_Dp: Float,
        guideSizeDp: Float
    ) {
        _expectedRectWidthDp.set(expectedRectWidthDp)
        _expectedRectHeightDp.set(expectedRectHeightDp)
        _screenWidthPx.set(screenWidthPx)
        _cameraPreviewActualHeightPx.set(cameraPreviewActualHeightPx)
        _density.set(density)
        _guideOffsetX_Dp.set(guideOffsetX_Dp)
        _guideOffsetY_Dp.set(guideOffsetY_Dp)
        _guideSizeDp.set(guideSizeDp)
        Log.d("ImageAnalyzer", "Updated Dimensions - ScreenWidthPx: $screenWidthPx, CameraPreviewHeightPx: $cameraPreviewActualHeightPx, " +
                "ExpectedRectWidthDp: $expectedRectWidthDp, ExpectedRectHeightDp: $expectedRectHeightDp, " +
                "GuideOffsetX_Dp: $guideOffsetX_Dp, GuideOffsetY_Dp: $guideOffsetY_Dp, GuideSizeDp: $guideSizeDp, Density: $density")
    }

    private fun calculateDynamicExpectedMarkArea(currentFrameWidth: Int, currentFrameHeight: Int): Double {
        val scaleFactorWidth = currentFrameWidth / PYTHON_REF_WIDTH_PX
        val scaleFactorHeight = currentFrameHeight / PYTHON_REF_HEIGHT_PX
        val averageScaleFactor = (scaleFactorWidth + scaleFactorHeight) / 2.0
        return PYTHON_BASE_EXPECTED_AREA_PX * averageScaleFactor * averageScaleFactor
    }

    @Suppress("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        Log.d("ImageAnalyzer", "analyze method invoked for frame: ${image.width}x${image.height}")

        analyzerScope.launch {
            val effectivePreviewContentWidthPx = _screenWidthPx.get()
            val effectivePreviewContentHeightPx = _cameraPreviewActualHeightPx.get()
            val density = _density.get()
            val expectedRectWidthDp = _expectedRectWidthDp.get()
            val expectedRectHeightDp = _expectedRectHeightDp.get()
            val guideSizeDp = _guideSizeDp.get()
            val guideOffsetX_Dp = _guideOffsetX_Dp.get()
            val guideOffsetY_Dp = _guideOffsetY_Dp.get()

            if (effectivePreviewContentWidthPx <= 0f || effectivePreviewContentHeightPx <= 0f || density <= 0f ||
                expectedRectWidthDp.isNaN() || expectedRectHeightDp.isNaN() || guideSizeDp.isNaN() ||
                guideOffsetX_Dp.isNaN() || guideOffsetY_Dp.isNaN()) {
                Log.w("ImageAnalyzer", "Skipping frame due to invalid dimensions: ScreenWidthPx=$effectivePreviewContentWidthPx, " +
                        "CameraPreviewHeightPx=$effectivePreviewContentHeightPx, ExpectedRectWidthDp=$expectedRectWidthDp, " +
                        "ExpectedRectHeightDp=$expectedRectHeightDp, GuideSizeDp=$guideSizeDp, GuideOffsetX_Dp=$guideOffsetX_Dp, " +
                        "GuideOffsetY_Dp=$guideOffsetY_Dp, Density=$density")
                image.close()
                return@launch
            }

            val yBuffer = image.planes[0].buffer
            val nv21 = ByteArray(yBuffer.remaining())
            yBuffer.get(nv21)
            val grayMat = Mat(image.height, image.width, CvType.CV_8UC1)
            grayMat.put(0, 0, nv21)

            val scaleFactorX = image.width.toFloat() / effectivePreviewContentWidthPx
            val scaleFactorY = image.height.toFloat() / effectivePreviewContentHeightPx
            Log.d("ImageAnalyzer", "Scale Factors - X: $scaleFactorX, Y: $scaleFactorY, Preview Height: $effectivePreviewContentHeightPx, Frame Height: ${image.height}")

            val guideSizePx_UI = guideSizeDp * density
            val offsetX_Px_UI = guideOffsetX_Dp * density
            val offsetY_Px_UI = guideOffsetY_Dp * density
            val roiSizePx_Cam = (guideSizePx_UI * scaleFactorX).roundToInt()

            val rawRoiRects = mapOf(
                Corner.TOP_LEFT to Rect((offsetX_Px_UI * scaleFactorX).roundToInt(), (offsetY_Px_UI * scaleFactorY).roundToInt(), roiSizePx_Cam, roiSizePx_Cam),
                Corner.TOP_RIGHT to Rect(((offsetX_Px_UI + expectedRectWidthDp * density - guideSizePx_UI) * scaleFactorX).roundToInt(), (offsetY_Px_UI * scaleFactorY).roundToInt(), roiSizePx_Cam, roiSizePx_Cam),
                Corner.BOTTOM_LEFT to Rect((offsetX_Px_UI * scaleFactorX).roundToInt(), ((offsetY_Px_UI + expectedRectHeightDp * density - guideSizePx_UI) * scaleFactorY).roundToInt(), roiSizePx_Cam, roiSizePx_Cam),
                Corner.BOTTOM_RIGHT to Rect(((offsetX_Px_UI + expectedRectWidthDp * density - guideSizePx_UI) * scaleFactorX).roundToInt(), ((offsetY_Px_UI + expectedRectHeightDp * density - guideSizePx_UI) * scaleFactorY).roundToInt(), roiSizePx_Cam, roiSizePx_Cam)
            )

            val roisMap = rawRoiRects.mapValues { createSafeRoi(it.value, image.width, image.height) }

            if (blobDetector == null || image.width != lastFrameWidth || image.height != lastFrameHeight) {
                try {
                    val dynamicExpectedArea = calculateDynamicExpectedMarkArea(image.width, image.height)
                    val params = SimpleBlobDetector_Params()
                    params.set_filterByArea(true)
                    params.set_minArea((dynamicExpectedArea * 0.3).toFloat())
                    params.set_maxArea((dynamicExpectedArea * 4.0).toFloat())
                    params.set_filterByCircularity(true)
                    params.set_minCircularity(0.75f)
                    params.set_filterByConvexity(true)
                    params.set_minConvexity(0.7f)
                    params.set_filterByInertia(true)
                    params.set_minInertiaRatio(0.6f)
                    blobDetector = SimpleBlobDetector.create(params)
                    Log.d("ImageAnalyzer", "Blob detector initialized successfully.")
                } catch (e: UnsatisfiedLinkError) {
                    Log.e("ImageAnalyzer", "Failed to initialize SimpleBlobDetector: ${e.message}")
                    blobDetector = null
                }
                lastFrameWidth = image.width
                lastFrameHeight = image.height
            }

            val detectedCornersThisFrame = mutableSetOf<Corner>()
            roisMap.forEach { (corner, rect) ->
                if (rect != null) {
                    var roiMat: Mat? = null
                    var roiForThresholding: Mat? = null
                    var thresholded: Mat? = null
                    var blurred: Mat? = null
                    try {
                        roiMat = grayMat.submat(rect)
                        val mean = Core.mean(roiMat).`val`[0]
                        Log.d("ImageAnalyzer", "ROI $corner - Mean pixel value: $mean, Rect: $rect")

                        roiForThresholding = Mat()
                        if (mean < 80) {
                            val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0)) // Larger tile grid for better contrast
                            clahe.apply(roiMat, roiForThresholding)
                            Log.d("ImageAnalyzer", "CLAHE applied to ROI $corner.")
                        } else {
                            roiMat.copyTo(roiForThresholding)
                        }

                        blurred = Mat()
                        Imgproc.GaussianBlur(roiForThresholding, blurred, Size(5.0, 5.0), 1.5)
                        thresholded = Mat()
                        val constant = when {
                            mean < 50 -> 2.0
                            mean > 100 -> 8.0
                            else -> 5.0
                        }
                        // Use THRESH_BINARY_INV to detect black marks as white blobs
                        Imgproc.adaptiveThreshold(blurred, thresholded, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, constant)

                        // Increase morphology for thicker borders
                        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0)) // Larger kernel
                        Imgproc.dilate(thresholded, thresholded, kernel, Point(-1.0, -1.0), 2) // Two iterations

                        val keypoints = MatOfKeyPoint()
                        if (blobDetector != null) {
                            blobDetector?.detect(thresholded, keypoints)
                        }
                        val detectedKeypoints = keypoints.toArray()
                        Log.d("BlobDetection", "ROI $corner: Detected ${detectedKeypoints.size} blobs.")

                        var isDetected = false
                        val validKeypoints = if (detectedKeypoints.isNotEmpty() && blobDetector != null) {
                            detectedKeypoints.filter { kp ->
                                val area = kp.size * kp.size * Math.PI
                                val aspectRatio = 1.0
                                area in (0.3 * calculateDynamicExpectedMarkArea(image.width, image.height))..(4.0 * calculateDynamicExpectedMarkArea(image.width, image.height)) &&
                                        aspectRatio in 0.8..1.2
                            }
                        } else emptyList()
                        isDetected = validKeypoints.size == 1

                        if (DEBUG_SAVE_IMAGES) {
                            val debugMat = roiMat.clone()
                            for (kp in detectedKeypoints) {
                                val color = if (isDetected && detectedKeypoints.indexOf(kp) < validKeypoints.size) Scalar(0.0, 255.0, 0.0) else Scalar(0.0, 0.0, 255.0)
                                Imgproc.circle(debugMat, Point(kp.pt.x, kp.pt.y), 5, color, -1)
                            }
                            ImageUtils.saveMatToPng(applicationContext, debugMat, "roi_${corner}_frame${frameCounter}_blobs${detectedKeypoints.size}.png")
                            ImageUtils.saveMatToPng(applicationContext, thresholded, "thresh_roi_${corner}_frame${frameCounter}_blobs${detectedKeypoints.size}.png")
                        }

                        viewModel.updateCornerDetection(corner, isDetected)
                        if (isDetected) {
                            detectedCornersThisFrame.add(corner)
                        }
                    } catch (e: Exception) {
                        Log.e("ImageAnalyzer", "Error processing ROI for $corner: ${e.message}", e)
                        viewModel.updateCornerDetection(corner, false)
                    } finally {
                        roiMat?.release()
                        roiForThresholding?.release()
                        thresholded?.release()
                        blurred?.release()
                    }
                } else {
                    Log.d("ImageAnalyzer", "Skipping processing for $corner because ROI is null or invalid.")
                    viewModel.updateCornerDetection(corner, false)
                }
            }

            frameCounter++
            if (frameCounter % 10 == 0) {
                Log.d("ImageAnalyzer", "Frame $frameCounter: Detected $detectedCornersThisFrame")
            }

            grayMat.release()
            image.close()
        }
    }

    private fun createSafeRoi(originalRect: Rect, imgWidth: Int, imgHeight: Int): Rect? {
        val x = maxOf(0, originalRect.x)
        val y = maxOf(0, originalRect.y)
        val width = minOf(originalRect.width, imgWidth - x)
        val height = minOf(originalRect.height, imgHeight - y)

        if (width <= 0 || height <= 0 || x >= imgWidth || y >= imgHeight) {
            Log.e("createSafeRoi", "Returning null ROI due to invalid dimensions.")
            return null
        }
        return Rect(x, y, width, height)
    }
}