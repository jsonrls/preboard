// com.pbec.preboardexamchecker/MyApplication.kt
package com.pbec.preboardexamchecker
import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core // Make sure this is imported
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        Log.d("OpenCV_Loader_Check", "Core.NATIVE_LIBRARY_NAME: ${Core.NATIVE_LIBRARY_NAME}")

        // Initialize OpenCV when the application process starts
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed!")
            // Consider adding more robust error handling or user notification here
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded.")
        }
    }
}