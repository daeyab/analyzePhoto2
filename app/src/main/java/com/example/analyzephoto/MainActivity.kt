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
    private val CLOUD_VISION_API_KEY="AIzaSyDhcd_FT8bg6UGS_MCe80swjveZ9fKFX2I"
    private val ANDRIOD_PACKAGE_HEADER="X-Andriod-Package"
    private val ANDRIOD_CERTIFICATION_HEADER="X-Andriod_Cert" //틀린 부분2
    private val MAX_LABEL_RESULTS=10
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            GALLERY_PERMISSION_REQUEST->{
               data?.let{ //data가 null 이 아니라면 let을 실행하는 거와 동일 if(data!=null)이런 느낌
                   uploadImage(it.data)
               }
            }
        }
    }

    private fun uploadImage(imageUri : Uri){
        val bitmap:Bitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageUri) //원리 이해할 필요 ㄴ
        uploaded_image.setImageBitmap(bitmap)
//            uploadChooser?.dismiss()
    }

    private fun requestCloudVisionApi(bitmap: Bitmap){
        val visionTask=ImageRequestTask(this, prepareImageRequest(bitmap))
        visionTask.execute()
    }

    private fun prepareImageRequest(bitmap: Bitmap):Vision.Images.Annotate{
        val httpTransport=AndroidHttp.newCompatibleTransport()
        val jsonFaceAnnotation=GsonFactory.getDefaultInstance()

        val requestInitializer=object : VisionRequestInitializer(CLOUD_VISION_API_KEY){
            override fun initializeVisionRequest(request: VisionRequest<*>?) {
                super.initializeVisionRequest(request)

                val packageName=packageName
                request?.requestHeaders?.set(ANDRIOD_PACKAGE_HEADER,packageName)
                val sig=PackageManagerUtil().getSignature(packageManager,packageName)
                request?.requestHeaders?.set(ANDRIOD_CERTIFICATION_HEADER,sig)
            }
        }
        val builder=Vision.Builder(httpTransport,jsonFaceAnnotation,null) //틀린 부분1
        builder.setVisionRequestInitializer(requestInitializer)
        val vision=builder.build()

        val batchAnnotateImageRequest=BatchAnnotateImagesRequest()
        batchAnnotateImageRequest.requests=object :ArrayList<AnnotateImageRequest>(){
            init{
                val annotateImageRequest=AnnotateImageRequest()
                val base64EncodedImage=Image()
                val byteArrayOutputStream=ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,byteArrayOutputStream)
                val imageBytes=byteArrayOutputStream.toByteArray()

                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image=base64EncodedImage

                annotateImageRequest.features=object :ArrayList<Feature>(){
                    init {
                        val labelDetection=Feature()
                        labelDetection.type="LABEL_DETECTION"
                        labelDetection.maxResults=MAX_LABEL_RESULTS
                        add(labelDetection)
                    }
                }
                add(annotateImageRequest)
            }
        }
        val annotateRequest=vision.images().annotate(batchAnnotateImageRequest)
        annotateRequest.setDisableGZipContent(true)
        return annotateRequest
    }

    inner class ImageRequestTask constructor(activity: MainActivity, val request: Vision.Images.Annotate) : AsyncTask<Any,Void,String>(){
        private val weakReferance:WeakReference<MainActivity>
        init{weakReferance= WeakReference(activity)
        }
        override fun doInBackground(vararg params: Any?): String {
            try {
                val response= request.execute()
                return convertResponseToString(response)
            }
            catch (e:Exception){
                e.printStackTrace()
            }
            return "실패"
        }

        override fun onPostExecute(result: String?) {
            uploaded_image_result.text=result
        }
    }

    private fun convertResponseToString(response:BatchAnnotateImagesResponse):String{
        val message=StringBuilder("분석결과\n")
        val labels=response.responses[0].labelAnnotations
        labels?.let{
            it.forEach{
                message.append(String.format(Locale.US,"%.3f:%s",it.score,it.description))
            }
            return message.toString()
        }
        return "분석 실패"
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


