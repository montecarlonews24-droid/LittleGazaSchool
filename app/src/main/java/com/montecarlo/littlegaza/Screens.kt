package com.montecarlo.littlegaza

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val AVATARS = listOf("🐱", "🐶", "🐰", "🦁", "🐢", "🐦", "🦋", "🐠", "🌻", "⭐")

private fun scrollCol() = Modifier.fillMaxSize()

// ------------------------------------------------------------------ who is learning?

@Composable
fun ProfilesScreen(
    profiles: List<Profile>,
    speaker: Speaker,
    onPick: (Profile) -> Unit,
    onNew: () -> Unit,
    onCalm: () -> Unit,
    onParent: () -> Unit,
    onShare: () -> Unit,
    onHelp: () -> Unit
) {
    LaunchedEffect(Unit) { speaker.say("welcome") }
    Column(
        scrollCol().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏫", fontSize = 64.sp)
        Text("مدرسة غزة الصغيرة", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Green)
        Text("من يريد أن يتعلّم اليوم؟", fontSize = 20.sp, color = Ink)
        Gap(20)
        Grid2(profiles) { p -> Tile(p.avatar, p.name, Modifier.weight(1f)) { onPick(p) } }
        if (profiles.size < 5) {
            BigButton("➕ طفل جديد", Modifier.fillMaxWidth(), color = Orange, onClick = onNew)
            Gap(20)
        }
        Grid2(
            listOf(
                Triple("🌿", "ركن الهدوء", onCalm),
                Triple("👪", "للأهل والمعلمين", onParent),
                Triple("📲", "شارك التطبيق", onShare),
                Triple("🤝", "كيف تساعد", onHelp)
            )
        ) { (e, t, f) -> Tile(e, t, Modifier.weight(1f), bg = Soft, onClick = f) }
        Gap(8)
        Text("يعمل بدون إنترنت • مجاني • بدون إعلانات", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun NewProfileScreen(onBack: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(AVATARS[0]) }
    Column(scrollCol().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        TopBar("طفل جديد", onBack)
        Gap(8)
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 15) name = it },
            label = { Text("الاسم أو اللقب") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 22.sp),
            modifier = Modifier.fillMaxWidth()
        )
        Text("لا تكتب الاسم الكامل، يكفي اسم قصير أو لقب", fontSize = 14.sp, color = Color.Gray)
        Gap(16)
        Text("اختر صورتك", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Green)
        Gap(8)
        AVATARS.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { a ->
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (a == avatar) Orange else Color.White)
                            .border(2.dp, if (a == avatar) Orange else Line, CircleShape)
                            .clickable { avatar = a },
                        contentAlignment = Alignment.Center
                    ) { Text(a, fontSize = 32.sp) }
                }
            }
            Gap(10)
        }
        Gap(20)
        BigButton("يلا نبدأ ▶", Modifier.fillMaxWidth()) { onCreate(name.trim().ifEmpty { "بطل" }, avatar) }
        Gap(24)
    }
}

// ------------------------------------------------------------------ home

@Composable
fun HomeScreen(
    p: Profile,
    content: Content,
    eastern: Boolean,
    speaker: Speaker,
    onBack: () -> Unit,
    onPlacement: () -> Unit,
    onSubject: (String) -> Unit,
    onCalm: () -> Unit
) {
    LaunchedEffect(p.id) { speaker.say("hello") }
    Column(scrollCol().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        TopBar("${p.avatar} أهلاً يا ${p.name}", onBack)
        Gap(8)
        if (!p.placementDone) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFFCEBCF)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯 لنتعرّف على مستواك أولاً", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("أسئلة قليلة لنختار لك الدروس المناسبة", fontSize = 16.sp, color = Ink)
                Gap(10)
                BigButton("ابدأ", Modifier.fillMaxWidth(), color = Orange, onClick = onPlacement)
            }
            Gap(16)
        }
        content.subjects.forEach { s ->
            val total = s.allLessons.size
            val done = s.allLessons.count { (p.stars[it.id] ?: 0) > 0 }
            val stars = s.allLessons.sumOf { p.stars[it.id] ?: 0 }
            Tile(
                s.icon, s.title, Modifier.fillMaxWidth(),
                subtitle = digits("أنهيت $done من $total درساً • ⭐ $stars", eastern)
            ) { onSubject(s.id) }
            Gap(12)
        }
        Tile("🌿", "ركن الهدوء", Modifier.fillMaxWidth(), subtitle = "تنفّس، قصص، مشاعر", bg = Soft, onClick = onCalm)
        Gap(24)
    }
}

// ------------------------------------------------------------------ calm corner

@Composable
fun CalmScreen(
    stories: List<Story>,
    speaker: Speaker,
    onBack: () -> Unit,
    onBreathing: () -> Unit,
    onFeelings: () -> Unit,
    onStory: (Int) -> Unit
) {
    LaunchedEffect(Unit) { speaker.say("calm") }
    Column(scrollCol().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        TopBar("ركن الهدوء 🌿", onBack)
        Gap(8)
        Grid2(listOf(Triple("🌬️", "نتنفّس معاً", onBreathing), Triple("💛", "كيف تشعر اليوم؟", onFeelings))) { (e, t, f) ->
            Tile(e, t, Modifier.weight(1f), bg = Soft, onClick = f)
        }
        Text("قصص قصيرة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange)
        Gap(8)
        Grid2(stories.indices.toList()) { i ->
            Tile(stories[i].icon, stories[i].title, Modifier.weight(1f)) { onStory(i) }
        }
    }
}

@Composable
fun BreathingScreen(speaker: Speaker, onBack: () -> Unit) {
    var inhale by remember { mutableStateOf(true) }
    var rounds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (rounds < 5) {
            inhale = true
            speaker.say("breathe_in")
            delay(4000)
            inhale = false
            speaker.say("breathe_out")
            delay(4000)
            rounds++
        }
        speaker.say("breathe_done")
    }
    val scale by animateFloatAsState(if (inhale && rounds < 5) 1f else 0.55f, tween(4000), label = "breath")
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("نتنفّس معاً 🌬️", onBack)
        Gap(40)
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(260.dp).scale(scale).clip(CircleShape).background(Soft))
            Text(
                when {
                    rounds >= 5 -> "أحسنت 💚"
                    inhale -> "شهيق…"
                    else -> "زفير…"
                },
                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Green
            )
        }
        Gap(24)
        Text(
            if (rounds >= 5) "أنت الآن أهدأ. أحسنت!" else "تابع الدائرة: تكبر مع الشهيق، وتصغر مع الزفير",
            fontSize = 19.sp, textAlign = TextAlign.Center, color = Ink
        )
        Gap(24)
        if (rounds >= 5) BigButton("رجوع", Modifier.fillMaxWidth(), onClick = onBack)
    }
}

private val FEELINGS = listOf(
    Triple("😊", "مبسوط", "رَائِع! الفَرَحُ جَمِيل، شَارِكْهُ مَعَ مَنْ تُحِبّ."),
    Triple("😢", "حزين", "لَا بَأْسَ أَنْ تَحْزَن. خُذْ نَفَسًا عَمِيقًا، وَاحْكِ لِشَخْصٍ كَبِيرٍ تَثِقُ بِه."),
    Triple("😠", "غاضب", "الغَضَبُ شُعُورٌ طَبِيعِيّ. لِنَعُدَّ مَعًا بِبُطْءٍ مِنْ وَاحِدٍ إِلَى خَمْسَة، ثُمَّ نَتَنَفَّس."),
    Triple("😨", "خائف", "أَنْتَ لَسْتَ وَحْدَك. اِقْتَرِبْ مِنْ شَخْصٍ كَبِيرٍ تُحِبُّه، وَجَرِّبْ تَمْرِينَ التَّنَفُّس."),
    Triple("😴", "تعبان", "جِسْمُكَ يَحْتَاجُ إِلَى رَاحَة. اِشْرَبْ قَلِيلًا مِنَ المَاء، وَاسْتَرِحْ قَلِيلًا.")
)

@Composable
fun FeelingsScreen(speaker: Speaker, onBack: () -> Unit) {
    var picked by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) { speaker.say("feelings") }
    Column(scrollCol().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        TopBar("كيف تشعر اليوم؟ 💛", onBack)
        Gap(8)
        Grid2(FEELINGS.indices.toList()) { i ->
            val f = FEELINGS[i]
            Tile(f.first, f.second, Modifier.weight(1f), bg = if (picked == i) Color(0xFFFCEBCF) else Color.White) {
                picked = i
                speaker.say("feel_$i")
            }
        }
        if (picked >= 0) {
            Text(
                FEELINGS[picked].third,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Soft).padding(18.dp),
                fontSize = 21.sp, color = Ink, textAlign = TextAlign.Center, lineHeight = 32.sp
            )
        }
        Gap(12)
        Text("لا نحفظ ما تختاره. هذا فقط لك 🤍", fontSize = 14.sp, color = Color.Gray)
        Gap(24)
    }
}

@Composable
fun StoryScreen(story: Story, speaker: Speaker, onBack: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val last = story.pages.size - 1
    LaunchedEffect(page) { speaker.speak(story.pages.getOrElse(page) { "" }) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("${story.icon} ${story.title}", onBack, onSpeak = { speaker.speak(story.pages.getOrElse(page) { "" }) })
        Gap(30)
        Text(story.icon, fontSize = 90.sp)
        Gap(20)
        Text(
            story.pages.getOrElse(page) { "" },
            Modifier.weight(1f),
            fontSize = 25.sp, lineHeight = 40.sp, textAlign = TextAlign.Center, color = Ink
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton("◀ السابق", Modifier.weight(1f), color = Color(0xFF6B8F7E), enabled = page > 0) { page-- }
            if (page < last) BigButton("التالي ▶", Modifier.weight(1f)) { page++ }
            else BigButton("انتهت 🌟", Modifier.weight(1f), color = Orange, onClick = onBack)
        }
    }
}

// ------------------------------------------------------------------ parents & teachers

@Composable
fun ParentScreen(
    store: Store,
    profiles: List<Profile>,
    content: Content,
    eastern: Boolean,
    speaker: Speaker,
    onBack: () -> Unit,
    onSave: (Profile) -> Unit,
    onDelete: (String) -> Unit,
    onEastern: (Boolean) -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf("") }
    val hasPin = store.pin != null

    Column(scrollCol().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        TopBar("للأهل والمعلمين 👪", onBack)
        Gap(8)
        if (!unlocked) {
            Text(
                if (hasPin) "أدخل الرقم السري" else "اختر رقماً سرياً من 4 أرقام (للكبار فقط)",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink
            )
            Gap(8)
            OutlinedTextField(
                value = pinInput,
                onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) pinInput = v },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(fontSize = 26.sp),
                modifier = Modifier.fillMaxWidth()
            )
            if (error) Text("الرقم غير صحيح", color = Color(0xFFC62828), fontSize = 16.sp)
            Gap(12)
            BigButton(if (hasPin) "دخول" else "حفظ ودخول", Modifier.fillMaxWidth(), enabled = pinInput.length == 4) {
                if (!hasPin) {
                    store.pin = pinInput; unlocked = true
                } else if (pinInput == store.pin) {
                    unlocked = true
                } else {
                    error = true; pinInput = ""
                }
            }
            Gap(12)
            Text("نسيت الرقم؟ امسح بيانات التطبيق من إعدادات الهاتف (سيُحذف تقدّم الأطفال).", fontSize = 14.sp, color = Color.Gray)
        } else {
            ParentDashboard(store, profiles, content, eastern, speaker, confirmDelete, { confirmDelete = it }, onSave, onDelete, onEastern) {
                unlocked = false; pinInput = ""
            }
        }
    }
}

@Composable
private fun ParentDashboard(
    store: Store,
    profiles: List<Profile>,
    content: Content,
    eastern: Boolean,
    speaker: Speaker,
    confirmDelete: String,
    setConfirm: (String) -> Unit,
    onSave: (Profile) -> Unit,
    onDelete: (String) -> Unit,
    onEastern: (Boolean) -> Unit,
    onLock: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (profiles.isEmpty()) Text("لا يوجد أطفال بعد.", fontSize = 18.sp, color = Ink)
        profiles.forEach { p ->
            Column(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp))
                    .background(Color.White).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(14.dp)
            ) {
                Text("${p.avatar} ${p.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
                content.subjects.forEach { s ->
                    val done = s.allLessons.count { (p.stars[it.id] ?: 0) > 0 }
                    val start = when {
                        !p.placementDone -> "لم يُجرِ اختبار المستوى"
                        (p.placed[s.id] ?: 1) >= 3 -> "تجاوز المستويين في الاختبار"
                        else -> "يبدأ من المستوى ${p.placed[s.id] ?: 1}"
                    }
                    Text(digits("${s.icon} ${s.title}: $start • أنهى $done من ${s.allLessons.size}", eastern), fontSize = 16.sp, color = Ink)
                }
                Gap(8)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("🎯 إعادة الاختبار", Modifier.weight(1f), color = Orange) { onSave(p.copy(placementDone = false)) }
                    if (confirmDelete == p.id) {
                        BigButton("تأكيد الحذف", Modifier.weight(1f), color = Color(0xFFC62828)) { onDelete(p.id); setConfirm("") }
                    } else {
                        BigButton("🗑️ حذف", Modifier.weight(1f), color = Color(0xFF8D8D8D)) { setConfirm(p.id) }
                    }
                }
            }
        }

        Gap(8)
        Text("الإعدادات", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الأرقام العربية (١٢٣) بدل (123)", Modifier.weight(1f), fontSize = 18.sp, color = Ink)
            Switch(checked = eastern, onCheckedChange = onEastern)
        }
        Text("الصوت", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange)
        Gap(4)
        Text(
            if (speaker.clipCount > 0) "🎙️ صوت مسجّل جاهز لـ ${speaker.clipCount} جملة. الباقي بصوت الهاتف."
            else "🎙️ الصوت المسجّل غير متوفّر بعد، يُستخدم صوت الهاتف.",
            fontSize = 15.sp, color = Ink
        )
        Gap(6)
        Text(
            when {
                !speaker.googleInstalled -> "🔇 للحصول على لفظ عربي صحيح، ثبّت تطبيق «Speech Services by Google» (مرة واحدة، يحتاج إنترنت)."
                !speaker.arabicAvailable -> "🔇 الصوت العربي غير مثبّت. اضغط الزر بالأسفل ونزّل «العربية» (مرة واحدة، يحتاج إنترنت)."
                else -> "🔊 الصوت العربي يعمل عبر محرّك Google."
            },
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(if (speaker.arabicAvailable && speaker.googleInstalled) Soft else Color(0xFFFCEBCF)).padding(12.dp),
            fontSize = 15.sp, color = Ink
        )
        Gap(8)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        BigButton(
            if (speaker.googleInstalled) "⬇️ تنزيل الصوت العربي" else "⬇️ تثبيت محرّك صوت Google",
            Modifier.fillMaxWidth(), color = Orange
        ) { Speaker.installArabicVoice(ctx) }
        Gap(8)
        BigButton("⚙️ إعدادات الصوت في الهاتف", Modifier.fillMaxWidth(), color = Color(0xFF6B8F7E)) { Speaker.openTtsSettings(ctx) }
        Gap(8)
        BigButton("🔊 جرّب الصوت", Modifier.fillMaxWidth(), color = Green) {
            speaker.say("test")
        }
        Text("إذا تغيّر الصوت بعد التنزيل، أغلق التطبيق وافتحه من جديد.", fontSize = 14.sp, color = Color.Gray)
        Gap(12)
        BigButton("تغيير الرقم السري", Modifier.fillMaxWidth(), color = Color(0xFF6B8F7E)) {
            store.pin = null; onLock()
        }
        Gap(12)
        Text("التطبيق لا يجمع أي بيانات ولا يحتاج إنترنت. كل التقدّم محفوظ على هذا الهاتف فقط.", fontSize = 14.sp, color = Color.Gray)
        Gap(24)
    }
}

// ------------------------------------------------------------------ how to help

@Composable
fun HelpScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    Column(scrollCol().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        TopBar("كيف تساعد 🤝", onBack)
        Gap(8)
        Text(
            "مدرسة غزة الصغيرة تطبيق مجاني بالكامل، بلا إعلانات ولا اشتراكات.\n\n" +
                "أفضل طريقة للمساعدة: شارك التطبيق مع الأهل والمعلمين، وأرسله لهواتف أخرى من زر «شارك التطبيق».\n\n" +
                "وإن أردت التبرّع، فتبرّع عبر الجهات الرسمية المعروفة:",
            fontSize = 18.sp, lineHeight = 28.sp, color = Ink
        )
        Gap(12)
        BigButton("الأونروا UNRWA", Modifier.fillMaxWidth()) { onOpen("https://www.unrwa.org") }
        Gap(10)
        BigButton("يونيسف فلسطين UNICEF", Modifier.fillMaxWidth()) { onOpen("https://www.unicef.org/sop") }
        Gap(10)
        BigButton("الهلال الأحمر الفلسطيني", Modifier.fillMaxWidth()) { onOpen("https://www.palestinercs.org") }
        Gap(12)
        Text("الروابط تفتح في المتصفح وتحتاج إنترنت. التطبيق نفسه لا يجمع أي أموال.", fontSize = 14.sp, color = Color.Gray)
        Gap(24)
    }
}
