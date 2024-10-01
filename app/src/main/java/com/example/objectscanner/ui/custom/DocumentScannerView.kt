package com.example.objectscanner.ui.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DocumentScannerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val previewView: PreviewView = PreviewView(context, attrs)
    private val overlayView: OverlayView = OverlayView(context)
    private var cameraProvider: ProcessCameraProvider? = null
    private var onDocumentDetectedListener: ((Bitmap) -> Unit)? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        addView(previewView)
        addView(overlayView)
        System.loadLibrary("opencv_java4")
    }

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(analysisExecutor, DocumentAnalyzer())
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
        } catch (exc: Exception) {
            Log.e("DocumentScannerView", "Use case binding failed", exc)
            exc.printStackTrace()
        }
    }

    fun setOnDocumentDetectedListener(listener: (Bitmap) -> Unit) {
        onDocumentDetectedListener = listener
    }

    private inner class DocumentAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val bitmap = imageProxy.toBitmap()
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            val detectedQuad = detectDocument(mat)

            detectedQuad?.let {
                val opencvQuadPoints = it.map { point -> Point(point.x.toDouble(), point.y.toDouble()) }.toTypedArray()
                overlayView.setQuadPoints(opencvQuadPoints)

                // Apply perspective transform
                val transformedBitmap = applyPerspectiveTransform(mat, it)
                onDocumentDetectedListener?.invoke(transformedBitmap)
            }
            bitmap.recycle()  // Avoid memory leaks
            imageProxy.close()
        }

        private fun detectDocument(inputMat: Mat): List<Point>? {
            // Convert the input image to grayscale
            val gray = Mat()
            Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)

            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            // Perform adaptive thresholding to enhance edges
            Imgproc.adaptiveThreshold(gray, gray, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0)

            val edges = Mat()
            Imgproc.Canny(gray, edges, 30.0, 100.0)

//            val contours = mutableListOf<MatOfPoint>()
//            val hierarchy = Mat()
//            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//            contours.sortByDescending { Imgproc.contourArea(it) }
//
//            var largestQuad: List<Point>? = null
//            for (contour in contours) {
//                val approx = MatOfPoint2f()
//                val contour2f = MatOfPoint2f(*contour.toArray())
//                val peri = Imgproc.arcLength(contour2f, true)
//
//                Imgproc.drawContours(inputMat, listOf(contour), -1, Scalar(255.0, 0.0, 0.0), 3)
//
//                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
//
////                if (approx.total() == 4L && Imgproc.isContourConvex(MatOfPoint(*approx.toArray()))) {
////                    val points = approx.toList().map { point -> Point(point.x.toInt(), point.y.toInt()) }
////                    if (largestQuad == null || Imgproc.contourArea(approx) > Imgproc.contourArea(MatOfPoint2f(*largestQuad.map { point -> Point(point.x.toDouble(), point.y.toDouble()) }.toTypedArray()))) {
////                        largestQuad = points
////                    }
////                }
//                if (approx.toArray().size == 4) {
//                    val points = approx.toList().map { point -> Point(point.x.toInt(), point.y.toInt()) }
//                    largestQuad = points
//                }
//            }
//            return largestQuad
            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            // Iterate through the contours and find the largest one
            var largestContour: MatOfPoint? = null
            var largestContourArea = 0.0
            for (contour in contours) {
                val contourArea = Imgproc.contourArea(contour)
                if (contourArea > largestContourArea) {
                    largestContour = contour
                    largestContourArea = contourArea
                }
            }

            // If a suitable contour was found, approximate its shape to a rectangle
            if (largestContour != null) {
                val approx = MatOfPoint2f()
                val largestContour2f = MatOfPoint2f(*largestContour.toArray())
                Imgproc.approxPolyDP(largestContour2f, approx, 0.02 * Imgproc.arcLength(largestContour2f, true), true)

                // Check if the approximated shape has four vertices
                if (approx.size().height == 4.0 && Imgproc.isContourConvex(MatOfPoint(*approx.toArray()))) {
                    val documentCorners = ArrayList<Point>()
                    for (point in approx.toArray()) {
                        documentCorners.add(Point(point.x.toInt(), point.y.toInt()))
                    }
                    return documentCorners
                }
            }

            // If no suitable contour or shape was found, return null
            return null
        }

        private fun applyPerspectiveTransform(inputMat: Mat, points: List<Point>): Bitmap {
            val srcPoints = MatOfPoint2f(*points.map { point -> Point(point.x.toDouble(), point.y.toDouble()) }.toTypedArray())
            val dstPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(inputMat.cols().toDouble(), 0.0),
                Point(inputMat.cols().toDouble(), inputMat.rows().toDouble()),
                Point(0.0, inputMat.rows().toDouble())
            )

            val perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
            val outputMat = Mat()
            Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, Size(inputMat.cols().toDouble(), inputMat.rows().toDouble()))

            val outputBitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(outputMat, outputBitmap)

            return outputBitmap
        }
    }
}
