package com.nodus.launcher.ui.welcome

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nodus.launcher.R
import com.nodus.launcher.ui.common.*
import com.nodus.launcher.util.Prefs
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen() {
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(16.dp)) { AppIdentityContent(welcome = true) }
}

@Composable
fun AppIdentityContent(welcome: Boolean) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val version = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() } catch (_: Exception) { "—" } }
    Column(Modifier.fillMaxWidth()) {
        Image(painterResource(if (MaterialTheme.colorScheme.background.luminance() < .5f) R.drawable.nodus_logo else R.drawable.nodus_logo_light), contentDescription = "noduS launcher — iSSU Open Homelab",
            modifier = Modifier.align(Alignment.CenterHorizontally).width(150.dp).padding(vertical = 12.dp))
        if (!welcome) AppText(context.getString(R.string.ui_version_1_s, version), size = 12, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        AppText(if (welcome) context.getString(R.string.ui_welcome_to_nodus) else context.getString(R.string.ui_a_place_for_your_communications), size = 14, medium = true, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        AppText(context.getString(R.string.ui_a_minimal_home_and_a_hub_for), size = 12, color = Color.LightGray)
        listOf(
            context.getString(R.string.ui_tap_a_notification) to context.getString(R.string.ui_open_its_content_in_the_app),
            context.getString(R.string.ui_touch_and_hold) to context.getString(R.string.ui_reply_from_the_hub_when_supported),
            context.getString(R.string.ui_swipe_left_or_right) to context.getString(R.string.ui_dismiss_the_notification_in_android_too),
            context.getString(R.string.ui_back_or_swipe_right_on_empty_space) to context.getString(R.string.ui_return_home)
        ).forEach { (title, detail) ->
            AppText(title, modifier = Modifier.padding(top = 16.dp))
            AppText(detail, size = 12, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        if (!welcome) {
            AppText(context.getString(R.string.ui_your_home), size = 14, medium = true, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            AppText(context.getString(R.string.ui_favorites_in_your_order_followed_by_your), size = 12, color = Color.LightGray)
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))) } }) { AppText(context.getString(R.string.ui_weather_open_meteo_cc_by_4_0), size = 11, color = Accent) }
        }
        AppText(context.getString(R.string.ui_swipe_up_inside_home_to_see_all), size = 12, color = Color.LightGray, modifier = Modifier.padding(top = 20.dp))
        AppText(context.getString(R.string.ui_hold_the_clock_or_an_empty_area), size = 12, color = Color.LightGray, modifier = Modifier.padding(top = 20.dp))
        TextButton(onClick = {
            if (welcome) scope.launch { Prefs(context).setFirstRun(false); (context as? Activity)?.finish() }
            else context.startActivity(Intent(context, WelcomeActivity::class.java))
        }, modifier = Modifier.padding(top = 16.dp)) { AppText(if (welcome) context.getString(R.string.ui_get_started) else context.getString(R.string.ui_show_welcome_again), color = Accent) }
    }
}
