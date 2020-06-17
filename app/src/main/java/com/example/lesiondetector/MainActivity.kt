package com.example.lesiondetector

import android.graphics.Bitmap
import android.media.Image
import android.media.Image.Plane
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.otaliastudios.cameraview.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    lateinit var cameraView: CameraView
    private lateinit var rgbBitmap: Bitmap
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null

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

            Log.d("camera engine", cameraView.engine.toString())

            cameraView.addFrameProcessor { frame ->

                // frame should only be an instance of Image, never byte[], since Camera2 engine is
                // specified in the xml layout of CameraView
                if (frame.dataClass == Image::class.java) {
                    val rotation = frame.rotationToView - frame.rotationToUser
                    Log.d(
                        "frame",
                        "rotationUser: ${frame.rotationToUser} rotationView: ${frame.rotationToView} $rotation"
                    )

                    val image = frame.getData<Image>()

                    val planes = image.planes
                    fillBytes(planes, yuvBytes)

                    val yRowStride = planes[0].rowStride
                    val uvRowStride = planes[1].rowStride
                    val uvPixelStride = planes[1].pixelStride
                    val previewWidth = image.width
                    val previewHeight = image.height
                    Log.d("preview shape", "w$previewWidth h$previewHeight")

                    rgbBitmap = Bitmap.createBitmap(
                        previewWidth, previewHeight,
                        Bitmap.Config.ARGB_8888
                    )

                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.IO) {
                            if (rgbBytes == null) {
                                rgbBytes = IntArray(previewWidth * previewHeight)
                            }
                            ImageUtils.convertYUV420ToARGB8888(
                                yuvBytes[0],
                                yuvBytes[1],
                                yuvBytes[2],
                                previewWidth,
                                previewHeight,
                                yRowStride,
                                uvRowStride,
                                uvPixelStride,
                                rgbBytes
                            ) // have to wait for this to finish
                            // then set rgb below
                            rgbBitmap.setPixels(
                                rgbBytes, 0, previewWidth, 0, 0,
                                previewWidth, previewHeight
                            )
                            // then run classifier again in background below
                            // and then in let below display results in main thread
                        }.let {
                            if (preview_frame.visibility == View.GONE) {
                                preview_frame.visibility = View.VISIBLE
                            }
                            preview_frame.setImageBitmap(rgbBitmap)
//                            Log.d("rgbBitmap", "w${rgbBitmap.width} h${rgbBitmap.height}")
                        }
                    }

                }
            }
        }
    }

    private fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            yuvBytes[i]?.let { buffer[it] }
        }
    }
}