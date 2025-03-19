package com.example.camerx.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.camera2.internal.annotation.CameraExecutor
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camerx.model.ImageCaptureModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel:ViewModel() {

    private var imageCapture:ImageCapture?=null

    private val _imageCaptureResult=MutableLiveData<ImageCaptureModel>()
    val imageCaptureResult: LiveData <ImageCaptureModel> = _imageCaptureResult

    private lateinit var cameraExecutor: ExecutorService

    init {
        cameraExecutor=Executors.newSingleThreadExecutor()
    }

    fun createImageCapture():ImageCapture{
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build().also { imageCapture = it }
        return imageCapture
    }

    fun captureImage(context: Context){
        val imageCapture=imageCapture?:return

        val name=SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues= ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME,name)
            put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
            //VERSION > 28
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.P){
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CameraX-Images")
            }
        }

        //output File Options
        val outputOptions=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            val contentResolver=context.contentResolver
            val contentUri= MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ImageCapture.OutputFileOptions.Builder(contentResolver, contentUri,contentValues).build()
        }else{
            //For device running Android <Q Version - 29
            val pictureDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val photoFile= File(pictureDir,"${name}.jpg")
            ImageCapture.OutputFileOptions.Builder(photoFile).build()
        }


        viewModelScope.launch {

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback{
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        _imageCaptureResult.value = ImageCaptureModel(imageUri = savedUri)
                    }
                    override fun onError(exc:ImageCaptureException){
                        _imageCaptureResult.value=ImageCaptureModel(
                            error="Photo capture failed:${exc.message}"
                        )
                    }
                }
            )
        }

    }

    override fun onCleared(){
        super.onCleared()
        cameraExecutor.shutdown()
    }
}