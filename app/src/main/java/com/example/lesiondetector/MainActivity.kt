package com.example.lesiondetector

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.otaliastudios.cameraview.CameraView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (isAllGranted(Permission.CAMERA)) {
            text_view.text = "Camera permission granted"
        }

        runWithPermissions(Permission.CAMERA) {
            camera_view.visibility = View.VISIBLE
            val cameraView: CameraView = camera_view
            cameraView.setLifecycleOwner(this)
        }
    }
}