package io.github.youndie.toon.bench.android

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.toon.bench.Arm
import io.github.youndie.toon.bench.FrameCostScene
import io.github.youndie.toon.bench.REPLAY_MS

private const val TAG = "ToonBench"

/**
 * Runs the B-05 protocol once and logs every frame: 5 warm-up replays per arm, 20 ABBA rounds of
 * Rebuild and Cache, 5 of Control. A frame belongs to the replay whose window its intended vsync
 * falls in; frames between replays are dropped.
 */
class BenchActivity : ComponentActivity() {
    private data class ReplayWindow(
        val arm: Arm,
        val replay: Int,
        val record: Boolean,
        val startNs: Long,
        var endNs: Long = Long.MAX_VALUE,
    )

    // The scene's own draw call on the UI thread (geometry + recording), for the frame being drawn.
    @Volatile private var lastSceneNs = 0L

    private val windows = java.util.Collections.synchronizedList(mutableListOf<ReplayWindow>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metricsThread = HandlerThread("frame-metrics").apply { start() }
        window.addOnFrameMetricsAvailableListener(::onFrame, Handler(metricsThread.looper))
        Log.i(TAG, "begin,arm,replay,intended_vsync_ns,total_ns,draw_ns,sync_ns,command_issue_ns,gpu_ns,scene_ns")
        val plan =
            buildList {
                repeat(5) {
                    add(Triple(Arm.Rebuild, it, false))
                    add(Triple(Arm.Cache, it, false))
                }
                for (round in 0 until 20) {
                    val order = if (round % 2 == 0) listOf(Arm.Rebuild, Arm.Cache) else listOf(Arm.Cache, Arm.Rebuild)
                    order.forEach { add(Triple(it, round, true)) }
                }
                repeat(5) { add(Triple(Arm.Control, it, true)) }
            }
        setContent {
            var step by remember { mutableStateOf(0) }
            var t by remember { mutableFloatStateOf(0f) }
            val (arm, replay, record) = plan[minOf(step, plan.lastIndex)]
            val scene = remember(step) { FrameCostScene(arm) }
            LaunchedEffect(step) {
                if (step >= plan.size) {
                    Log.i(TAG, "end")
                    return@LaunchedEffect
                }
                var start = 0L
                withFrameNanos { start = it }
                windows += ReplayWindow(arm, replay, record, start)
                while (true) {
                    val now = withFrameNanos { it }
                    t = (now - start) / 1e6f
                    if (t > REPLAY_MS) break
                }
                windows.last().endNs = withFrameNanos { it }
                step++
            }
            Canvas(Modifier.fillMaxSize()) {
                val start = System.nanoTime()
                translate(16f * density, 40f * density) { scene.draw(this, t) }
                lastSceneNs = System.nanoTime() - start
            }
        }
    }

    private fun onFrame(
        window: Window,
        metrics: FrameMetrics,
        dropped: Int,
    ) {
        val vsync = metrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
        val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val draw = metrics.getMetric(FrameMetrics.DRAW_DURATION)
        val sync = metrics.getMetric(FrameMetrics.SYNC_DURATION)
        val issue = metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
        val gpu = metrics.getMetric(FrameMetrics.GPU_DURATION)
        val w = synchronized(windows) { windows.lastOrNull { vsync >= it.startNs && vsync <= it.endNs } } ?: return
        // scene_ns is the latest draw's, which lags the metrics callback by up to a frame; it is
        // read as a distribution per arm, not frame by frame.
        if (w.record) Log.i(TAG, "frame,${w.arm},${w.replay},$vsync,$total,$draw,$sync,$issue,$gpu,$lastSceneNs")
    }
}
