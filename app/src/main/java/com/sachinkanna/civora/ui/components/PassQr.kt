package com.sachinkanna.civora.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun PassQr(payload: String,description: String) {
    val bitmap=remember(payload) {
        val matrix=com.google.zxing.MultiFormatWriter().encode(payload,com.google.zxing.BarcodeFormat.QR_CODE,512,512)
        android.graphics.Bitmap.createBitmap(512,512,android.graphics.Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until 512)for(x in 0 until 512)setPixel(x,y,if(matrix[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    Image(bitmap.asImageBitmap(),description,Modifier.size(256.dp))
}
