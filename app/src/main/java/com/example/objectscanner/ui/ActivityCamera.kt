package com.example.objectscanner.ui
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.objectscanner.databinding.ActivityCameraBinding
import java.io.File
import com.example.objectscanner.R

import org.opencv.android.OpenCVLoader;


class ActivityCamera : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private val REQUEST_CAMERA_PERMISSION = 1001
    private var isFlashOn = false
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (OpenCVLoader.initLocal()) {
            Log.i("OpenCV", "OpenCV successfully loaded.");
        } else {
            Log.e("OpenCV", "OpenCV not loaded.");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            startCamera()
        }
        binding.closeButton.setOnClickListener {
            finish()
        }
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }
        binding.ivFlash.setOnClickListener {
            toggleFlash()
        }
        binding.scanButton.setOnClickListener {
            Log.d("ActivityCamera_LOG", "scanButton Scan button clicked")
            takePhoto()
        }
        binding.ivScan.setOnClickListener {
            Log.d("ActivityCamera_LOG", "ivScan Scan button clicked")
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            // Initialize the ImageCapture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                updateFlash()

            } catch (exc: Exception) {
                Log.e("ActivityCamera_LOG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        val imageCapture = imageCapture
        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")
        Log.d("ActivityCamera_LOG", "Photo file path: ${photoFile.absolutePath}")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("ActivityCamera_LOG", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("ActivityCamera_LOG", "Photo capture succeeded: ${photoFile.absolutePath}")
                    val intent = Intent(this@ActivityCamera, ActivityPhotoView::class.java).apply {
                        putExtra("photo_path", photoFile.absolutePath)
                    }
                    startActivity(intent)
                }
            }
        )
    }
    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        updateFlash()
    }

    private fun updateFlash() {
        camera?.cameraControl?.enableTorch(isFlashOn)
        binding.ivFlash.setImageResource(if (isFlashOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startCamera()
            } else {
                // Handle the case where the user denies the permission
            }
        }
    }
}