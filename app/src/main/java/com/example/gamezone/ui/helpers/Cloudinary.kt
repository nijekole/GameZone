package com.example.gamezone.ui.helpers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryUploader {

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "dglgy66ub",
                "api_key" to "531887257777869",
                "api_secret" to "7rHQ7F5rvovBAVZ1B6sQOe1l9WU"
            )
            MediaManager.init(context, config)
            isInitialized = true
            Log.d("CloudinaryUploader", "Cloudinary initialized")
        }
    }

    suspend fun uploadImage(imageUri: Uri, publicId: String): String {

        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(imageUri)
                .unsigned("profilneslike")
                .option("public_id", publicId)
                .option("folder", "profile_images")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("CloudinaryUploader", "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        Log.d("CloudinaryUploader", "Upload progress: $bytes/$totalBytes")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        Log.d("CloudinaryUploader", "Upload successful! URL: $url")
                        if (url != null) {
                            continuation.resume(url)
                        } else {
                            continuation.resumeWithException(Exception("URL not found in response"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("CloudinaryUploader", "Upload failed: ${error.description}")
                        continuation.resumeWithException(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("CloudinaryUploader", "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        }
    }
}