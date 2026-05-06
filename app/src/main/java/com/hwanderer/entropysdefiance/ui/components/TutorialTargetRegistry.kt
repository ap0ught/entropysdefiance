package com.hwanderer.entropysdefiance.ui.components

import android.view.View
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import kotlin.reflect.KProperty

data class TargetBounds(
    val rect: Rect,
    val timestamp: Long = System.currentTimeMillis()
)

class TutorialTargetRegistry {
    private val targets = mutableMapOf<String, TargetBounds>()

    fun reportBounds(targetId: String, rect: Rect) {
        targets[targetId] = TargetBounds(rect)
    }

    fun getBounds(targetId: String): Rect? {
        return targets[targetId]?.rect
    }

    fun remove(targetId: String) {
        targets.remove(targetId)
    }

    fun clear() {
        targets.clear()
    }

    fun getAllTargets(): Map<String, TargetBounds> = targets.toMap()
}

val LocalTutorialTargets = compositionLocalOf<TutorialTargetRegistry> {
    error("No TutorialTargetRegistry provided")
}

operator fun TutorialTargetRegistry.getValue(thisRef: Any?, property: KProperty<*>): TutorialTargetRegistry = this

fun TutorialTargetRegistry.report(
    targetId: String,
    rect: androidx.compose.ui.geometry.Rect
) = reportBounds(targetId, rect)

fun TutorialTargetRegistry.report(
    targetId: String,
    coordinates: LayoutCoordinates,
    view: View
) {
    val boundsInWindow = coordinates.boundsInWindow()
    val locationOnScreen = IntArray(2)
    view.getLocationOnScreen(locationOnScreen)

    val screenRect = Rect(
        left = boundsInWindow.left + locationOnScreen[0],
        top = boundsInWindow.top + locationOnScreen[1],
        right = boundsInWindow.right + locationOnScreen[0],
        bottom = boundsInWindow.bottom + locationOnScreen[1]
    )

    reportBounds(targetId, screenRect)
}
