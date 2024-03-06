package com.inasweaterpoorlyknit.jniplayground

import android.app.Application
import android.widget.Toast

class JNIPlaygroundApplication: Application() {
    companion object {
        init {
            System.loadLibrary("jniplayground")
        }
    }
}