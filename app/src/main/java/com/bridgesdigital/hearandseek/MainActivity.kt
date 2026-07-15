package com.bridgesdigital.hearandseek

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bridgesdigital.hearandseek.ui.HearAndSeekApp
import com.bridgesdigital.hearandseek.ui.theme.HearAndSeekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            HearAndSeekTheme {
                HearAndSeekApp()
            }
        }
    }
}
