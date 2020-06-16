package com.example.lesiondetector

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.otaliastudios.cameraview.CameraView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var cameraView: CameraView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isAllGranted(Permission.CAMERA)) {
            askForPermissions(Permission.CAMERA) { result ->
                if (result.isAllGranted(Permission.CAMERA)) {
                    launchCameraView()
                }
            }
        }

        text_view.setOnClickListener {
            launchCameraView()
        }
    }

    private fun launchCameraView() {
        runWithPermissions(Permission.CAMERA) {
            camera_view.visibility = View.VISIBLE
            cameraView = camera_view
            cameraView.setLifecycleOwner(this)
        }
    }
}