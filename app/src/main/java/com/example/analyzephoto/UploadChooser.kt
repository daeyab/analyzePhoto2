package com.example.analyzephoto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.upload_chooser.*

class UploadChooser : BottomSheetDialogFragment(){

    //리스너를 다른곳에서 작업하고 싶을 때 인터페이스를 활용하여 작업을 수행한다
    interface UploadChooserNotifierInterface {
        fun CamaraOnClick()
        fun GalleryOnClick()
    }

    var uploadChooserNotifierInterface : UploadChooserNotifierInterface?=null

    fun addNotifier(listener : UploadChooserNotifierInterface){
        uploadChooserNotifierInterface=listener
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_chooser,container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListener()
    }

    private fun setupListener() {
        upload_camera.setOnClickListener {
            uploadChooserNotifierInterface?.CamaraOnClick()
        }
        upload_gallery.setOnClickListener {
            uploadChooserNotifierInterface?.GalleryOnClick()
        }
    }
}