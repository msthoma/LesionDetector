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
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {
    private val IMAGE_MEAN = 0.0f
    private val IMAGE_STD = 255.0f
    private val PROBABILITY_MEAN = 0.0f
    private val PROBABILITY_STD = 1.0f

    lateinit var cameraView: CameraView
    private lateinit var tflite: Interpreter
    private lateinit var rgbBitmap: Bitmap
    private lateinit var inputImageBuffer: TensorImage
    private lateinit var outputProbabilityBuffer: TensorBuffer
    private lateinit var probabilityProcessor: TensorProcessor

    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tfliteModel = FileUtil.loadMappedFile(this, "cnn64RGB.tflite")
        tflite = Interpreter(tfliteModel, Interpreter.Options())

        val labels = FileUtil.loadLabels(this, "cnn64RGB_labels.txt")

        val imageTensorIndex = 0
        val imageShape = tflite.getInputTensor(imageTensorIndex).shape()
        val imageSizeY = imageShape[1]
        val imageSizeX = imageShape[2]

        val imageDataType = tflite.getInputTensor(imageTensorIndex).dataType()
        val probabilityTensorIndex = 0
        val probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape()
        val probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType()

        inputImageBuffer = TensorImage(imageDataType)

        outputProbabilityBuffer =
            TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)

        probabilityProcessor =
            TensorProcessor.Builder().add(NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)).build()

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