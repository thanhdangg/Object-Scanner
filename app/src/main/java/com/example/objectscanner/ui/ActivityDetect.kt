package com.example.objectscanner.ui
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.objectscanner.R
import com.example.objectscanner.databinding.ActivityDetectBinding
import com.example.objectscanner.ui.ActivityPhotoView
import com.example.objectscanner.ui.custom.CameraXPreview
import com.example.objectscanner.ui.custom.DocumentScannerView
import com.example.objectscanner.utils.OpenCVUtils.saveBitmapToFile
import java.io.File

class ActivityDetect : AppCompatActivity() {

    private lateinit var binding: ActivityDetectBinding
    private val REQUEST_CAMERA_PERMISSION = 1001
    private var isFlashOn = false
    private var detectedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            showDetectedDocument()
        }
        binding.ivScan.setOnClickListener {
            showDetectedDocument()
        }
    }

    private fun startCamera() {
        val documentScannerView = findViewById<DocumentScannerView>(R.id.documentScannerView)
        documentScannerView.startCamera(this)
        documentScannerView.setOnDocumentDetectedListener { bitmap ->
            detectedBitmap = bitmap
        }
    }

    private fun showDetectedDocument() {
        detectedBitmap?.let { bitmap ->
            val file = File(getExternalFilesDir(null), "detected_document.png")
            if (saveBitmapToFile(bitmap, file)) {
                val intent = Intent(this, ActivityPhotoView::class.java).apply {
                    putExtra("photo_path", file.absolutePath)
                }
                startActivity(intent)
            }
        }
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        updateFlash()
    }

    private fun updateFlash() {
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