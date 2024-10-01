package com.example.objectscanner.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.objectscanner.R
import com.example.objectscanner.database.AppDatabase
import com.example.objectscanner.databinding.ActivityPhotoViewBinding
import com.example.objectscanner.models.PhotoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityPhotoView : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewBinding
    private lateinit var database: AppDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        database = AppDatabase.getDatabase(this)


        val photoPath = intent.getStringExtra("photo_path")
        if (photoPath != null) {
            binding.imageView.setImageURI(photoPath.toUri())
            binding.editTextTitle.setText(photoPath.substringAfterLast("/"))
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnSave.setOnClickListener {
            savePhoto()
            val intent = Intent(this, ActivityMain::class.java)
            startActivity(intent)
        }
    }
    private fun savePhoto() {
        val title = binding.editTextTitle.text.toString()
        val imagePath = intent.getStringExtra("photo_path") ?: return
        val timestamp = System.currentTimeMillis()

        val photo = PhotoResult(title = title, imagePath = imagePath, timestamp = timestamp)

        CoroutineScope(Dispatchers.IO).launch {
            database.photoResultDao().insert(photo)
//            finish()
        }
    }
}