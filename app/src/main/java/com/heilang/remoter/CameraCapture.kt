package com.heilang.remoter

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CameraCapture {

    private lateinit var cameraExecutor: ExecutorService

    // Initialize executor for background thread
    fun initializeExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // Clean up the executor when the activity is destroyed
    fun shutdownExecutor() {
        cameraExecutor.shutdown()
    }

    // Function to capture image from either front or back camera
    fun captureImageFromCamera(
        context: Context,
        isFrontCamera: Boolean,
        onImageCaptured: (ImageBitmap?) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set up the image capture use case
            val imageCapture = ImageCapture.Builder().build()

            // Choose the front or back camera based on the `isFrontCamera` flag
            val cameraSelector = if (isFrontCamera) {
                androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll() // Unbind any existing use cases
                val camera = cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector, imageCapture
                )

                // Create a file to save the captured image
                val file = File(context.externalCacheDir, "captured_image_${System.currentTimeMillis()}.jpg")

                // Capture the image
                imageCapture.takePicture(
                    androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build(),
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            // Image captured successfully, convert to ImageBitmap and update the UI
                            val imageBitmap = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()

                            // Post to main thread to update UI
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onImageCaptured(imageBitmap)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // Handle error
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "Image capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to bind camera use case: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
