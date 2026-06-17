package com.hwanderer.entropysdefiance.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.absoluteValue

/**
 * The welcome narration. Six full-screen pages introducing the launcher's
 * philosophy. Shown once on first launch and replayable from
 * Settings → Introduction.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    primaryColor: Color,
    onComplete: () -> Unit
) {
    val pages = remember { welcomePages }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    PaintSystemBarsBlack()

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val pageOffset = (
                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(0f, 1f)
                val contentAlpha = 1f - pageOffset

                WelcomePageContent(
                    page = pages[pageIndex],
                    contentAlpha = contentAlpha,
                    primaryColor = primaryColor,
                    isFinalPage = pageIndex == pages.size - 1,
                    onBegin = onComplete
                )
            }

            // Skip — top-right, hidden on the final page where the Begin button takes over.
            AnimatedVisibility(
                visible = pagerState.currentPage < pages.size - 1,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 12.dp)
            ) {
                TextButton(onClick = onComplete) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W400
                    )
                }
            }

            // Page indicator — replaced by the Begin button on the final page.
            AnimatedVisibility(
                visible = pagerState.currentPage < pages.size - 1,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            ) {
                PageDots(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                    primaryColor = primaryColor
                )
            }
        }
    }
}

@Composable
private fun PaintSystemBarsBlack() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val decorView = window?.decorView

        // The activity theme uses windowIsTranslucent + windowShowWallpaper so the launcher
        // can breathe through the wallpaper, with statusBarColor=transparent so the bars
        // never paint a contrast scrim over the home content. On some OEMs (notably the
        // Fairphone 4) that combination leaves the bar area showing the wallpaper — a white
        // strip if the wallpaper happens to be light there.
        //
        // For the welcome, we want a uniformly black canvas. We paint three layers:
        //   1. decor view background — the bottom of the window, beneath the Compose content
        //   2. window background drawable — used by some OEMs as a back-stop
        //   3. statusBarColor / navigationBarColor — the system bars themselves
        // and we keep the bars *visible* (rather than hiding them), because OEMs are far more
        // likely to honor an explicit black bar color than to respect a hidden-bar inset.
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val previousAppearanceLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousAppearanceLightNavigationBars = controller?.isAppearanceLightNavigationBars

        decorView?.setBackgroundColor(android.graphics.Color.BLACK)
        window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        )
        window?.statusBarColor = android.graphics.Color.BLACK
        window?.navigationBarColor = android.graphics.Color.BLACK
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        controller?.show(WindowInsetsCompat.Type.systemBars())

        onDispose {
            // Always restore to fully transparent — the launcher relies on showing the
            // wallpaper through the activity, so reading back whatever drawable was there
            // before is unreliable (it's null on the first welcome) and would leave the home
            // screen looking opaque-black instead of breathing through the wallpaper.
            decorView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            previousStatusBarColor?.let { window.statusBarColor = it }
            previousNavigationBarColor?.let { window.navigationBarColor = it }
            previousAppearanceLightStatusBars?.let { controller.isAppearanceLightStatusBars = it }
            previousAppearanceLightNavigationBars?.let {
                controller.isAppearanceLightNavigationBars = it
            }
        }
    }
}

@Composable
private fun WelcomePageContent(
    page: WelcomePage,
    contentAlpha: Float,
    primaryColor: Color,
    isFinalPage: Boolean,
    onBegin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.showHorizonsDiagram) {
            ConcentricHorizonsDiagram(primaryColor = primaryColor)
            Spacer(modifier = Modifier.height(56.dp))
        }

        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 38.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.5).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W100
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                lineHeight = 28.sp
            ),
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W400
        )

        if (isFinalPage) {
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onBegin,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 200.dp)
            ) {
                Text(
                    text = "Begin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W500,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ConcentricHorizonsDiagram(primaryColor: Color) {
    val infinite = rememberInfiniteTransition(label = "horizonPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val strokePx = 1.4f.dp.toPx()
        // Outer-to-inner: Life, Year, Month, Week. Today is the filled dot below.
        val ringFractions = listOf(1.0f, 0.78f, 0.58f, 0.40f)
        ringFractions.forEachIndexed { index, fraction ->
            // Outermost (Life) is faintest; rings get more present as they approach today.
            val alpha = 0.20f + index * 0.10f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = maxRadius * fraction,
                center = center,
                style = Stroke(width = strokePx)
            )
        }
        // Today — the moment, in the user's chosen accent.
        drawCircle(
            color = primaryColor,
            radius = maxRadius * 0.13f * pulse,
            center = center
        )
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    primaryColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { i ->
            val isActive = i == currentPage
            val width by animateDpAsState(
                targetValue = if (isActive) 22.dp else 6.dp,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "dotWidth"
            )
            val color = if (isActive) primaryColor else Color.White.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .width(width)
                    .height(6.dp)
                    .background(color = color, shape = RoundedCornerShape(3.dp))
            )
        }
    }
}

private data class WelcomePage(
    val title: String,
    val body: String,
    val showHorizonsDiagram: Boolean = false
)

private val welcomePages = listOf(
    WelcomePage(
        title = "Entropy's Defiance",
        body = "A launcher for the finite hours of your life."
    ),
    WelcomePage(
        title = "Five horizons",
        body = "Today. This Week. This Month. This Year. This Life.\n" +
            "Every task lives at one of these scales.",
        showHorizonsDiagram = true
    ),
    WelcomePage(
        title = "Move with a swipe",
        body = "Swipe a task left or right to carry it across horizons — " +
            "pull it into today, or let it drift toward this life."
    ),
    WelcomePage(
        title = "Top-down",
        body = "Long-press the horizon title to align — from life, " +
            "down through the year, the month, the week, into today. " +
            "The smaller scales serve the larger ones."
    ),
    WelcomePage(
        title = "Peek ahead",
        body = "Press and hold the + button to peek into the next horizon — " +
            "glimpse what's coming without leaving where you are."
    ),
    WelcomePage(
        title = "One task at a time",
        body = "Long-press a task to enter Zen Mode. " +
            "One task, no drawer, no calendar. " +
            "The only way out is to mark it done."
    ),
    WelcomePage(
        title = "Honest hours",
        body = "The bar shows time left in the horizon. " +
            "The number below shows hours actually available — " +
            "your calendar deducted, your productivity ratio applied."
    ),
    WelcomePage(
        title = "By gesture",
        body = "Swipe up for your apps, then type to find one. " +
            "Double-tap to dial. Long-press the home screen for settings."
    ),
    WelcomePage(
        title = "Yours to keep",
        body = "Tasks are plain text in a folder you choose. " +
            "Sync it with Syncthing, or edit from a laptop. " +
            "No account, no cloud, no lock-in."
    ),
    WelcomePage(
        title = "Begin",
        body = "Your time is finite.\nSpend it on what matters."
    )
)
