package com.nodus.launcher.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.nodus.launcher.ui.theme.NodusTheme
import com.nodus.launcher.ui.welcome.WelcomeActivity
import com.nodus.launcher.util.Prefs

class HomeActivity : ComponentActivity() {
    private val homeRequests = mutableIntStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeRequests.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = Prefs(this)
        setContent {
            NodusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black
                ) {
                    val firstRun by prefs.firstRun.collectAsState(initial = false)
                    var welcomed by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(firstRun) {
                        if (firstRun && !welcomed) {
                            welcomed = true
                            startActivity(Intent(this@HomeActivity, WelcomeActivity::class.java))
                        }
                    }
                    HomeScreen(homeRequest = homeRequests.intValue)
                }
            }
        }
    }
}
