package com.sachinkanna.civora.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object PhotoRepository {
    private const val MAX_BYTES=3L*1024*1024
    suspend fun upload(context: Context,uri: Uri,path: String,progress: (Float)->Unit): String {
        val authorizedPath=(com.google.firebase.functions.FirebaseFunctions.getInstance().getHttpsCallable("authorizePhotoUpload").call(mapOf("path" to path)).await().getData() as? Map<*,*>)?.get("path") as? String
            ?: error("Photo upload was not authorized.")
        require(authorizedPath==path) { "Photo destination changed. Choose the photo again." }
        val bytes=withContext(Dispatchers.IO) {
            val bitmap=if(Build.VERSION.SDK_INT>=28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)) { decoder,info,_->
                val ratio=maxOf(info.size.width,info.size.height)/1600f
                if(ratio>1)decoder.setTargetSize((info.size.width/ratio).toInt().coerceAtLeast(1),(info.size.height/ratio).toInt().coerceAtLeast(1))
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            } else {
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,bounds) }
                require(bounds.outWidth>0&&bounds.outHeight>0) { "Choose a readable photo." }
                val options=BitmapFactory.Options();while(bounds.outWidth/options.inSampleSize.coerceAtLeast(1)>1600||bounds.outHeight/options.inSampleSize.coerceAtLeast(1)>1600)options.inSampleSize=options.inSampleSize.coerceAtLeast(1)*2
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,options) } ?: error("Photo could not be opened")
            }
            val output=ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,82,output);bitmap.recycle();output.toByteArray().also { require(it.size in 1..MAX_BYTES.toInt()) { "Choose a smaller photo (up to 3 MB)." } }
        }
        val objectRef=FirebaseStorage.getInstance().reference.child(path)
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        try { val metadata=objectRef.metadata.await();if(metadata.getCustomMetadata("sha256")==digest)return path;error("Choose the photo again to create a fresh upload.") }
        catch(e: StorageException) { if(e.errorCode!=StorageException.ERROR_OBJECT_NOT_FOUND)throw e }
        val task=objectRef.putBytes(bytes,StorageMetadata.Builder().setContentType("image/jpeg").setCustomMetadata("sha256",digest).build())
        task.addOnProgressListener { progress((it.bytesTransferred.toFloat()/it.totalByteCount.coerceAtLeast(1)).coerceIn(0f,1f)) }
        task.await();return path
    }
    suspend fun read(path: String): ByteArray=FirebaseStorage.getInstance().reference.child(path).getBytes(MAX_BYTES).await()
}
