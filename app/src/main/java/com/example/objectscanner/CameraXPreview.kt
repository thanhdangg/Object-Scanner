package com.example.objectscanner

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class CameraXPreview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val previewView: PreviewView = PreviewView(context, attrs)
    private val imageView: ImageView = ImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    private var cameraProvider: ProcessCameraProvider? = null
    private var onDocumentDetectedListener: ((Bitmap) -> Unit)? = null

    init {
        addView(previewView)
        addView(imageView)
        System.loadLibrary("opencv_java4") // Load the OpenCV library

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

        val rotation = previewView.display.rotation


        val preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalyzer = ImageAnalysis
            .Builder()
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context), DocumentAnalyzer())
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("CameraXPreview", "Use case binding failed", exc)
        }
    }

    fun setOnDocumentDetectedListener(listener: (Bitmap) -> Unit) {
        onDocumentDetectedListener = listener
    }

    private inner class DocumentAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val bitmap = image.toBitmap()
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            detectDocument(mat)
            image.close()
        }

        private fun detectDocument(mat: Mat) {
            // Convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

            // Apply Gaussian blur
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            // Detect edges using Canny
            val edges = Mat()
            Imgproc.Canny(gray, edges, 75.0, 200.0)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            // Find the largest quadrilateral
            var largestQuad: MatOfPoint2f? = null
            for (contour in contours) {
                val approx = MatOfPoint2f()
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
                if (approx.total() == 4L && (largestQuad == null || Imgproc.contourArea(approx) > Imgproc.contourArea(largestQuad))) {
                    largestQuad = approx
                }
            }

            // Draw the quadrilateral
            if (largestQuad != null) {
                val points = largestQuad.toArray()
                for (i in points.indices) {
                    Imgproc.line(mat, points[i], points[(i + 1) % points.size], Scalar(0.0, 255.0, 0.0), 2)
                }
            }

            // Convert back to bitmap and display
            val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, resultBitmap)
            post {
                imageView.setImageBitmap(resultBitmap)
                onDocumentDetectedListener?.invoke(resultBitmap)
            }
        }
    }
}