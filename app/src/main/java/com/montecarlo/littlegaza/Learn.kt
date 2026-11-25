package com.montecarlo.littlegaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ one exercise

@Composable
fun ExerciseView(
    ex: Exercise,
    eastern: Boolean,
    speaker: Speaker,
    allowRetry: Boolean,
    onDone: (Boolean) -> Unit
) {
    val wrong = remember { mutableStateListOf<Int>() }
    var solved by remember { mutableStateOf(false) }
    var firstTry by remember { mutableStateOf(true) }
    val say = ex.speak.ifBlank { ex.prompt }
    val canHear = speaker.hasClip(say)

    LaunchedEffect(Unit) { speaker.speak(say) }
    LaunchedEffect(solved) {
        if (solved) {
            delay(1100)
            onDone(firstTry)
        }
    }

    fun pick(i: Int) {
        if (solved) return
        if (i == ex.answer) {
            solved = true
            speaker.say("praise")
        } else if (allowRetry) {
            firstTry = false
            wrong.add(i)
            speaker.say("retry")
        } else {
            onDone(false)
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Gap(8)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                digits(ex.prompt, eastern),
                Modifier.weight(1f, fill = false),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Ink
            )
            TextButton(onClick = { speaker.speak(say) }) { Text("🔊", fontSize = 28.sp) }
        }
        Gap(8)
        if (ex.type == "listen") {
            if (canHear) {
                BigButton("🔊  اسمع", color = Orange) { speaker.speak(say) }
            } else {
                // Recording not ready yet: show the word so the question can still be answered.
                Text(
                    Speaker.clean(say).trimEnd('.', '،'),
                    fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Orange, textAlign = TextAlign.Center
                )
            }
            Gap(8)
        }
        if (!canHear) {
            Text("🔇 صوت هذا السؤال قيد التجهيز", fontSize = 14.sp, color = Color.Gray)
            Gap(4)
        }
        if (ex.show.isNotBlank()) {
            val size = when {
                ex.show.length <= 4 -> 96.sp
                ex.show.length <= 16 -> 54.sp
                else -> 38.sp
            }
            Text(digits(ex.show, eastern), fontSize = size, textAlign = TextAlign.Center, color = Ink)
        }
        if (ex.type == "count" && ex.count > 0) {
            (0 until ex.count).chunked(5).forEach { row ->
                Text(ex.countEmoji.repeat(row.size), fontSize = 40.sp, letterSpacing = 4.sp)
            }
        }
        Gap(16)
        Grid2(ex.options.indices.toList()) { i ->
            val isWrong = i in wrong
            val isRight = solved && i == ex.answer
            val bg = when {
                isRight -> Green
                isWrong -> WrongBg
                else -> Color.White
            }
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 84.dp)
                    .clip(shape)
                    .background(bg)
                    .border(2.dp, if (isRight) Green else Line, shape)
                    .clickable(enabled = !solved && !isWrong) { pick(i) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val opt = ex.options[i]
                Text(
                    digits(opt, eastern),
                    fontSize = if (opt.length <= 3) 40.sp else 22.sp,
                    color = if (isRight) Color.White else Ink,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (solved) {
            Text("أحسنت! 🌟", fontSize = 26.sp, color = Green, fontWeight = FontWeight.Bold)
        } else if (wrong.isNotEmpty()) {
            Text("حاول مرة أخرى 💪", fontSize = 20.sp, color = Orange, fontWeight = FontWeight.Bold)
        }
        Gap(24)
    }
}

// ------------------------------------------------------------------ subject map

@Composable
fun SubjectScreen(
    p: Profile,
    subject: Subject,
    eastern: Boolean,
    onBack: () -> Unit,
    onOpen: (Lesson) -> Unit
) {
    val lessons = subject.allLessons
    val placed = p.placed[subject.id] ?: 1
    val firstOfPlaced = lessons.indexOfFirst { it.level == placed }
    fun unlocked(i: Int): Boolean =
        lessons[i].level < placed || i == 0 || i == firstOfPlaced || (p.stars[lessons[i - 1].id] ?: 0) > 0

    Column(Modifier.fillMaxSize()) {
        TopBar("${subject.icon} ${subject.title}", onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            subject.levels.forEach { lv ->
                item(key = "h${lv.level}") {
                    Text(
                        lv.title,
                        Modifier.padding(top = 14.dp, bottom = 8.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange
                    )
                }
                items(lv.lessons, key = { it.id }) { l ->
                    val i = lessons.indexOf(l)
                    val open = unlocked(i)
                    val stars = p.stars[l.id] ?: 0
                    val shape = RoundedCornerShape(18.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(shape)
                            .background(if (open) Color.White else Color(0xFFF1ECE2))
                            .border(1.dp, Line, shape)
                            .clickable(enabled = open) { onOpen(l) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (open) l.icon else "🔒", fontSize = 32.sp)
                        Text(
                            digits(l.title, eastern),
                            Modifier.weight(1f).padding(horizontal = 12.dp),
                            fontSize = 19.sp,
                            color = if (open) Ink else Color.Gray
                        )
                        if (stars > 0) Text(starsText(stars), fontSize = 16.sp)
                    }
                }
            }
            item { Gap(24) }
        }
    }
}

// ------------------------------------------------------------------ lesson

fun starsFor(correct: Int, total: Int): Int {
    if (total == 0) return 3
    val r = correct.toFloat() / total
    return when {
        r >= 0.9f -> 3
        r >= 0.6f -> 2
        else -> 1
    }
}

@Composable
fun LessonScreen(
    lesson: Lesson,
    next: Lesson?,
    eastern: Boolean,
    speaker: Speaker,
    onBack: () -> Unit,
    onFinished: (Int) -> Unit,
    onNext: (Lesson) -> Unit
) {
    var step by remember(lesson.id) { mutableIntStateOf(0) }
    var correct by remember(lesson.id) { mutableIntStateOf(0) }
    val n = lesson.exercises.size

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        TopBar(digits(lesson.title, eastern), onBack, onSpeak = {
            when {
                step == 0 -> speaker.speak(lesson.introSpeak)
                step <= n -> lesson.exercises[step - 1].let { speaker.speak(it.speak.ifBlank { it.prompt }) }
            }
        })
        LinearProgressIndicator(
            progress = { step.toFloat() / (n + 1) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = Orange,
            trackColor = Soft
        )
        Gap(8)
        when {
            step == 0 -> {
                LaunchedEffect(lesson.id) { speaker.speak(lesson.introSpeak) }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Gap(16)
                    val big = lesson.introBig
                    Text(
                        digits(big, eastern),
                        fontSize = if (big.length <= 2) 120.sp else if (big.length <= 12) 56.sp else 40.sp,
                        color = Green,
                        textAlign = TextAlign.Center
                    )
                    Gap(12)
                    Text(
                        digits(lesson.introText, eastern),
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        color = Ink,
                        lineHeight = 36.sp
                    )
                    Gap(28)
                    BigButton("يلا نتمرّن ▶", Modifier.fillMaxWidth()) { step = 1 }
                }
            }
            step <= n -> {
                key(lesson.id, step) {
                    ExerciseView(lesson.exercises[step - 1], eastern, speaker, allowRetry = true) { ok ->
                        if (ok) correct++
                        step++
                    }
                }
            }
            else -> {
                val stars = starsFor(correct, n)
                LaunchedEffect(lesson.id) {
                    onFinished(stars)
                    speaker.say(if (stars == 3) "lesson_done_3" else "lesson_done")
                }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Gap(24)
                    Text("🎉", fontSize = 90.sp)
                    Text(starsText(stars), fontSize = 44.sp)
                    Gap(8)
                    Text(
                        if (stars == 3) "ممتاز! أنت بطل" else "أحسنت! أنهيت الدرس",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                    if (stars < 3) {
                        Text("تستطيع أن تعيد الدرس لتجمع نجوماً أكثر", fontSize = 18.sp, color = Ink, textAlign = TextAlign.Center)
                    }
                    Gap(28)
                    if (next != null) {
                        BigButton("الدرس التالي ▶", Modifier.fillMaxWidth()) { onNext(next) }
                        Gap(12)
                    }
                    BigButton("🔁 أعيد الدرس", Modifier.fillMaxWidth(), color = Orange) {
                        correct = 0
                        step = 0
                    }
                    Gap(12)
                    BigButton("🗺️ رجوع إلى الدروس", Modifier.fillMaxWidth(), color = Color(0xFF6B8F7E), onClick = onBack)
                    Gap(24)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ placement test

@Composable
fun PlacementScreen(
    content: Content,
    eastern: Boolean,
    speaker: Speaker,
    onBack: () -> Unit,
    onFinish: (Map<String, Int>) -> Unit
) {
    val subjects = content.subjects
    var sIdx by remember { mutableIntStateOf(0) }
    var stage by remember { mutableIntStateOf(0) }   // 0 = intro, 1 = level-1 questions, 2 = level-2 questions
    var q by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val results = remember { mutableStateMapOf<String, Int>() }

    fun finishSubject(subjectId: String, level: Int) {
        results[subjectId] = level
        q = 0
        score = 0
        if (sIdx + 1 < subjects.size) {
            sIdx++
            stage = 0
        } else {
            finished = true
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        TopBar("نتعرّف على مستواك 🎯", onBack)
        if (finished) {
            LaunchedEffect(Unit) { speaker.say("placement_done") }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Gap(40)
                Text("🎉", fontSize = 96.sp)
                Text("رائع! جهّزنا لك الدروس المناسبة", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Green, textAlign = TextAlign.Center)
                Gap(28)
                BigButton("إلى الدروس ▶", Modifier.fillMaxWidth()) { onFinish(results.toMap()) }
            }
        } else {
            PlacementStep(
                content, subjects[sIdx], sIdx, stage, q, eastern, speaker,
                onStart = { stage = 1 },
                onAnswer = { ok ->
                    val qs = content.placement[subjects[sIdx].id].orEmpty().filter { it.level == stage }.take(3)
                    val newScore = score + if (ok) 1 else 0
                    if (qs.isEmpty() || q + 1 >= qs.size) {
                        val passed = qs.isNotEmpty() && newScore * 3 >= qs.size * 2
                        val sid = subjects[sIdx].id
                        if (stage == 1) {
                            if (passed) {
                                stage = 2
                                q = 0
                                score = 0
                            } else {
                                finishSubject(sid, 1)
                            }
                        } else {
                            finishSubject(sid, if (passed) 3 else 2)
                        }
                    } else {
                        score = newScore
                        q++
                    }
                }
            )
        }
    }
}

@Composable
private fun PlacementStep(
    content: Content,
    s: Subject,
    sIdx: Int,
    stage: Int,
    q: Int,
    eastern: Boolean,
    speaker: Speaker,
    onStart: () -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        val qs = content.placement[s.id].orEmpty().filter { it.level == stage }.take(3)
        when {
            stage == 0 -> {
                LaunchedEffect(sIdx) { speaker.say("placement_start_${s.id}") }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Gap(40)
                    Text(s.icon, fontSize = 96.sp)
                    Text("الآن: ${s.title}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Green)
                    Gap(8)
                    Text("أجب على قدر ما تعرف، ولا بأس إن لم تعرف 😊", fontSize = 19.sp, textAlign = TextAlign.Center, color = Ink)
                    Gap(28)
                    BigButton("ابدأ ▶", Modifier.fillMaxWidth(), onClick = onStart)
                }
            }
            qs.isEmpty() -> {
                LaunchedEffect(sIdx, stage) { onAnswer(false) }
            }
            else -> {
                key(sIdx, stage, q) {
                    ExerciseView(qs[q.coerceIn(0, qs.size - 1)].exercise, eastern, speaker, allowRetry = false, onDone = onAnswer)
                }
            }
        }
    }
}
