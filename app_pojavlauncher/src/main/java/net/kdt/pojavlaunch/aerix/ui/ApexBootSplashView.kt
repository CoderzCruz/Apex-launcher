/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The boot splash: the Apex launch artwork with a live progress bar drawn over the artwork's own
 * bar, so the percentage and the fill are real instead of the numbers painted into the image.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import net.kdt.pojavlaunch.R
import kotlin.math.roundToInt

/**
 * Draws the boot artwork full bleed and replaces its baked-in loading bar with a working one.
 *
 * The bar geometry below is measured from the artwork itself (track spans 25.0%–74.7% of the width
 * and 76.6%–83.0% of the height), so the replacement lands exactly over the printed bar on any
 * screen size, and the printed "72%" never shows through.
 */
class ApexBootSplashView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val art: Bitmap? = runCatching {
        BitmapFactory.decodeResource(resources, R.drawable.apex_boot_splash)
    }.getOrNull()

    /** Overall boot progress, 0..100. */
    private var progress = 0

    /** Drawn progress, eased towards [progress] so the bar never jumps. */
    private var drawnProgress = 0f

    private var animator: ValueAnimator? = null

    private val dst = Rect()
    private val track = RectF()
    private val interior = RectF()
    private val fill = RectF()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4141")
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }
    private val backdrop = Paint().apply { color = Color.parseColor("#080000") }

    fun setProgress(percent: Int) {
        val target = percent.coerceIn(0, 100)
        if (target == progress) return
        progress = target
        animator?.cancel()
        animator = ValueAnimator.ofFloat(drawnProgress, target.toFloat()).apply {
            duration = 260L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                drawnProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Jumps straight to [percent] without animating; used when the first frame is drawn. */
    fun snapTo(percent: Int) {
        animator?.cancel()
        progress = percent.coerceIn(0, 100)
        drawnProgress = progress.toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = art ?: return
        cropToFill(bitmap)

        canvas.drawBitmap(bitmap, null, dst, null)

        val inset = (track.height() * 0.10f).coerceAtLeast(2f)
        interior.set(track.left + inset, track.top + inset, track.right - inset, track.bottom - inset)

        // Hide the bar and the percentage printed into the artwork, then draw the real ones.
        canvas.drawRoundRect(interior, inset, inset, backdrop)

        if (drawnProgress > 0f) {
            val right = interior.left + interior.width() * (drawnProgress / 100f)
            fill.set(interior.left, interior.top, right.coerceAtLeast(interior.left + 1f), interior.bottom)
            fillPaint.shader = LinearGradient(
                fill.left, fill.top, fill.right, fill.bottom,
                intArrayOf(Color.parseColor("#B31313"), Color.parseColor("#FF3B3B")),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(fill, inset * 0.6f, inset * 0.6f, fillPaint)
            fillPaint.shader = null
        }

        percentPaint.textSize = interior.height() * 0.74f
        val textY = interior.centerY() - (percentPaint.descent() + percentPaint.ascent()) / 2f
        canvas.drawText(
            "${drawnProgress.roundToInt()}%",
            interior.right - interior.height() * 0.18f,
            textY,
            percentPaint
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutTrack()
    }

    /** The artwork is drawn over the whole view, cropped (never stretched) like centerCrop. */
    private fun cropToFill(bitmap: Bitmap) {
        val scale = maxOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val w = (bitmap.width * scale).roundToInt()
        val h = (bitmap.height * scale).roundToInt()
        val left = (width - w) / 2
        val top = (height - h) / 2
        dst.set(left, top, left + w, top + h)
    }

    private fun layoutTrack() {
        val bitmap = art ?: return
        cropToFill(bitmap)
        track.set(
            dst.left + dst.width() * BAR_LEFT,
            dst.top + dst.height() * BAR_TOP,
            dst.left + dst.width() * BAR_RIGHT,
            dst.top + dst.height() * BAR_BOTTOM
        )
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private companion object {
        // Measured from res/drawable-nodpi/apex_boot_splash.png.
        const val BAR_LEFT = 0.250f
        const val BAR_RIGHT = 0.747f
        const val BAR_TOP = 0.766f
        const val BAR_BOTTOM = 0.830f
    }
}
