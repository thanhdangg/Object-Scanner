package com.example.objectscanner.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.objectscanner.R
import com.example.objectscanner.database.AppDatabase
import com.example.objectscanner.databinding.ActivityMainBinding
import com.example.objectscanner.models.PhotoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityMain : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var database: AppDatabase
    private lateinit var photoResultAdapter: PhotoResultAdapter
    private var photoResults: List<PhotoResult> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)
        photoResultAdapter = PhotoResultAdapter()

        binding.rvResult.layoutManager = LinearLayoutManager(this)
        binding.rvResult.adapter = photoResultAdapter

        loadPhotoResults()


        binding.ivScan.setOnClickListener {
//            intent = Intent(this, ActivityCamera::class.java)
            intent = Intent(this, ActivityDetect::class.java)
            startActivity(intent)
        }
    }

    private fun loadPhotoResults() {
        CoroutineScope(Dispatchers.IO).launch {
            photoResults = database.photoResultDao().getAllPhotos()
            withContext(Dispatchers.Main) {
                photoResultAdapter.submitList(photoResults)
            }
            Log.d("ActivityMain", "Photo results: $photoResults")
        }
    }
}