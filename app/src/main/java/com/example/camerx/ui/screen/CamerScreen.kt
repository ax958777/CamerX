package com.example.camerx.ui.screen

import android.Manifest
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.camerx.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context= LocalContext.current
    val captureResult by viewModel.imageCaptureResult.observeAsState()
    LaunchedEffect(key1=Unit){
        if(!cameraPermissionState.status.isGranted){
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()){
        if(cameraPermissionState.status.isGranted){
            CameraPreview(
                modifier=Modifier.fillMaxSize(),
                viewModel=viewModel
            )

            IconButton(
              modifier=Modifier
                  .align(Alignment.BottomCenter)
                  .padding(bottom = 32.dp)
                  .size(72.dp)
                  .border(2.dp, Color.White, CircleShape),
                onClick = {viewModel.captureImage(context)}
            ){
                Icon(
                    imageVector=Icons.Default.Camera,
                    contentDescription = "Take photo",
                    tint=Color.White,
                    modifier=Modifier.size(36.dp)
                )
            }

            captureResult?.imageUri?.let { uri->
                AsyncImage(
                    model=uri,
                    contentDescription = "Captured image",
                    modifier=Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(80.dp)
                        .border(2.dp,Color.White)
                )
            }
        }
        else{
            Column(
                modifier=Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required")
                Button(
                    onClick = {cameraPermissionState.launchPermissionRequest()}
                ,modifier=Modifier.padding(16.dp)
                ){
                    Text("Request Permission")
                }
            }
        }
    }

}

@Composable
fun CameraPreview(
    modifier: Modifier=Modifier,
    viewModel: CameraViewModel
){
    val context = LocalContext.current
    val cameraSelector = remember{CameraSelector.DEFAULT_BACK_CAMERA}
    val lifecycleOwner= LocalLifecycleOwner.current
    val preview= remember { Preview.Builder().build() }
    val previewView=remember{PreviewView(context)}
    LaunchedEffect(key1 = cameraSelector) {
        val cameraProvider = context.getCameraProvider()

        val imageCapture=viewModel.createImageCapture()

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
    AndroidView(
        factory = {previewView},
        modifier=modifier
    )

}

suspend fun Context.getCameraProvider():ProcessCameraProvider= suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also{future->
        future.addListener({
            continuation.resume(future.get())
        },ContextCompat.getMainExecutor(this))
    }
}