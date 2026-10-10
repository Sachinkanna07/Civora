package com.sachinkanna.civora.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.sachinkanna.civora.data.model.parseTicketPayload
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun TicketScanner(prefix: String="civora",onScanned: (String)->Unit,onClose: ()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    var allowed by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    var error by remember {mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {allowed=it;if(!it)error="Camera permission was declined. Use the ticket code fallback."}
    val preview=remember {PreviewView(context).apply {scaleType=PreviewView.ScaleType.FIT_CENTER}}
    val callback by rememberUpdatedState(onScanned)
    val detected=remember {AtomicBoolean(false)}
    DisposableEffect(allowed,owner) {
        var cameraProvider: ProcessCameraProvider?=null
        val executor=Executors.newSingleThreadExecutor()
        val disposed=AtomicBoolean(false)
        val main=ContextCompat.getMainExecutor(context)
        if(allowed) {
            val future=ProcessCameraProvider.getInstance(context)
            future.addListener({
                if(!disposed.get())try {
                    val provider=future.get();cameraProvider=provider
                    val cameraPreview=Preview.Builder().build().also {it.setSurfaceProvider(preview.surfaceProvider)}
                    val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) {image->
                        try {
                            if(!detected.get()) {
                                val plane=image.planes[0];val buffer=plane.buffer;val bytes=ByteArray(image.width*image.height)
                                for(y in 0 until image.height)for(x in 0 until image.width)bytes[y*image.width+x]=buffer.get(y*plane.rowStride+x*plane.pixelStride)
                                var source: LuminanceSource=PlanarYUVLuminanceSource(bytes,image.width,image.height,0,0,image.width,image.height,false)
                                val reader=MultiFormatReader().apply {setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER to true))}
                                for(rotation in 0..3) {
                                    val decoded=try {reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text}catch(_: NotFoundException) {null}finally {reader.reset()}
                                    if(decoded!=null&&parseTicketPayload(decoded,prefix)!=null&&detected.compareAndSet(false,true)) {main.execute {if(!disposed.get())callback(decoded)};break}
                                    if(source.isRotateSupported)source=source.rotateCounterClockwise()else break
                                }
                            }
                        }catch(_: Exception) { /* Skip incomplete frames; keep scanning. */ }finally {image.close()}
                    }
                    provider.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,cameraPreview,analysis)
                }catch(_: Exception) {error="Camera could not start. Close the scanner and use the ticket code fallback."}
            },main)
        }
        onDispose {disposed.set(true);cameraProvider?.unbindAll();executor.shutdownNow()}
    }
    AlertDialog(onDismissRequest=onClose,title={Text("Scan ${if(prefix=="civora-meal")"meal pass"else"event ticket"}")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Keep the complete QR code inside the camera view. Campus services validate it before check-in.")
        if(allowed)AndroidView(factory={preview},modifier=Modifier.fillMaxWidth().height(300.dp))
        else Button(onClick={permission.launch(Manifest.permission.CAMERA)},modifier=Modifier.heightIn(min=48.dp)) {Text("Allow camera")}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick=onClose) {Text("Close scanner")}})
}
