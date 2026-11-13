package io.github.youndie.toon.bench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.toon.component.ToonSpeakerKind
import io.github.youndie.toon.component.ToonTranscriptTurn
import io.github.youndie.toon.theme.Toon
import io.github.youndie.toon.theme.ToonTheme
import java.io.File

/**
 * B-78: the transcript turn's budget, re-measured the way the desktop design states it — 1,500
 * turns in a list scrolled at 60 FPS on an Apple M1. The list fills the design's 1280 × 800 window
 * at density 2 (2560 × 1600 px), its column the extension's 640 dp text measure plus the turn's
 * gutter and padding, under a mouse resting in the list, so plates and actions come and go as
 * turns pass under it.
 *
 * Each frame scrolls the list by a fixed distance and times one `render`: composition, layout and
 * drawing, rasterised on the CPU (no GPU, no compositor), so it is an upper bound of the work the
 * app's frame does. Arms, interleaved over 3 rounds after a warm-up pass over the whole list:
 * reading (4 dp a frame, 240 dp/s), wheel (30 dp a frame), fling (240 dp a frame, the whole list),
 * and the wheel again with a search marking every "релиз". One CSV row per frame.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "turn-scroll.csv")
    out.parentFile?.mkdirs()
    val density = 2f
    val (w, h) = 1280 to 800
    var mark = ""
    val state = LazyListState()
    val turns = sampleTurns(1_500)
    val scene =
        ImageComposeScene((w * density).toInt(), (h * density).toInt(), Density(density)) {
            ToonTheme {
                Box(Modifier.fillMaxSize().background(Toon.tokens.colors.stage)) {
                    LazyColumn(Modifier.width(COLUMN.dp).fillMaxSize(), state) {
                        itemsIndexed(turns) { i, t ->
                            ToonTranscriptTurn(
                                speaker = t.speaker,
                                paragraphs = t.paragraphs,
                                letter = t.letter,
                                kind = t.kind,
                                shape = t.shape,
                                color = t.color,
                                time = t.time,
                                playing = i == 40,
                                mark = mark,
                                seed = i,
                            )
                        }
                    }
                }
            }
        }
    val pointer = Offset(400f * density, 400f * density)
    var clock = 0L
    val frameNs = 1_000_000_000L / 60

    fun frame(dp: Float): Long {
        state.dispatchRawDelta(dp * density)
        scene.sendPointerEvent(PointerEventType.Move, pointer)
        clock += frameNs
        val start = System.nanoTime()
        scene.render(clock).close()
        return System.nanoTime() - start
    }
    val rows = StringBuilder("arm,round,frame,first_item,render_ns\n")

    fun arm(
        name: String,
        round: Int,
        step: Float,
        frames: Int,
        record: Boolean = true,
    ) {
        state.requestScrollToItem(if (name == "fling") 0 else 200 + maxOf(0, round) * 300)
        scene.render(clock)
        for (f in 0 until frames) {
            val ns = frame(step)
            if (record) rows.append("$name,$round,$f,${state.firstVisibleItemIndex},$ns\n")
            // A look at what is being timed: TURN_BENCH_SHOT=<dir> keeps the first search frame.
            if (record && name == "search" && round == 0 && f == 0) {
                System.getenv("TURN_BENCH_SHOT")?.let { dir ->
                    File(dir, "turn-scroll-frame.png").writeBytes(
                        scene.render(clock).use { it.encodeToData()!!.bytes },
                    )
                }
            }
            if (!state.canScrollForward) break
        }
    }
    scene.sendPointerEvent(PointerEventType.Enter, pointer)
    // Warm-up: the whole list once, so every code path is compiled before a frame is kept.
    arm("fling", -1, 240f, 10_000, record = false)
    arm("wheel", -1, 30f, 300, record = false)
    for (round in 0 until 3) {
        mark = ""
        arm("reading", round, 4f, 300)
        arm("wheel", round, 30f, 300)
        arm("fling", round, 240f, 10_000)
        mark = "релиз"
        arm("search", round, 30f, 300)
    }
    scene.close()
    out.writeText(rows.toString())
    summarise(rows.toString())
    println("B-78 turn scroll: frames written to $out")
}

private const val COLUMN = 640 + 56 + 12

private class SampleTurn(
    val speaker: String,
    val letter: String,
    val kind: ToonSpeakerKind,
    val shape: Int,
    val color: Int,
    val time: String,
    val paragraphs: List<String>,
)

/** A two-hour call's turns: four voices in turn, one to three paragraphs of the design's sentences. */
private fun sampleTurns(n: Int): List<SampleTurn> {
    val sentences =
        listOf(
            "Тогда давайте так.",
            "Релиз не в четверг.",
            "Переносим на вторник, семнадцатое.",
            "Кто берёт на себя проверку сборки?",
            "Я посмотрю логи после обеда и напишу в чат.",
            "Then let us agree: the release moves, the freeze stays.",
        )
    val voices =
        listOf(
            listOf("Марина", "М", ToonSpeakerKind.Named, 0, 0),
            listOf("Олег", "О", ToonSpeakerKind.Named, 1, 1),
            listOf("Я", "Я", ToonSpeakerKind.Me, 0, 0),
            listOf("Спикер 4", "4", ToonSpeakerKind.Unnamed, 2, 2),
        )
    return List(n) { i ->
        val v = voices[i % voices.size]
        val paras =
            List(1 + i % 3) { p ->
                List(2 + (i + p) % 5) { s -> sentences[(i * 7 + p * 3 + s) % sentences.size] }.joinToString(" ")
            }
        val sec = i * 5
        SampleTurn(
            v[0] as String,
            v[1] as String,
            v[2] as ToonSpeakerKind,
            v[3] as Int,
            v[4] as Int,
            "%d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60),
            paras,
        )
    }
}

private fun summarise(csv: String) {
    val byArm =
        csv
            .lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { it.split(',') }
            .groupBy({ it[0] }, { it[4].toLong() / 1e6 })
    for ((arm, ms) in byArm) {
        val s = ms.sorted()

        fun q(p: Double) = s[((s.size - 1) * p).toInt()]
        println(
            "%-8s frames %5d  p50 %6.2f ms  p95 %6.2f ms  p99 %6.2f ms  max %6.2f ms  over 16.7 ms: %d"
                .format(arm, s.size, q(.5), q(.95), q(.99), s.last(), s.count { it > 1000.0 / 60 }),
        )
    }
}
