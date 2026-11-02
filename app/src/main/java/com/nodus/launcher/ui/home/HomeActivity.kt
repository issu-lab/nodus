package com.nodus.launcher.ui.home

import android.content.Intent
import android.os.Bundle
import com.nodus.launcher.util.installedVersion
import com.nodus.launcher.home.UpdateNoticePolicy
import com.nodus.launcher.ui.welcome.ReleaseNotesActivity
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
        val installed = installedVersion()
        setContent {
            NodusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black
                ) {
                    val launchState by prefs.launchState.collectAsState(initial = null)
                    var welcomed by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(launchState) {
                        val state = launchState ?: return@LaunchedEffect
                        if (!welcomed) {
                            if (state.firstRun) {
                                welcomed = true
                                startActivity(Intent(this@HomeActivity, WelcomeActivity::class.java))
                            } else if (UpdateNoticePolicy.show(false, state.seenVersion, installed.code)) {
                                welcomed = true
                                startActivity(Intent(this@HomeActivity, ReleaseNotesActivity::class.java))
                            }
                        }
                    }
                    HomeScreen(homeRequest = homeRequests.intValue)
                }
            }
        }
    }
}
