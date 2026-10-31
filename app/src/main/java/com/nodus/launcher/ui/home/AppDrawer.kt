package com.nodus.launcher.ui.home

import androidx.compose.ui.platform.LocalContext
import com.nodus.launcher.R
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.nodus.launcher.ui.common.*

/** Keep the first lazy viewport warm below home; closed content has no accessible nodes. */
@Composable
fun AppDrawer(state: AppDrawerState, visible: Boolean, apps: List<AppItemUi>, list: LazyListState,
              close: () -> Unit, open: (AppItemUi) -> Unit, favorite: (AppItemUi) -> Unit) {
    val context = LocalContext.current
    var height by remember { mutableStateOf(0f) }
    val density = LocalDensity.current.density
    val scrollConnection = remember(state, list, density) {
        object : NestedScrollConnection {
            private var closing = false
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!closing || source != NestedScrollSource.Drag) return Offset.Zero
                state.drag(available.y, height)
                return Offset(0f, available.y)
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Only deliberate drag left over at the top can move the panel; never a list fling.
                if (source != NestedScrollSource.Drag || available.y <= 0f || list.canScrollBackward) return Offset.Zero
                if (!closing) closing = state.startClosing()
                if (!closing) return Offset.Zero
                state.drag(available.y, height)
                return Offset(0f, available.y)
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!closing) return Velocity.Zero
                closing = false
                state.release(available.y, height, density)
                return available
            }
        }
    }
    Column(Modifier.fillMaxSize().onSizeChanged { height = it.height.toFloat() }.zIndex(if (visible) 1f else -1f).graphicsLayer { translationY = size.height * (1f - state.progress) }
        .background(Color.Black)
        .pointerInput(Unit) { detectTapGestures(onTap = {}) }
        .then(if (visible) Modifier.semantics { paneTitle = context.getString(R.string.ui_runtime_all_apps) } else Modifier.clearAndSetSemantics {})
        .padding(start = ContentGrid.Outer, end = ContentGrid.End)) {
        TextButton(onClick = close, modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(ContentGrid.IconLane)) { AppText("‹") }
                AppText(context.getString(R.string.ui_runtime_home))
            }
        }
        LazyColumn(state = list, modifier = Modifier.fillMaxWidth().weight(1f).nestedScroll(scrollConnection), contentPadding = PaddingValues(bottom = 16.dp)) {
            var letter = ""
            apps.forEachIndexed { index, app ->
                val first = app.app.label.take(1).uppercase()
                if (first != letter) {
                    letter = first
                    item(key = "letter:$index:$first", contentType = "letter") {
                        AppText(first, size = 13, color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }
                }
                item(key = "app:${app.app.packageName}", contentType = "app") {
                    AppRow(app, onClick = { open(app) }, onLongClick = { favorite(app) })
                }
            }
        }
    }
}
