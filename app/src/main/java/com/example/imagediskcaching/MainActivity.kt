package com.example.imagediskcaching

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val imageUrls = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        imageAdapter = ImageAdapter(this,imageUrls)
        recyclerView.adapter = imageAdapter
        fetchImages()
    }
    private fun fetchImages() {
        val url =
            "https://acharyaprashant.org/api/v2/content/misc/media-coverages?limit=100" // Replace with your API endpoint
        val client = OkHttpClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val imagesArray = JSONArray(responseBody)
                for (i in 0 until imagesArray.length()) {
                    val thumbnail = imagesArray.getJSONObject(i).getJSONObject("thumbnail")
                    val domain = thumbnail.getString("domain")
                    val basePath = thumbnail.getString("basePath")
                    val key = thumbnail.getString("key")
                    val imageUrl = "$domain/$basePath/0/$key"
                    Log.e("IMAGES","ImageURL:-${imageUrl}")
                    imageUrls.add(imageUrl)
                }

                withContext(Dispatchers.Main) {
                    imageAdapter.notifyDataSetChanged()
                }
            } catch (e: IOException) {
                // Handle network error
            }
        }
    }
}