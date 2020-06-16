package com.example.lesiondetector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (isAllGranted(Permission.CAMERA)) {
            text_view.text = "Camera permission granted"
        }

        runWithPermissions(Permission.CAMERA) {
            text_view.text = "Camera permission granted"
        }
    }
}