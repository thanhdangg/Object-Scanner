package com.example.objectscanner.ui.custom

import android.content.Context
import android.graphics.Bitmap
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
import com.example.objectscanner.algorithm.CoreAlgorithm
import org.opencv.core.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DetectView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), CoreAlgorithm.OpenCVResultCallback {

    private val previewView: PreviewView = PreviewView(context, attrs)
    private val overlayView: OverlayView = OverlayView(context)
    private var cameraProvider: ProcessCameraProvider? = null
    private var onDocumentDetectedListener: ((Bitmap) -> Unit)? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val coreAlgorithm: CoreAlgorithm = CoreAlgorithm(this)

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

    override fun onDocumentRectResult(rect: Rect) {
        val points = listOf(
            Point(rect.x.toDouble(), rect.y.toDouble()),
            Point((rect.x + rect.width).toDouble(), rect.y.toDouble()),
            Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
            Point(rect.x.toDouble(), (rect.y + rect.height).toDouble())
        )
        overlayView.setQuadPoints(points.toTypedArray())
    }

    override fun onBinarizeDocResult(binImage: Bitmap) {
        onDocumentDetectedListener?.invoke(binImage)
    }

    private inner class DocumentAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val bitmap = imageProxy.toBitmap()
            coreAlgorithm.getDocumentRect(bitmap)
            imageProxy.close()
        }
    }
}