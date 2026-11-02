package com.nodus.launcher.ui.welcome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.nodus.launcher.util.installedVersion
import com.nodus.launcher.R
import com.nodus.launcher.ui.common.Accent
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.theme.NodusTheme
import com.nodus.launcher.util.Prefs

/** Bundled, offline notes. The first-install welcome remains a separate flow. */
class ReleaseNotesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val installed = installedVersion()
        setContent {
            NodusTheme {
                LaunchedEffect(Unit) { Prefs(this@ReleaseNotesActivity).markVersionSeen(installed.code) }
                BackHandler { finish() }
                Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
                    .verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Image(painterResource(R.drawable.nodus_icon), contentDescription = "noduS",
                        modifier = Modifier.size(64.dp).padding(bottom = 8.dp))
                    AppText(stringResource(R.string.ui_update_title, installed.name), size = 20, medium = true)
                    AppText(stringResource(R.string.ui_update_intro), size = 12, color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                    listOf(R.string.ui_update_layout_controls, R.string.ui_update_presence_indicator,
                        R.string.ui_update_settings_groups, R.string.ui_update_licenses).forEach { text ->
                        AppText("• " + stringResource(text), size = 13, modifier = Modifier.padding(bottom = 16.dp))
                    }
                    TextButton(onClick = { finish() }, contentPadding = PaddingValues(0.dp)) {
                        AppText(stringResource(R.string.ui_update_continue), color = Accent)
                    }
                }
            }
        }
    }
}
