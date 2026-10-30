package com.closetstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.closetstudio.app.ui.RoomScreen
import com.closetstudio.app.ui.theme.ClosetStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClosetStudioTheme {
                RoomScreen()
            }
        }
    }
}
