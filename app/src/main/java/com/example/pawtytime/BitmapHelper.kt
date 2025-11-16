package com.example.pawtytime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object BitmapHelper {


    fun vectorToBitmap(
        context: Context,
        resId: Int,
        sizeDp: Float = 32f
    ): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, resId)!!

        val metrics = context.resources.displayMetrics
        val width = (sizeDp * metrics.density).toInt()
        val height = (sizeDp * metrics.density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}