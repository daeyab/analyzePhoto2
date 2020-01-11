package com.example.analyzephoto

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.widget.AutoCompleteTextView
import android.widget.TextView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class LabelDetectionTask(private val packageName:String, private val packageManager: PackageManager, private val activity: MainActivity) {
    //q비트맵을 받아서 해당 비트맵 요청,응답 받아오기


    private val CLOUD_VISION_API_KEY="AIzaSyCW2mDIQKPYAk8YQd6PpIwM-55G311xOC4"
    private val ANDRIOD_PACKAGE_HEADER="X-Andriod-Package"
    private val ANDRIOD_CERTIFICATION_HEADER="X-Andriod_Cert" //틀린 부분2
    private val MAX_RESULT=10
    private var labelDetectionNotifierInterface :LabelDetectionNotifierInterface?=null


    interface LabelDetectionNotifierInterface{
        fun notifyResult(result:String)
    }
    fun requestCloudVisionApi(bitmap: Bitmap, labelDetectionNotifierInterface :LabelDetectionNotifierInterface){

       this.labelDetectionNotifierInterface=labelDetectionNotifierInterface
        val visionTask=ImageRequestTask(prepareImageRequest(bitmap))
        visionTask.execute()
    }

    private fun prepareImageRequest(bitmap: Bitmap): Vision.Images.Annotate{
        val httpTransport= AndroidHttp.newCompatibleTransport()
        val jsonFaceAnnotation= GsonFactory.getDefaultInstance()

        val requestInitializer=object : VisionRequestInitializer(CLOUD_VISION_API_KEY){
            override fun initializeVisionRequest(request: VisionRequest<*>?) {
                super.initializeVisionRequest(request)

                val packageName=packageName
                request?.requestHeaders?.set(ANDRIOD_PACKAGE_HEADER,packageName)
                val sig=PackageManagerUtil().getSignature(packageManager,packageName)
                request?.requestHeaders?.set(ANDRIOD_CERTIFICATION_HEADER,sig)
            }
        }
        val builder= Vision.Builder(httpTransport,jsonFaceAnnotation,null) //틀린 부분1
        builder.setVisionRequestInitializer(requestInitializer)
        val vision=builder.build()

        val batchAnnotateImageRequest= BatchAnnotateImagesRequest()
        batchAnnotateImageRequest.requests=object :ArrayList<AnnotateImageRequest>(){
            init{
                val annotateImageRequest= AnnotateImageRequest()
                val base64EncodedImage= Image()
                val byteArrayOutputStream= ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,byteArrayOutputStream)
                val imageBytes=byteArrayOutputStream.toByteArray()

                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image=base64EncodedImage

                annotateImageRequest.features=object :ArrayList<Feature>(){
                    init {
                        val labelDetection= Feature()
                        labelDetection.type="LABEL_DETECTION"
                        labelDetection.maxResults=MAX_RESULT
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


    private fun convertResponseToString(response: BatchAnnotateImagesResponse):String{
        val message=StringBuilder("분석 결과\n")
        val labels=response.responses[0].labelAnnotations
        labels?.let{
            it.forEach{
                message.append(String.format(Locale.US,"%.3f:%s",it.score,it.description))
                message.append("\n")
            }
            return message.toString()
        }
        return "convertResponseToString 분석 실패"
    }



    inner class ImageRequestTask constructor(val request: Vision.Images.Annotate) : AsyncTask<Any, Void, String>(){
        private val weakReference: WeakReference<MainActivity>
        init{weakReference= WeakReference(activity)
        }
        override fun doInBackground(vararg params: Any?): String {
            try {
                val response= request.execute()
                return convertResponseToString(response)
            }
            catch (e:Exception){
                e.printStackTrace()
            }
            return "doInBackground 분석 실패"
        }

        override fun onPostExecute(result: String?) {
            result?.let{
                labelDetectionNotifierInterface?.notifyResult(it)
            }
        }
    }
}