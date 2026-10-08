package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.sin
import kotlin.random.Random

/**
 * الألوان المعتمدة في اللعبة (أحمر، أزرق، أخضر، أصفر، برتقالي).
 * كل لون يحتوي على درجته الزاهية ودرجة تظليل لإعطاء شكل ثلاثي الأبعاد جذاب للأطفال.
 */
enum class GameColor(
    val arabicName: String,
    val primaryColor: Color,
    val darkShade: Color,
    val lightShade: Color
) {
    RED("أحمر", Color(0xFFEF4444), Color(0xFFB91C1C), Color(0xFFFCA5A5)),
    BLUE("أزرق", Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF93C5FD)),
    GREEN("أخضر", Color(0xFF22C55E), Color(0xFF15803D), Color(0xFF86EFAC)),
    YELLOW("أصفر", Color(0xFFEAB308), Color(0xFFA16207), Color(0xFFFEF08A)),
    ORANGE("برتقالي", Color(0xFFF97316), Color(0xFFC2410C), Color(0xFFFDBA74));

    companion object {
        fun random(excluding: GameColor? = null): GameColor {
            val list = entries.filter { it != excluding }
            return list.random()
        }
    }
}

/**
 * كائن الكرة الساقطة:
 * نصف القطر 30.dp حسب المطلوب في التعليمات.
 */
data class FallingBall(
    val id: Long,
    val color: GameColor?, // إذا كان null فهي كرة ذهبية
    val isGolden: Boolean = false,
    var x: Float, // إحداثي المركز الأفقي بالبكسل
    var y: Float, // إحداثي المركز الرأسي بالبكسل
    val speed: Float, // سرعة السقوط (بكسل بالثانية)
    val radiusPx: Float,
    var isCollected: Boolean = false
)

/**
 * رسائل النقاط العائمة (مثل +1 أو +2 أو -❤️)
 */
data class ScorePopup(
    val id: Long,
    val text: String,
    val color: Color,
    val x: Float,
    val y: Float,
    val startTime: Long
)

/**
 * أنواع الاهتزازات التفاعلية
 */
enum class HapticFeedbackType {
    COLOR_CHANGE,
    CORRECT_CATCH,
    GOLDEN_CATCH,
    WRONG_CATCH
}

/**
 * تفاصيل شخصية الاحتفال عند كل 20 نقطة
 */
data class CelebrationCharacter(
    val minScore: Int,
    val nameAr: String,
    val emoji: String,
    val messageAr: String,
    val badgeColor: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FlyingCupGameScreen()
            }
        }
    }
}

/**
 * الشاشة الرئيسية للعبة "الكأس الطائرة"
 */
@Composable
fun FlyingCupGameScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    // أبعاد الكرة (نصف القطر 30.dp = القطر 60.dp)
    val ballRadiusDp = 30.dp
    val ballRadiusPx = with(density) { ballRadiusDp.toPx() }

    // أبعاد الكأس
    val cupWidthDp = 130.dp
    val cupHeightDp = 75.dp
    val cupWidthPx = with(density) { cupWidthDp.toPx() }
    val cupHeightPx = with(density) { cupHeightDp.toPx() }

    // حالة اللعبة الأساسية
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }

    // لون الكأس الحالي (يبدأ بلون عشوائي)
    var currentCupColor by remember { mutableStateOf(GameColor.random()) }
    var colorChangeTimer by remember { mutableFloatStateOf(10f) }

    // مضاعف النقاط الذهبي (×2) لمدة 5 ثوانٍ
    var goldenMultiplierActive by remember { mutableStateOf(false) }
    var goldenMultiplierTimer by remember { mutableFloatStateOf(0f) }

    // موضع الكأس الأفقي (بالبكسل)
    var cupX by remember { mutableFloatStateOf(0f) }
    var targetCupX by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }
    var tiltX by remember { mutableFloatStateOf(0f) }

    // قائمة الكرات النشطة على الشاشة
    val balls = remember { mutableStateListOf<FallingBall>() }
    var nextBallId by remember { mutableLongStateOf(0L) }

    // قائمة التأثيرات العائمة
    val popups = remember { mutableStateListOf<ScorePopup>() }

    // موقت ظهور الكرة الذهبية (كل 15 ثانية)
    var goldenSpawnTimer by remember { mutableFloatStateOf(15f) }
    var ballSpawnTimer by remember { mutableFloatStateOf(0f) }

    // شخصية الاحتفال الحالية
    val celebrationCharacters = remember {
        listOf(
            CelebrationCharacter(20, "القطة ميمي", "🐱", "القطة الذكية تصفق لك!", Color(0xFFFFB74D)),
            CelebrationCharacter(40, "البطريق بيبو", "🐧", "البطريق اللطيف فخور بك!", Color(0xFF4FC3F7)),
            CelebrationCharacter(60, "الديناصور ريكس", "🦕", "الديناصور القوي يشجعك يا بطل!", Color(0xFF81C784))
        )
    }
    var activeCelebration by remember { mutableStateOf<CelebrationCharacter?>(null) }
    var celebrationTimer by remember { mutableFloatStateOf(0f) }

    // إدارة مستشعر التسارع (Accelerometer)
    var isSensorAvailable by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        isSensorAvailable = accelerometer != null

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // في الوضع العمودي، إمالة الهاتف لليمين تنتج قيماً سالبة لـ X
                    // لذلك نعكس القيمة ليتحرك الكأس لليمين بسلاسة
                    val ax = event.values[0]
                    tiltX = -ax
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accelerometer?.let {
                    sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                sensorManager?.unregisterListener(sensorListener)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // دالة إرسال التغذية الراجعة اللمسية (الاهتزاز)
    fun triggerVibration(type: HapticFeedbackType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                when (type) {
                    HapticFeedbackType.COLOR_CHANGE -> {
                        vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    HapticFeedbackType.CORRECT_CATCH -> {
                        vibrator?.vibrate(VibrationEffect.createOneShot(35, 120))
                    }
                    HapticFeedbackType.GOLDEN_CATCH -> {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 80), -1))
                    }
                    HapticFeedbackType.WRONG_CATCH -> {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 100), -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                when (type) {
                    HapticFeedbackType.COLOR_CHANGE -> vibrator?.vibrate(75)
                    HapticFeedbackType.CORRECT_CATCH -> vibrator?.vibrate(35)
                    HapticFeedbackType.GOLDEN_CATCH -> vibrator?.vibrate(longArrayOf(0, 40, 50, 80), -1)
                    HapticFeedbackType.WRONG_CATCH -> vibrator?.vibrate(longArrayOf(0, 80, 50, 100), -1)
                }
            }
        } catch (_: Exception) {
            // صمام أمان في حال عدم وجود هزاز في الجهاز
        }
    }

    // إعادة بدء اللعبة
    fun restartGame(screenWidthPx: Float) {
        score = 0
        hearts = 3
        isGameOver = false
        currentCupColor = GameColor.random()
        colorChangeTimer = 10f
        goldenMultiplierActive = false
        goldenMultiplierTimer = 0f
        goldenSpawnTimer = 15f
        ballSpawnTimer = 0f
        activeCelebration = null
        celebrationTimer = 0f
        cupX = screenWidthPx / 2f
        targetCupX = screenWidthPx / 2f
        isTouching = false
        balls.clear()
        popups.clear()
    }

    // الشاشة الحاوية مع قراءة الأبعاد الفعلية
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBAE6FD), // سماء زرقاء فاتحة في الأعلى
                        Color(0xFFE0F2FE),
                        Color(0xFFF0F9FF)  // تدرج لطيف ومريح لعيون الأطفال
                    )
                )
            )
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // ضبط موضع الكأس الأولي عند بدء العرض
        LaunchedEffect(screenWidthPx) {
            if (cupX == 0f && screenWidthPx > 0f) {
                cupX = screenWidthPx / 2f
                targetCupX = screenWidthPx / 2f
            }
        }

        // حلقة اللعبة الأساسية (Game Loop) بمعدل 60 إطاراً في الثانية
        LaunchedEffect(isGameOver) {
            if (isGameOver) return@LaunchedEffect

            var lastFrameTimeNanos = 0L

            while (!isGameOver) {
                withFrameMillis { currentFrameMillis ->
                    val currentNanos = currentFrameMillis * 1_000_000L
                    if (lastFrameTimeNanos == 0L) {
                        lastFrameTimeNanos = currentNanos
                    }
                    val dt = ((currentNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastFrameTimeNanos = currentNanos

                    // 1. تحديث موضع الكأس (باللمس المباشر أو بمستشعر التسارع)
                    if (isTouching) {
                        // عند اللمس أو التوجيه بالأصبع، يتبع الكأس الأصبع بسلاسة وفورية
                        val touchLerpSpeed = (18f * dt).coerceAtMost(1f)
                        cupX += (targetCupX - cupX) * touchLerpSpeed
                    } else {
                        // عند عدم اللمس، يتحرك الكأس عبر إمالة الهاتف بمستشعر التسارع
                        val tiltSpeed = tiltX * 180f * dt * (screenWidthPx / 360f).coerceAtLeast(1f)
                        cupX += tiltSpeed
                    }
                    cupX = cupX.coerceIn(cupWidthPx / 2f, screenWidthPx - cupWidthPx / 2f)

                    // 2. تحديث موقت تغيير لون الكأس (كل 10 ثوانٍ)
                    colorChangeTimer -= dt
                    if (colorChangeTimer <= 0f) {
                        currentCupColor = GameColor.random(excluding = currentCupColor)
                        colorChangeTimer = 10f
                        triggerVibration(HapticFeedbackType.COLOR_CHANGE)
                        popups.add(
                            ScorePopup(
                                id = System.currentTimeMillis(),
                                text = "تغير اللون إلى ${currentCupColor.arabicName}! 🎨",
                                color = currentCupColor.primaryColor,
                                x = cupX,
                                y = screenHeightPx - cupHeightPx - 100f,
                                startTime = System.currentTimeMillis()
                            )
                        )
                    }

                    // 3. تحديث مضاعف النقاط الذهبي (5 ثوانٍ)
                    if (goldenMultiplierActive) {
                        goldenMultiplierTimer -= dt
                        if (goldenMultiplierTimer <= 0f) {
                            goldenMultiplierActive = false
                            goldenMultiplierTimer = 0f
                        }
                    }

                    // 4. موقت شخصيات الاحتفال
                    if (activeCelebration != null) {
                        celebrationTimer -= dt
                        if (celebrationTimer <= 0f) {
                            activeCelebration = null
                        }
                    }

                    // 5. موقت ظهور الكرة الذهبية (كل 15 ثانية)
                    goldenSpawnTimer -= dt
                    val shouldSpawnGolden = goldenSpawnTimer <= 0f
                    if (shouldSpawnGolden) {
                        goldenSpawnTimer = 15f
                        val margin = ballRadiusPx * 2
                        val randomX = Random.nextFloat() * (screenWidthPx - margin * 2) + margin
                        balls.add(
                            FallingBall(
                                id = nextBallId++,
                                color = null,
                                isGolden = true,
                                x = randomX,
                                y = -ballRadiusPx,
                                speed = (200f + Random.nextFloat() * 60f) * density.density,
                                radiusPx = ballRadiusPx
                            )
                        )
                    }

                    // 6. توليد الكرات الملونة العادية بشكل عشوائي وبسرعات متفاوتة
                    ballSpawnTimer += dt
                    val spawnInterval = 1.3f // كرة كل ثانية وثلث تقريباً
                    if (ballSpawnTimer >= spawnInterval) {
                        ballSpawnTimer = 0f
                        val margin = ballRadiusPx * 2
                        val randomX = Random.nextFloat() * (screenWidthPx - margin * 2) + margin
                        // احتمال أكبر قليلاً لظهور لون مطابق لتشجيع الطفل
                        val chosenColor = if (Random.nextFloat() < 0.4f) {
                            currentCupColor
                        } else {
                            GameColor.entries.random()
                        }
                        val speedPx = (160f + Random.nextFloat() * 120f) * density.density

                        balls.add(
                            FallingBall(
                                id = nextBallId++,
                                color = chosenColor,
                                isGolden = false,
                                x = randomX,
                                y = -ballRadiusPx,
                                speed = speedPx,
                                radiusPx = ballRadiusPx
                            )
                        )
                    }

                    // 7. تحريك الكرات وفحص الاصطدام بالكأس
                    val cupTopY = screenHeightPx - cupHeightPx - 25f * density.density
                    val cupLeft = cupX - (cupWidthPx * 0.48f)
                    val cupRight = cupX + (cupWidthPx * 0.48f)

                    val iterator = balls.iterator()
                    while (iterator.hasNext()) {
                        val ball = iterator.next()
                        ball.y += ball.speed * dt

                        // فحص ملامسة الكأس
                        val hitsCupVertical = (ball.y + ball.radiusPx >= cupTopY) && (ball.y - ball.radiusPx <= cupTopY + 30f * density.density)
                        val hitsCupHorizontal = ball.x >= cupLeft && ball.x <= cupRight

                        if (hitsCupVertical && hitsCupHorizontal && !ball.isCollected) {
                            ball.isCollected = true

                            if (ball.isGolden) {
                                // التقاط الكرة الذهبية: تفعيل مضاعف 2X لمدة 5 ثوانٍ
                                goldenMultiplierActive = true
                                goldenMultiplierTimer = 5f
                                triggerVibration(HapticFeedbackType.GOLDEN_CATCH)
                                popups.add(
                                    ScorePopup(
                                        id = System.currentTimeMillis(),
                                        text = "🌟 مضاعف 2X مفعل!",
                                        color = Color(0xFFF59E0B),
                                        x = ball.x,
                                        y = cupTopY - 40f,
                                        startTime = System.currentTimeMillis()
                                    )
                                )
                            } else if (ball.color == currentCupColor) {
                                // التقاط كرة مطابقة: نقطة + مضاعف إن وجد
                                val pointsToAdd = if (goldenMultiplierActive) 2 else 1
                                val oldScore = score
                                score += pointsToAdd
                                triggerVibration(HapticFeedbackType.CORRECT_CATCH)

                                val popupText = if (goldenMultiplierActive) "+2 🌟" else "+1 ⭐"
                                popups.add(
                                    ScorePopup(
                                        id = System.currentTimeMillis(),
                                        text = popupText,
                                        color = Color(0xFF16A34A),
                                        x = ball.x,
                                        y = cupTopY - 30f,
                                        startTime = System.currentTimeMillis()
                                    )
                                )

                                // فحص وصول الطفل إلى عتبة 20 نقطة لإظهار شخصية الاحتفال
                                val oldMilestone = oldScore / 20
                                val newMilestone = score / 20
                                if (newMilestone > oldMilestone) {
                                    val charIndex = (newMilestone - 1).coerceAtMost(celebrationCharacters.size - 1)
                                    activeCelebration = celebrationCharacters[charIndex]
                                    celebrationTimer = 4.5f
                                }
                            } else {
                                // التقاط كرة بلون مختلف: خسارة قلب واحد
                                hearts = (hearts - 1).coerceAtLeast(0)
                                triggerVibration(HapticFeedbackType.WRONG_CATCH)
                                popups.add(
                                    ScorePopup(
                                        id = System.currentTimeMillis(),
                                        text = "-💔 انتبه للون!",
                                        color = Color(0xFFDC2626),
                                        x = ball.x,
                                        y = cupTopY - 30f,
                                        startTime = System.currentTimeMillis()
                                    )
                                )

                                if (hearts <= 0) {
                                    isGameOver = true
                                }
                            }
                            iterator.remove()
                        } else if (ball.y - ball.radiusPx > screenHeightPx) {
                            // الكرة سقطت دون التقاط: لا يفقد الطفل قلباً حسب القواعد
                            iterator.remove()
                        }
                    }

                    // 8. تنظيف رسائل النقاط العائمة القديمة
                    val now = System.currentTimeMillis()
                    popups.removeAll { now - it.startTime > 1200 }
                }
            }
        }

        // خلفية السحب الكرتونية المتحركة
        AnimatedCloudsBackground(screenWidthPx = screenWidthPx, screenHeightPx = screenHeightPx)

        // رسم الكرات الساقطة
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            balls.forEach { ball ->
                if (!ball.isCollected) {
                    drawKidBall(ball)
                }
            }
        }

        // الكأس الطائرة في الأسفل مع إمكانية التوجيه باللمس المباشر أو السحب
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenWidthPx) {
                    // دعم النقر لتوجيه الكأس مباشرة إلى مكان اللمس
                    detectTapGestures(
                        onPress = { offset ->
                            isTouching = true
                            targetCupX = offset.x.coerceIn(cupWidthPx / 2f, screenWidthPx - cupWidthPx / 2f)
                            tryAwaitRelease()
                            isTouching = false
                        },
                        onTap = { offset ->
                            targetCupX = offset.x.coerceIn(cupWidthPx / 2f, screenWidthPx - cupWidthPx / 2f)
                        }
                    )
                }
                .pointerInput(screenWidthPx) {
                    // دعم السحب المستمر لتوجيه الكأس بإصبع الطفل
                    detectDragGestures(
                        onDragStart = { offset ->
                            isTouching = true
                            targetCupX = offset.x.coerceIn(cupWidthPx / 2f, screenWidthPx - cupWidthPx / 2f)
                        },
                        onDragEnd = {
                            isTouching = false
                        },
                        onDragCancel = {
                            isTouching = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            isTouching = true
                            targetCupX = change.position.x.coerceIn(cupWidthPx / 2f, screenWidthPx - cupWidthPx / 2f)
                        }
                    )
                }
        ) {
            // رسم الكأس الطائرة
            FlyingCupComponent(
                cupX = cupX,
                cupY = screenHeightPx - cupHeightPx - 25f * density.density,
                widthPx = cupWidthPx,
                heightPx = cupHeightPx,
                currentColor = currentCupColor,
                isGoldenBuff = goldenMultiplierActive
            )
        }

        // النصوص والتأثيرات العائمة (+1, +2, etc.)
        popups.forEach { popup ->
            val elapsed = (System.currentTimeMillis() - popup.startTime) / 1000f
            val floatOffset = -elapsed * 70f * density.density
            val alpha = (1f - (elapsed / 1.2f)).coerceIn(0f, 1f)

            Text(
                text = popup.text,
                color = popup.color.copy(alpha = alpha),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (popup.x - 60f).toInt(),
                            y = (popup.y + floatOffset).toInt()
                        )
                    }
            )
        }

        // الشريط العلوي (النقاط، القلوب، موقت تغيير اللون، مؤشر المضاعف)
        TopStatusBar(
            score = score,
            hearts = hearts,
            colorChangeTimer = colorChangeTimer,
            currentColor = currentCupColor,
            goldenMultiplierActive = goldenMultiplierActive,
            goldenMultiplierTimer = goldenMultiplierTimer,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        )

        // بطاقة احتفال الشخصيات (تظهر عند تجاوز كل 20 نقطة)
        AnimatedVisibility(
            visible = activeCelebration != null,
            enter = slideInVertically(initialOffsetY = { -it }) + scaleIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            activeCelebration?.let { celebration ->
                CelebrationBanner(celebration = celebration)
            }
        }

        // تلميح صغير للأطفال في أسفل الشاشة
        if (!isGameOver) {
            Text(
                text = stringResource(R.string.tilt_or_drag_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF0369A1),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // شاشة "انتهت اللعبة" عند نفاذ القلوب
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            GameOverDialog(
                finalScore = score,
                onRestart = { restartGame(screenWidthPx) }
            )
        }
    }
}

/**
 * الشريط العلوي التفاعلي لعرض النتيجة والقلوب وموقت تغيير اللون
 */
@Composable
fun TopStatusBar(
    score: Int,
    hearts: Int,
    colorChangeTimer: Float,
    currentColor: GameColor,
    goldenMultiplierActive: Boolean,
    goldenMultiplierTimer: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // بطاقة النقاط
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "النقاط",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$score",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // بطاقة مؤشر اللون الحالي وموقت التغيير
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(currentColor.primaryColor)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${currentColor.arabicName} (${colorChangeTimer.toInt()}s)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            // عرض القلوب (3 قلوب)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val hasHeart = index < hearts
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "قلب $index",
                            tint = if (hasHeart) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        // شارة المضاعف الذهبي 2X إذا كانت مفعلة
        if (goldenMultiplierActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌟 ${stringResource(R.string.multiplier_active)} (${String.format("%.1f", goldenMultiplierTimer)}s)",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFB45309),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * رسم الكرة باستخدام Canvas مع تظليل وإضاءة لتشبه كرات الألعاب الكرتونية
 */
fun DrawScope.drawKidBall(ball: FallingBall) {
    val center = Offset(ball.x, ball.y)
    val radius = ball.radiusPx

    if (ball.isGolden) {
        // كرة ذهبية مشعة
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFBEB),
                    Color(0xFFFDE047),
                    Color(0xFFF59E0B),
                    Color(0xFFB45309)
                ),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                radius = radius * 1.3f
            ),
            radius = radius,
            center = center
        )
        // لمعان نجمي داخل الكرة الذهبية
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = radius * 0.22f,
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
        )
    } else {
        val baseColor = ball.color ?: GameColor.RED
        // كرة ملونة عادية بتأثير ثلاثي الأبعاد
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.lightShade,
                    baseColor.primaryColor,
                    baseColor.darkShade
                ),
                center = Offset(center.x - radius * 0.32f, center.y - radius * 0.32f),
                radius = radius * 1.25f
            ),
            radius = radius,
            center = center
        )
        // بريق لمعان لطيف (Specular highlight)
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = radius * 0.2f,
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
        )
    }
}

/**
 * مكون الكأس الطائرة (شكل نصف دائرة/قوس مع أجنحة لطيفة ترفرف ووجه مبتسم)
 */
@Composable
fun FlyingCupComponent(
    cupX: Float,
    cupY: Float,
    widthPx: Float,
    heightPx: Float,
    currentColor: GameColor,
    isGoldenBuff: Boolean
) {
    // حركة رفرفة أجنحة الكأس الطائرة
    val infiniteTransition = rememberInfiniteTransition(label = "wings")
    val wingAngle by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wingFlutter"
    )

    // وميض خفيف للون الكأس عند التبديل
    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = currentColor.primaryColor,
        animationSpec = tween(350),
        label = "cupColor"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (cupX - widthPx / 2f).toInt(),
                    y = cupY.toInt()
                )
            }
            .size(
                width = with(LocalDensity.current) { widthPx.toDp() },
                height = with(LocalDensity.current) { heightPx.toDp() }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cupCenter = Offset(w / 2f, h * 0.35f)
            val bowlRadius = w * 0.38f

            // توهج ذهبي إذا كان المضاعف مفعلاً
            if (isGoldenBuff) {
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                    radius = bowlRadius * 1.35f,
                    center = Offset(cupCenter.x, cupCenter.y + h * 0.15f)
                )
            }

            // 1. الأجنحة الجانبية (الكأس الطائرة)
            // الجناح الأيسر
            drawWing(
                isLeft = true,
                centerX = w * 0.18f,
                centerY = h * 0.38f,
                wingLength = w * 0.28f,
                angle = wingAngle
            )
            // الجناح الأيمن
            drawWing(
                isLeft = false,
                centerX = w * 0.82f,
                centerY = h * 0.38f,
                wingLength = w * 0.28f,
                angle = -wingAngle
            )

            // 2. جسم الكأس (نصف دائرة / قوس جذاب)
            val arcRect = Rect(
                left = cupCenter.x - bowlRadius,
                top = cupCenter.y - bowlRadius * 0.2f,
                right = cupCenter.x + bowlRadius,
                bottom = cupCenter.y + bowlRadius * 1.8f
            )

            val cupPath = Path().apply {
                arcTo(
                    rect = arcRect,
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }

            // تدرج لوني لجسم الكأس
            drawPath(
                path = cupPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedColor,
                        currentColor.darkShade
                    )
                )
            )

            // إطار خارجي أنيق للكأس
            drawPath(
                path = cupPath,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 5f)
            )

            // 3. الحافة العلوية للكأس (شكل بيضاوي أفقي مائل للنعومة)
            drawOval(
                color = currentColor.lightShade,
                topLeft = Offset(cupCenter.x - bowlRadius, cupCenter.y - 10f),
                size = Size(bowlRadius * 2f, 22f)
            )
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(cupCenter.x - bowlRadius, cupCenter.y - 10f),
                size = Size(bowlRadius * 2f, 22f),
                style = Stroke(width = 4f)
            )

            // 4. ملامح وجه كرتوني لطيف يسعد الأطفال في الكأس
            val eyeOffsetY = cupCenter.y + h * 0.32f
            val eyeSpacing = bowlRadius * 0.42f

            // العينان
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 7f,
                center = Offset(cupCenter.x - eyeSpacing, eyeOffsetY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(cupCenter.x - eyeSpacing - 2f, eyeOffsetY - 2f)
            )

            drawCircle(
                color = Color(0xFF1E293B),
                radius = 7f,
                center = Offset(cupCenter.x + eyeSpacing, eyeOffsetY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(cupCenter.x + eyeSpacing - 2f, eyeOffsetY - 2f)
            )

            // الخدود الوردية
            drawCircle(
                color = Color(0xFFF43F5E).copy(alpha = 0.45f),
                radius = 8f,
                center = Offset(cupCenter.x - eyeSpacing - 8f, eyeOffsetY + 8f)
            )
            drawCircle(
                color = Color(0xFFF43F5E).copy(alpha = 0.45f),
                radius = 8f,
                center = Offset(cupCenter.x + eyeSpacing + 8f, eyeOffsetY + 8f)
            )

            // الابتسامة
            val smilePath = Path().apply {
                arcTo(
                    rect = Rect(
                        left = cupCenter.x - 14f,
                        top = eyeOffsetY + 2f,
                        right = cupCenter.x + 14f,
                        bottom = eyeOffsetY + 22f
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF1E293B),
                style = Stroke(width = 4f)
            )
        }
    }
}

/**
 * دالة مساعدة لرسم جناح الكأس الطائرة
 */
fun DrawScope.drawWing(
    isLeft: Boolean,
    centerX: Float,
    centerY: Float,
    wingLength: Float,
    angle: Float
) {
    val direction = if (isLeft) -1f else 1f
    val wingPath = Path().apply {
        moveTo(centerX, centerY)
        quadraticBezierTo(
            centerX + direction * wingLength * 0.7f,
            centerY - wingLength * 0.6f + angle,
            centerX + direction * wingLength,
            centerY - wingLength * 0.15f
        )
        quadraticBezierTo(
            centerX + direction * wingLength * 0.6f,
            centerY + wingLength * 0.35f,
            centerX,
            centerY
        )
        close()
    }

    drawPath(
        path = wingPath,
        color = Color.White.copy(alpha = 0.95f),
        style = Fill
    )
    drawPath(
        path = wingPath,
        color = Color(0xFF94A3B8),
        style = Stroke(width = 3.5f)
    )
}

/**
 * خلفية سحب متحركة وناعمة تعطي شعور الطيران في السماء
 */
@Composable
fun AnimatedCloudsBackground(
    screenWidthPx: Float,
    screenHeightPx: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = screenWidthPx + 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud1"
    )

    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = screenWidthPx + 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // سحابة 1
        drawCartoonCloud(
            center = Offset(cloudOffset1, screenHeightPx * 0.18f),
            scale = 1.1f
        )
        // سحابة 2
        drawCartoonCloud(
            center = Offset(cloudOffset2, screenHeightPx * 0.34f),
            scale = 0.85f
        )
    }
}

/**
 * رسم سحابة كرتونية لطيفة
 */
fun DrawScope.drawCartoonCloud(center: Offset, scale: Float) {
    val cloudColor = Color.White.copy(alpha = 0.82f)
    val r = 38f * scale

    drawCircle(cloudColor, radius = r, center = center)
    drawCircle(cloudColor, radius = r * 0.75f, center = Offset(center.x - r * 0.9f, center.y + r * 0.2f))
    drawCircle(cloudColor, radius = r * 0.85f, center = Offset(center.x + r * 0.95f, center.y + r * 0.15f))
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(center.x - r * 1.3f, center.y),
        size = Size(r * 2.6f, r * 0.85f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
    )
}

/**
 * لافتة الاحتفال بالشخصية (قطة، بطريق، ديناصور) بعد كل 20 نقطة
 */
@Composable
fun CelebrationBanner(celebration: CelebrationCharacter) {
    Card(
        colors = CardDefaults.cardColors(containerColor = celebration.badgeColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = celebration.emoji,
                fontSize = 42.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "🎉 ${celebration.nameAr} تحتفل بك!",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = celebration.messageAr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}

/**
 * شاشة نهاية اللعبة عندما تصل القلوب إلى الصفر
 */
@Composable
fun GameOverDialog(
    finalScore: Int,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.game_over),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.game_over_desc),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // عرض النقاط النهائية المحققة
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مجموع النقاط",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "$finalScore",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // زر إعادة التشغيل
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_again_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة التشغيل",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.play_again),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * دالة مساعدة لضمان توافق اختبارات Robolectric الحالية
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun FlyingCupPreview() {
    MyApplicationTheme {
        FlyingCupGameScreen()
    }
}

