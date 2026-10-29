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
        Image(painterResource(R.drawable.nodus_logo), contentDescription = "noduS launcher — iSSU Open Homelab",
            modifier = Modifier.align(Alignment.CenterHorizontally).width(150.dp).padding(vertical = 12.dp))
        if (!welcome) AppText("Versione $version", size = 12, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        AppText(if (welcome) "Benvenuto in noduS" else "Un posto per le comunicazioni", size = 14, medium = true, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        AppText("Una home essenziale e un hub per le notifiche attive. Le app a portata di mano, le comunicazioni in un unico elenco.", size = 12, color = Color.LightGray)
        listOf(
            "Tocca una notifica" to "Apri il contenuto nell’app.",
            "Tieni premuto" to "Rispondi dall’hub, se supportato.",
            "Scorri a destra o sinistra" to "Elimina la notifica anche da Android.",
            "Indietro o swipe destro nel vuoto" to "Torna alla home."
        ).forEach { (title, detail) ->
            AppText(title, modifier = Modifier.padding(top = 16.dp))
            AppText(detail, size = 12, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        if (!welcome) {
            AppText("La tua home", size = 14, medium = true, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            AppText("Preferiti nel tuo ordine, poi le app più usate. Musica, eventi di oggi e meteo opzionali. Font, dimensioni, icone e posizione verticale regolabili.", size = 12, color = Color.LightGray)
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))) } }) { AppText("Meteo: Open-Meteo · CC BY 4.0", size = 11, color = Accent) }
        }
        AppText("Scorri verso l’alto nella home per tutte le app, partendo lontano dai bordi di Android.", size = 12, color = Color.LightGray, modifier = Modifier.padding(top = 20.dp))
        AppText("Tieni premuto sull’ora o su un’area vuota della home per le impostazioni.", size = 12, color = Color.LightGray, modifier = Modifier.padding(top = 20.dp))
        TextButton(onClick = {
            if (welcome) scope.launch { Prefs(context).setFirstRun(false); (context as? Activity)?.finish() }
            else context.startActivity(Intent(context, WelcomeActivity::class.java))
        }, modifier = Modifier.padding(top = 16.dp)) { AppText(if (welcome) "Inizia" else "Rivedi il benvenuto", color = Accent) }
    }
}
