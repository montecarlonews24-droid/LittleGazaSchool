package com.montecarlo.littlegaza

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class Screen {
    object Profiles : Screen()
    object NewProfile : Screen()
    data class Home(val pid: String) : Screen()
    data class Placement(val pid: String) : Screen()
    data class SubjectMap(val pid: String, val sid: String) : Screen()
    data class LessonRun(val pid: String, val lid: String) : Screen()
    object Calm : Screen()
    object Breathing : Screen()
    object Feelings : Screen()
    data class StoryRead(val index: Int) : Screen()
    object Parent : Screen()
    object Help : Screen()
}

@Composable
fun LittleGazaApp(content: Content, store: Store, speaker: Speaker) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val stack = remember { mutableStateListOf<Screen>(Screen.Profiles) }
    var profiles by remember { mutableStateOf(store.profiles()) }
    var eastern by remember { mutableStateOf(store.easternDigits) }

    fun push(s: Screen) {
        speaker.stop(); stack.add(s)
    }

    fun pop() {
        speaker.stop(); if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun replace(s: Screen) {
        speaker.stop(); stack[stack.lastIndex] = s
    }

    fun save(p: Profile) {
        store.upsert(p); profiles = store.profiles()
    }

    fun profile(id: String): Profile? = profiles.firstOrNull { it.id == id }

    BackHandler(enabled = stack.size > 1) { pop() }

    LgTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = Sand) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    when (val s = stack.last()) {
                        is Screen.Profiles -> ProfilesScreen(
                            profiles, speaker,
                            onPick = { push(Screen.Home(it.id)) },
                            onNew = { push(Screen.NewProfile) },
                            onCalm = { push(Screen.Calm) },
                            onParent = { push(Screen.Parent) },
                            onShare = { shareApp(ctx, scope) },
                            onHelp = { push(Screen.Help) }
                        )

                        is Screen.NewProfile -> NewProfileScreen(onBack = { pop() }) { name, avatar ->
                            val p = Profile(
                                id = System.currentTimeMillis().toString(),
                                name = name, avatar = avatar,
                                placementDone = false, placed = emptyMap(), stars = emptyMap()
                            )
                            save(p)
                            replace(Screen.Placement(p.id))
                        }

                        is Screen.Home -> {
                            val p = profile(s.pid)
                            if (p == null) LaunchedEffect(s) { pop() } else HomeScreen(
                                p, content, eastern, speaker,
                                onBack = { pop() },
                                onPlacement = { push(Screen.Placement(p.id)) },
                                onSubject = { push(Screen.SubjectMap(p.id, it)) },
                                onCalm = { push(Screen.Calm) }
                            )
                        }

                        is Screen.Placement -> {
                            val p = profile(s.pid)
                            if (p == null) LaunchedEffect(s) { pop() } else PlacementScreen(
                                content, eastern, speaker,
                                onBack = { pop() },
                                onFinish = { placed ->
                                    save(p.copy(placementDone = true, placed = placed))
                                    if (stack.size >= 2 && stack[stack.size - 2] == Screen.Home(p.id)) pop()
                                    else replace(Screen.Home(p.id))
                                }
                            )
                        }

                        is Screen.SubjectMap -> {
                            val p = profile(s.pid)
                            if (p == null) LaunchedEffect(s) { pop() } else SubjectScreen(
                                p, content.subject(s.sid), eastern,
                                onBack = { pop() },
                                onOpen = { push(Screen.LessonRun(p.id, it.id)) }
                            )
                        }

                        is Screen.LessonRun -> {
                            val p = profile(s.pid)
                            val lesson = content.lesson(s.lid)
                            if (p == null || lesson == null) LaunchedEffect(s) { pop() } else {
                                val list = content.subject(lesson.subjectId).allLessons
                                val next = list.getOrNull(list.indexOf(lesson) + 1)
                                LessonScreen(
                                    lesson, next, eastern, speaker,
                                    onBack = { pop() },
                                    onFinished = { stars ->
                                        val fresh = profile(p.id)
                                        if (fresh != null && stars > (fresh.stars[lesson.id] ?: 0)) {
                                            save(fresh.copy(stars = fresh.stars + (lesson.id to stars)))
                                        }
                                    },
                                    onNext = { replace(Screen.LessonRun(p.id, it.id)) }
                                )
                            }
                        }

                        is Screen.Calm -> CalmScreen(
                            content.stories, speaker,
                            onBack = { pop() },
                            onBreathing = { push(Screen.Breathing) },
                            onFeelings = { push(Screen.Feelings) },
                            onStory = { push(Screen.StoryRead(it)) }
                        )

                        is Screen.Breathing -> BreathingScreen(speaker, onBack = { pop() })
                        is Screen.Feelings -> FeelingsScreen(speaker, onBack = { pop() })
                        is Screen.StoryRead -> StoryScreen(content.stories[s.index], speaker, onBack = { pop() })

                        is Screen.Parent -> ParentScreen(
                            store, profiles, content, eastern, speaker,
                            onBack = { pop() },
                            onSave = { save(it) },
                            onDelete = { store.delete(it); profiles = store.profiles() },
                            onEastern = { store.easternDigits = it; eastern = it },
                            onHelp = { push(Screen.Help) }
                        )

                        is Screen.Help -> HelpScreen(onBack = { pop() }, onOpen = { openLink(ctx, it) })
                    }
                }
            }
        }
    }
}

fun openLink(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(ctx, "لا يوجد متصفح على هذا الجهاز", Toast.LENGTH_SHORT).show()
    }
}

/** Sends this app's own APK to another phone (Quick Share, Bluetooth, WhatsApp…), so it spreads without internet. */
fun shareApp(ctx: Context, scope: CoroutineScope) {
    scope.launch {
        try {
            val uri = withContext(Dispatchers.IO) {
                val src = File(ctx.applicationInfo.sourceDir)
                val out = File(ctx.cacheDir, "LittleGazaSchool.apk")
                src.copyTo(out, overwrite = true)
                FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", out)
            }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, "شارك مدرسة غزة الصغيرة"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "تعذّرت المشاركة، حاول مرة أخرى", Toast.LENGTH_SHORT).show()
        }
    }
}
