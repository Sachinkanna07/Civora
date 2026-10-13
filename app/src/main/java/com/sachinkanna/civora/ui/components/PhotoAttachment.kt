package com.sachinkanna.civora.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sachinkanna.civora.data.repository.PhotoRepository
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PhotoAttachment(path: String,pathForPhoto: ()->String,onUploaded: (String)->Unit,onBusy: (Boolean)->Unit={},label: String="Photo (optional)") {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var source by rememberSaveable { mutableStateOf<String?>(null) };var target by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) };var progress by remember { mutableFloatStateOf(0f) };var error by remember { mutableStateOf<String?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val upload: (Uri,Boolean)->Unit={uri,new->
        source=uri.toString();if(new)target=pathForPhoto()
        scope.launch { busy=true;onBusy(true);error=null;progress=0f
            try { onUploaded(PhotoRepository.upload(context,uri,target) { progress=it }) }
            catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(e: Exception) { error=if(e is IllegalArgumentException)e.message else "Photo upload interrupted. Keep this draft and retry." }
            finally { busy=false;onBusy(false) }
        }
    }
    val gallery=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { uri->upload(uri,true) } }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success->if(success)cameraUri?.let { upload(Uri.parse(it),true) } }
    val takePhoto: ()->Unit={
        val dir=File(context.cacheDir,"civora_captures").apply { mkdirs() }
        val file=File.createTempFile("photo_",".jpg",dir)
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.photos",file);cameraUri=uri.toString();camera.launch(uri)
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it)takePhoto()else error="Camera access was declined. You can still choose a photo or submit without one." }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(label,style=MaterialTheme.typography.titleSmall)
        if(path.isNotBlank())PrivateImage(path,"Attached photo")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)takePhoto()else permission.launch(Manifest.permission.CAMERA)},enabled=!busy,modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Icon(Icons.Outlined.PhotoCamera,null);Spacer(Modifier.width(6.dp));Text("Camera") }
            OutlinedButton(onClick={gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!busy,modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Icon(Icons.Outlined.PhotoLibrary,null);Spacer(Modifier.width(6.dp));Text("Gallery") }
        }
        if(busy) { LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth());Text("Uploading photo · ${(progress*100).toInt()}%",style=MaterialTheme.typography.bodySmall) }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={source?.let { uri->upload(Uri.parse(uri),false) }},enabled=!busy&&source!=null) {Text("Retry upload")} }
        Text("Photos are resized and shared only with the people handling this record.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrivateImage(path: String,description: String,modifier: Modifier=Modifier) {
    var failed by remember(path) { mutableStateOf(false) };var retry by remember(path) { mutableIntStateOf(0) }
    val bitmap by produceState<android.graphics.Bitmap?>(null,path,retry) {
        failed=false
        try { val bytes=PhotoRepository.read(path);value=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size);if(value==null)failed=true }
        catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(_: Exception) {failed=true}
    }
    if(bitmap!=null) Image(bitmap!!.asImageBitmap(),description,modifier.fillMaxWidth().heightIn(min=120.dp,max=220.dp),contentScale=ContentScale.Fit)
    else if(failed) TextButton(onClick={retry++}) {Text("Photo unavailable · Retry")}
    else LinearProgressIndicator(modifier.fillMaxWidth())
}
