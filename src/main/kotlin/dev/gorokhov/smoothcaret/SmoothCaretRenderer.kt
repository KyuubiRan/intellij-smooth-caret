package dev.gorokhov.smoothcaret

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import javax.swing.Timer
import kotlin.math.abs

class SmoothCaretRenderer(private val settings: SmoothCaretSettings) : CustomHighlighterRenderer {
    private var currentX: Double = 0.0
    private var currentY: Double = 0.0
    private var targetX: Double = 0.0
    private var targetY: Double = 0.0
    private var timer: Timer? = null
    private var lastEditor: Editor? = null

    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        if (!settings.isEnabled) return

        // Reset position if editor changed
        if (lastEditor != editor) {
            lastEditor = editor
            resetPosition(editor)
        }

        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val point = editor.caretModel.visualPosition.let { editor.visualPositionToXY(it) }

        // Reset position if there's a large jump
        if (Math.abs(point.x - targetX) > 1000 || Math.abs(point.y - targetY) > 1000) {
            resetPosition(editor)
        }

        ensureTimerStarted(editor)

        targetX = point.x.toDouble()
        targetY = point.y.toDouble()

        g2d.color = editor.colorsScheme.defaultForeground

        val metrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(null))
        val height = metrics.height

        // Only draw if we have valid positions
        if (currentX.isFinite() && currentY.isFinite()) {
            when (settings.caretStyle) {
                SmoothCaretSettings.CaretStyle.BLOCK -> {
                    g2d.fillRect(
                        currentX.toInt(),
                        currentY.toInt(),
                        settings.caretWidth,
                        height
                    )
                }
                SmoothCaretSettings.CaretStyle.LINE -> {
                    g2d.fillRect(
                        currentX.toInt(),
                        currentY.toInt(),
                        settings.caretWidth,
                        height
                    )
                }
                SmoothCaretSettings.CaretStyle.UNDERSCORE -> {
                    g2d.fillRect(
                        currentX.toInt(),
                        currentY.toInt() + height - 2,
                        settings.caretWidth * 2,
                        2
                    )
                }
            }
        }
    }

    private fun resetPosition(editor: Editor) {
        val point = editor.caretModel.visualPosition.let { editor.visualPositionToXY(it) }
        currentX = point.x.toDouble()
        currentY = point.y.toDouble()
        targetX = currentX
        targetY = currentY
    }

    private fun getScreenRefreshRate(): Int {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.screenDevices
        var refreshRate = 60
        for (device in gd) {
            val mode = device.displayMode
            refreshRate = mode.refreshRate
        }
        return refreshRate
    }

    private fun ensureTimerStarted(editor: Editor) {
        if (timer == null) {
            timer = Timer(1000 / getScreenRefreshRate()) {
                if (!editor.isDisposed) {
                    val dx = targetX - currentX
                    val dy = targetY - currentY
                    if (abs(dx) > 0.01 || abs(dy) > 0.01) {
                        currentX += dx * 0.3
                        currentY += dy * 0.3
                        editor.contentComponent.repaint()
                    }
                } else {
                    timer?.stop()
                    timer = null
                }
            }
            timer?.start()
        }
    }
}