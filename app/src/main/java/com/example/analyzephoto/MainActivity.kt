package com.example.analyzephoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {


    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupListener()
    }

    private fun setupListener() {
        uploadImage.setOnClickListener {
            UploadChooser().apply {
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
            }.show(supportFragmentManager, "")
        }
    }

    private fun checkCameraPermission() {
        if (PermissionUtil().requestPermission(this, CAMERA_PERMISSION_REQUEST, android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            openCamera()
        }
    }

    private fun checkGalleryPermission() {
        PermissionUtil().requestPermission(this, GALLERY_PERMISSION_REQUEST, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    }

    private fun openCamera() {
        //인텐트를 통해 열어준다 uri : 경로를 의미
        //내가 카메라로 찍은 사진이 저장될 위치
        val photoUri = FileProvider.getUriForFile(this,applicationContext.packageName + ".provider", createCameraFile())

        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, CAMERA_PERMISSION_REQUEST)

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
            }
            CAMERA_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode,CAMERA_PERMISSION_REQUEST,grantResults)){
                    openCamera()
                }
            }
        }
    }
}


