package com.example.pawtytime

import android.content.Context
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class CloudinaryUploader(
    private val context: Context,
    private val cloudName: String = "dk8e8o6vc",
    private val uploadPreset: String = "pawty_unsigned"
) {
    private val client = OkHttpClient()


    fun upload(uri: Uri, onDone: (String?) -> Unit) {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        if (bytes == null) {
            onDone(null)
            return
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart(
                "file",
                "upload.jpg",
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), bytes)
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onDone(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = it.body?.string().orEmpty()
                    val regex = Regex("\"secure_url\"\\s*:\\s*\"([^\"]+)\"")
                    val url = regex.find(json)?.groupValues?.getOrNull(1)
                    onDone(url)
                }
            }
        })
    }
}