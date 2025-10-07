package com.lms.ayan.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lms.ayan.R
import com.lms.ayan.util.SessionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        moveToNextScreen()
    }

    private fun moveToNextScreen() {
        val sessionUtil = SessionUtil.getInstance(this)
        val mainActIntent = Intent(this, HomeActivity::class.java)
        val authIntent = Intent(this, AuthenticationActivity::class.java)
        Handler(Looper.getMainLooper()).postDelayed({
            CoroutineScope(Dispatchers.Default).launch {
                if (sessionUtil.isLoggedIn()) {
                    withContext(Dispatchers.Main) {
                        startActivity(mainActIntent)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        startActivity(authIntent)
                        finish()
                    }
                }
            }
        }, 3000)
    }
}