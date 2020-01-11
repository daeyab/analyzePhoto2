package com.example.analyzephoto

import android.app.Activity
import android.content.Intent
import android.content.Intent.createChooser
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import androidx.core.content.FileProvider
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.security.Permission
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList
import  com.fasterxml.jackson.core.JsonFactory


class MainActivity : AppCompatActivity() {


    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser:UploadChooser?=null
    private var labelDetectionTask:LabelDetectionTask?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        labelDetectionTask=LabelDetectionTask(packageName,packageManager,this)
        setupListener()
    }

    private fun setupListener() {
        uploadImage.setOnClickListener {
            uploadChooser=UploadChooser().apply {
                // apply는 앞에 있는 부분을 초기설정하는 것
                addNotifier(object : UploadChooser.UploadChooserNotifierInterface {
                    override fun CamaraOnClick() { // 버튼을 클릭하였을 때 실행될 작업들
                        Log.d("upload", "카메라")
                        //카메라 권한 요청 시작
                        checkCameraPermission()
                    }

                    override fun GalleryOnClick() {
                        Log.d("upload", "갤러리")
                        //갤러리 권한 요청 시작
                        checkGalleryPermission()
                    }
                })
            }
            uploadChooser!!.show(supportFragmentManager,"") //nullable 한 값이니까 강제 수행
        }
    }

    private fun checkCameraPermission() {
        if (PermissionUtil().requestPermission(this, CAMERA_PERMISSION_REQUEST, android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            openCamera()
        }
    }

    private fun checkGalleryPermission() {
        if (PermissionUtil().requestPermission(this, GALLERY_PERMISSION_REQUEST, android.Manifest.permission.READ_EXTERNAL_STORAGE))
            openGallery()
    }

    private fun openGallery(){
        val intent=Intent().apply{
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        startActivityForResult(Intent.createChooser(intent,"Select Photo"),GALLERY_PERMISSION_REQUEST)
    }

    private fun openCamera() {
        //인텐트를 통해 열어준다 uri : 경로를 의미
        //내가 카메라로 찍은 사진이 저장될 위치
        val photoUri = FileProvider.getUriForFile(this,applicationContext.packageName + ".provider", createCameraFile())//저장할 경로

        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, CAMERA_PERMISSION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        when(requestCode) {
            CAMERA_PERMISSION_REQUEST->{
                if(resultCode!= Activity.RESULT_OK) return
                val photoUri= FileProvider.getUriForFile(this,packageName+ ".provider",createCameraFile())//저장된 경로
                uploadImage(photoUri)
            }
            GALLERY_PERMISSION_REQUEST -> data?.let { uploadImage(it.data) }

        }
    }

    private fun uploadImage(imageUri : Uri){
        val bitmap:Bitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageUri) //원리 이해할 필요 ㄴ
        uploaded_image.setImageBitmap(bitmap)
        uploadChooser?.dismissAllowingStateLoss()
        requestCloudVisionApi(bitmap)
    }

    private fun requestCloudVisionApi(bitmap: Bitmap) {
        labelDetectionTask?.requestCloudVisionApi(bitmap,object:LabelDetectionTask.LabelDetectionNotifierInterface{
            override fun notifyResult(result: String) {
               uploaded_image_result.text=result
            }
        })
    }


    private fun createCameraFile(): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GALLERY_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode,GALLERY_PERMISSION_REQUEST,grantResults)){
                    //여기서 갤러리를 열어야해
                    openGallery()
                }
            }
            CAMERA_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode,CAMERA_PERMISSION_REQUEST,grantResults)){
                    openCamera()
                }
            }
        }
    }
}