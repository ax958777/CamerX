package com.example.camerx.model

import android.net.Uri
import java.io.File

data class ImageCaptureModel(
    val imageUri:Uri?=null,
    val file: File?=null,
    val error:String?=null
)
