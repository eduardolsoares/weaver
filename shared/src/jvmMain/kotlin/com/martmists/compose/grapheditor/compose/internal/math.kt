package com.martmists.compose.grapheditor.compose.internal

import androidx.compose.ui.geometry.*
import kotlin.math.*

internal fun Size.offsetSize(offset: Offset): Size =
    Size(this.width - offset.x, this.height - offset.y)

internal fun Rect.intersects(start: Offset, end: Offset): Boolean {
    if (start in this || end in this) return true

    var tmin = 0f
    var tmax = 1f
    val dx = end.x - start.x
    val dy = end.y - start.y

    fun clip(p: Float, q: Float): Boolean {
        if (p == 0f) {
            return q >= 0
        }
        val t = q / p
        if (p < 0) {
            if (t > tmax) return false
            if (t > tmin) tmin = t
        } else {
            if (t < tmin) return false
            if (t < tmax) tmax = t
        }
        return true
    }

    if (!clip(-dx, start.x - left)) return false
    if (!clip(dx, right - start.x)) return false
    if (!clip(-dy, start.y - top)) return false
    if (!clip(dy, bottom - start.y)) return false

    return tmin <= tmax
}

internal fun Rect.intersectsBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, depth: Int): Boolean {
    val minX = minOf(p0.x, p1.x, p2.x, p3.x)
    val maxX = maxOf(p0.x, p1.x, p2.x, p3.x)
    val minY = minOf(p0.y, p1.y, p2.y, p3.y)
    val maxY = maxOf(p0.y, p1.y, p2.y, p3.y)

    if (left > maxX || right < minX || top > maxY || bottom < minY) return false
    if (this.intersects(p0, p3)) return true
    if (depth >= CONNECTION_BEZIER_MAX_DEPTH) return false

    val p01 = (p0 + p1) / 2f
    val p12 = (p1 + p2) / 2f
    val p23 = (p2 + p3) / 2f
    val p012 = (p01 + p12) / 2f
    val p123 = (p12 + p23) / 2f
    val p0123 = (p012 + p123) / 2f

    return intersectsBezier(p0, p01, p012, p0123, depth + 1) || intersectsBezier(p0123, p123, p23, p3, depth + 1)
}

internal fun Rect.intersectsBezier(start: Offset, end: Offset): Boolean {
    if (!this.overlaps(Rect(
            min(start.x, end.x),
            min(start.y, end.y),
            max(start.x, end.x),
            max(start.y, end.y)
        ))) return false

    val horizontalDistance = abs(end.x - start.x)
    val controlPointOffset = horizontalDistance / 2f
    return intersectsBezier(start, start + Offset(controlPointOffset, 0f), end - Offset(controlPointOffset, 0f), end, 0)
}
