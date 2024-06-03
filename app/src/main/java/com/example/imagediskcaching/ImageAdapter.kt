package com.example.imagediskcaching

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ImageAdapter(private val context: Context, private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val memoryCache: LruCache<String, Bitmap>
    private val diskCacheDir: File
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val loadingJobs = mutableMapOf<Int, Job>()

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        diskCacheDir = File(context.cacheDir, "images")
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        holder.imageView.setImageResource(R.drawable.ic_loading_icon) // Set a placeholder image
        holder.imageView.tag = imageUrl

        loadingJobs[position]?.cancel()
        loadingJobs[position] = scope.launch {
            val bitmap = loadBitmap(imageUrl)
            withContext(Dispatchers.Main) {
                if (holder.imageView.tag == imageUrl) {
                    holder.imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun getItemCount(): Int = imageUrls.size

    private suspend fun loadBitmap(imageUrl: String): Bitmap? {
        memoryCache.get(imageUrl)?.let { return it }

        val file = File(diskCacheDir, imageUrl.hashCode().toString())
        if (file.exists()) {
            Log.e("IMAGES", "LOAD FROM CASH")
            BitmapFactory.decodeFile(file.path)?.let { bitmap ->
                memoryCache.put(imageUrl, bitmap)
                return bitmap
            }
        }

        return try {
            Log.e("IMAGES", "LOAD FROM NETWORK")
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            memoryCache.put(imageUrl, bitmap)
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            bitmap
        } catch (e: IOException) {
            null
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

}

