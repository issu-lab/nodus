package com.nodus.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.nodus.launcher.home.HomePolicy
import kotlin.math.roundToInt

/** Measure once, then move the whole home up as content grows. No scroll state.
 * Only an overfull viewport requires proportional fitting; user settings remain intact.
 * Scaling around the leading edge preserves alignment of dots/icons and text.
 */
@Composable
fun FixedHomeLayout(offset: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = { Box { content() } }) { children, constraints ->
        val child = children.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val fit = HomePolicy.fixedPlacement(constraints.maxHeight.toFloat(), child.height.toFloat(), offset.toPx())
        layout(constraints.maxWidth, constraints.maxHeight) {
            child.placeWithLayer(0, fit.top.roundToInt()) {
                scaleX = fit.scale; scaleY = fit.scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
