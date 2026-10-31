package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.GlassSurface
import com.swordfish.touchinput.radial.ui.LemuroidButtonPressFeedback
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun MobileGameScreen(viewModel: BaseGameScreenViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = constraints.maxWidth > constraints.maxHeight

        LaunchedEffect(isLandscape) {
            val orientation =
                if (isLandscape) {
                    TouchControllerSettingsManager.Orientation.LANDSCAPE
                } else {
                    TouchControllerSettingsManager.Orientation.PORTRAIT
                }
            viewModel.onScreenOrientationChanged(orientation)
        }

        val controllerConfigState = viewModel.getTouchControllerConfig().collectAsState(null)
        val touchControlsVisibleState = viewModel.isTouchControllerVisible().collectAsState(false)
        val touchControllerSettingsState =
            viewModel
                .getTouchControlsSettings(LocalDensity.current, WindowInsets.displayCutout)
                .collectAsState(null)

        val touchControllerSettings = touchControllerSettingsState.value
        val currentControllerConfig = controllerConfigState.value

        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }

        val touchGamePads = currentControllerConfig?.getTouchControllerConfig()
        val leftGamePad = touchGamePads?.leftComposable
        val rightGamePad = touchGamePads?.rightComposable

        val hapticFeedbackMode =
            viewModel
                .getTouchHapticFeedbackMode()
                .collectAsState(HapticFeedbackMode.NONE)

        val padHapticFeedback =
            when (hapticFeedbackMode.value) {
                HapticFeedbackMode.NONE -> HapticFeedbackType.NONE
                HapticFeedbackMode.PRESS -> HapticFeedbackType.PRESS
                HapticFeedbackMode.PRESS_RELEASE -> HapticFeedbackType.PRESS_RELEASE
            }

        PadKit(
            modifier = Modifier.fillMaxSize(),
            onInputEvents = { viewModel.handleVirtualInputEvent(it) },
            hapticFeedbackType = padHapticFeedback,
            simulatedState = tiltSimulatedStates,
            simulatedControlIds = tiltSimulatedControls,
        ) {
            val localContext = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current

            val fullScreenPosition = remember { mutableStateOf<Rect?>(null) }
            val viewportPosition = remember { mutableStateOf<Rect?>(null) }

            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { fullScreenPosition.value = it.boundsInRoot() },
                factory = {
                    viewModel.createRetroView(localContext, lifecycle)
                },
            )

            val fullPos = fullScreenPosition.value
            val viewPos = viewportPosition.value
            val scaling = viewModel.getScaling().collectAsState().value

            LaunchedEffect(fullPos, viewPos, scaling) {
                val gameView = viewModel.retroGameView.retroGameViewFlow()
                if (fullPos == null || viewPos == null) return@LaunchedEffect
                val area =
                    RectF(
                        viewPos.left - fullPos.left,
                        viewPos.top - fullPos.top,
                        viewPos.right - fullPos.left,
                        viewPos.bottom - fullPos.top,
                    )
                gameView.viewport =
                    viewModel.computeScaledViewport(
                        scaling,
                        gameView.viewportAlignment,
                        fullPos.width,
                        fullPos.height,
                        area,
                    )
            }

            ConstraintLayout(
                modifier = Modifier.fillMaxSize(),
                constraintSet =
                    GameScreenLayout.buildConstraintSet(
                        isLandscape,
                        currentControllerConfig?.allowTouchOverlay ?: true,
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW)
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            .onGloballyPositioned { viewportPosition.value = it.boundsInRoot() },
                )

                val isVisible =
                    touchControllerSettings != null &&
                        currentControllerConfig != null &&
                        touchControlsVisibleState.value

                if (isVisible) {
                    CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {
                        if (!isLandscape) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_BOTTOM_CONTAINER),
                            )
                        } else if (!currentControllerConfig.allowTouchOverlay) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_CONTAINER),
                            )
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_CONTAINER),
                            )
                        }

                        leftGamePad?.invoke(
                            this,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD),
                            touchControllerSettings,
                        )
                        rightGamePad?.invoke(
                            this,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD),
                            touchControllerSettings,
                        )

                        GameScreenRunningCentralMenu(
                            modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_GAME_CONTAINER),
                            controllerConfig = currentControllerConfig,
                            touchControllerSettings = touchControllerSettings,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }

        val isLoading =
            viewModel.loadingState
                .collectAsState(true)
                .value

        if (!isLoading) {
            GameOverlayButtons(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(8.dp),
                viewModel = viewModel,
            )
            GameOverlayCard(
                modifier = Modifier.align(Alignment.Center),
                viewModel = viewModel,
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/** In-game buttons, top right: quick save state, mute, fast forward. */
@Composable
private fun GameOverlayButtons(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
) {
    // The buttons fade back after a while so they don't distract from the game, and come back to
    // full strength whenever one of them is touched (the tap still does its job).
    var interactions by remember { mutableIntStateOf(0) }
    var dimmed by remember { mutableStateOf(false) }
    LaunchedEffect(interactions) {
        dimmed = false
        delay(OVERLAY_BUTTONS_DIM_DELAY_MILLIS)
        dimmed = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (dimmed) OVERLAY_BUTTONS_DIMMED_ALPHA else 1f,
        animationSpec = tween(durationMillis = OVERLAY_BUTTONS_FADE_MILLIS),
        label = "overlayButtonsAlpha",
    )

    Row(
        modifier =
            modifier
                .alpha(alpha)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) interactions++
                        }
                    }
                },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (viewModel.areStatesSupported()) {
            StatesButton(viewModel)
        }
        MuteButton(viewModel)
        if (viewModel.isFastForwardSupported()) {
            FastForwardButton(viewModel)
        }
        FilterButton(viewModel)
        if (viewModel.isScalingSupported()) {
            ScalingButton(viewModel)
        }
        MenuButton(viewModel)
    }
}

/** Opens the in-game menu (same as the L1+R1 / menu shortcut). */
@Composable
private fun MenuButton(viewModel: BaseGameScreenViewModel) {
    val label = stringResource(com.swordfish.lemuroid.R.string.game_button_menu)
    OverlayButton(active = false, label = label, onClick = { viewModel.openMenu() }) {
        Icon(
            modifier = Modifier.size(OVERLAY_ICON_SIZE),
            imageVector = Icons.Filled.Menu,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.8f),
        )
    }
}

/** Switches to the next filter; the tiny screen shows its number (1 Sharp, 2 Smooth, 3 CRT, 4 LCD). */
@Composable
private fun FilterButton(viewModel: BaseGameScreenViewModel) {
    val filter = viewModel.getFilter().collectAsState(null).value
    val label = stringResource(com.swordfish.lemuroid.R.string.game_button_filter)
    OverlayButton(active = false, label = label, onClick = { viewModel.cycleFilter() }) {
        MiniScreenIcon(text = filter?.let { "${it.ordinal + 1}" } ?: "HD", stand = true)
    }
}

/** Switches to the next scaling mode; the tiny screen shows its number (1 Fit, 2 Integer, 3 Integer -1). */
@Composable
private fun ScalingButton(viewModel: BaseGameScreenViewModel) {
    val scaling = viewModel.getScaling().collectAsState().value
    val label = stringResource(com.swordfish.lemuroid.R.string.game_button_scaling)
    OverlayButton(active = false, label = label, onClick = { viewModel.cycleScaling() }) {
        MiniScreenIcon(text = "${scaling.ordinal + 1}", stand = false)
    }
}

/**
 * A tiny screen with a number inside. With [stand] it has a small foot (filter); without it has
 * two corner marks (scaling).
 */
@Composable
private fun MiniScreenIcon(
    text: String,
    stand: Boolean,
) {
    val color = Color.White.copy(alpha = 0.85f)
    Canvas(modifier = Modifier.size(28.dp)) {
        val stroke = 1.8.dp.toPx()
        val screenWidth = 20.dp.toPx()
        val screenHeight = 15.dp.toPx()
        val left = (size.width - screenWidth) / 2f
        val top = (size.height - screenHeight) / 2f - if (stand) 1.5.dp.toPx() else 0f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(2.5.dp.toPx()),
            style = Stroke(width = stroke),
        )
        if (stand) {
            val footWidth = 6.dp.toPx()
            val footY = top + screenHeight + 2.5.dp.toPx()
            drawLine(
                color = color,
                start = Offset((size.width - footWidth) / 2f, footY),
                end = Offset((size.width + footWidth) / 2f, footY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        } else {
            val arm = 3.5.dp.toPx()
            val gap = 2.dp.toPx()
            val x0 = left - gap
            val y0 = top - gap
            val x1 = left + screenWidth + gap
            val y1 = top + screenHeight + gap
            drawLine(color, Offset(x0, y0), Offset(x0 + arm, y0), stroke, StrokeCap.Round)
            drawLine(color, Offset(x0, y0), Offset(x0, y0 + arm), stroke, StrokeCap.Round)
            drawLine(color, Offset(x1, y1), Offset(x1 - arm, y1), stroke, StrokeCap.Round)
            drawLine(color, Offset(x1, y1), Offset(x1, y1 - arm), stroke, StrokeCap.Round)
        }
    }
    Text(
        text = text,
        color = Color.White,
        fontSize = if (text.length > 1) 8.sp else 10.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.offset(y = if (stand) (-1.5).dp else 0.dp),
    )
}

/** Card in the middle of the game after changing filter or scaling: number, name and position. */
@Composable
private fun GameOverlayCard(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
) {
    var visibleCard by remember { mutableStateOf<BaseGameScreenViewModel.OverlayCard?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getOverlayCards().filterNotNull().collectLatest { card ->
            viewModel.onOverlayCardShown(card.id)
            visibleCard = card
            delay(OVERLAY_CARD_MILLIS)
            visibleCard = null
        }
    }

    var lastCard by remember { mutableStateOf<BaseGameScreenViewModel.OverlayCard?>(null) }
    if (visibleCard != null) lastCard = visibleCard
    val shown = lastCard

    AnimatedVisibility(modifier = modifier, visible = visibleCard != null, enter = fadeIn(), exit = fadeOut()) {
        if (shown == null) return@AnimatedVisibility
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0C0E).copy(alpha = 0.82f))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = shown.title.uppercase(),
                color = Color(0xFF9AA4AD),
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = shown.value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(shown.count) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(width = 20.dp, height = 5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == shown.index) {
                                        FAST_FORWARD_ACTIVE_COLOR
                                    } else {
                                        Color.White.copy(alpha = 0.22f)
                                    },
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayButton(
    active: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(OVERLAY_BUTTON_SIZE)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = if (active) 0.6f else 0.35f))
                .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Saves a state into an empty slot, or over the oldest one. */
@Composable
private fun StatesButton(viewModel: BaseGameScreenViewModel) {
    val label = stringResource(com.swordfish.lemuroid.R.string.game_button_states)
    OverlayButton(
        active = false,
        label = label,
        onClick = { viewModel.quickSaveToNextSlot() },
    ) {
        Icon(
            modifier = Modifier.size(OVERLAY_ICON_SIZE),
            imageVector = Icons.Outlined.Save,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.8f),
        )
    }
}

/** Mutes and unmutes the game; shown highlighted while muted. */
@Composable
private fun MuteButton(viewModel: BaseGameScreenViewModel) {
    val audioEnabled = viewModel.isAudioEnabled().collectAsState(true).value
    val label = stringResource(com.swordfish.lemuroid.R.string.game_menu_mute_audio)
    OverlayButton(active = !audioEnabled, label = label, onClick = { viewModel.toggleMute() }) {
        Icon(
            modifier = Modifier.size(OVERLAY_ICON_SIZE),
            imageVector =
                if (audioEnabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
            contentDescription = label,
            tint = if (audioEnabled) Color.White.copy(alpha = 0.8f) else FAST_FORWARD_ACTIVE_COLOR,
        )
    }
}

/** Turns fast forward (2x) on and off; yellow while on. */
@Composable
private fun FastForwardButton(viewModel: BaseGameScreenViewModel) {
    val enabled = viewModel.isFastForwardEnabled().collectAsState(false).value
    val label = stringResource(com.swordfish.lemuroid.R.string.game_menu_fast_forward)

    OverlayButton(active = enabled, label = label, onClick = { viewModel.toggleFastForward() }) {
        Icon(
            modifier =
                Modifier
                    .size(OVERLAY_ICON_SIZE)
                    .semantics { stateDescription = if (enabled) "On" else "Off" },
            painter = painterResource(com.swordfish.lemuroid.R.drawable.ic_menu_fast_forward),
            contentDescription = label,
            tint = if (enabled) FAST_FORWARD_ACTIVE_COLOR else Color.White.copy(alpha = 0.7f),
        )
    }
}

private const val OVERLAY_BUTTONS_DIM_DELAY_MILLIS = 15_000L
private const val OVERLAY_BUTTONS_DIMMED_ALPHA = 0.3f
private const val OVERLAY_BUTTONS_FADE_MILLIS = 600
private val OVERLAY_BUTTON_SIZE = 46.dp
private val OVERLAY_ICON_SIZE = 23.dp
private const val OVERLAY_CARD_MILLIS = 1200L

private val FAST_FORWARD_ACTIVE_COLOR = Color(0xFFFFC107)

@Composable
private fun PadContainer(modifier: Modifier = Modifier) {
    val theme = LocalLemuroidPadTheme.current
    GlassSurface(
        modifier = modifier,
        cornerRadius = theme.level0CornerRadius,
        fillColor = theme.level0Fill,
        shadowColor = theme.level0Shadow,
        shadowWidth = theme.level0ShadowWidth,
    )
}

@Composable
private fun GameScreenRunningCentralMenu(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
    controllerConfig: ControllerConfig,
) {
    val menuPressed = viewModel.isMenuPressed().collectAsState(false)
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        LemuroidButtonPressFeedback(
            pressed = menuPressed.value,
            animationDurationMillis = MENU_LOADING_ANIMATION_MILLIS,
            icon = R.drawable.button_menu,
        )
        MenuEditTouchControls(viewModel, controllerConfig, touchControllerSettings)
    }
}

@Composable
private fun MenuEditTouchControls(
    viewModel: BaseGameScreenViewModel,
    controllerConfig: ControllerConfig,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
) {
    val showEditControls = viewModel.isEditControlShown().collectAsState(false)
    if (!showEditControls.value) return

    Dialog(onDismissRequest = { viewModel.showEditControls(false) }) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuEditTouchControlRow(Icons.Default.OpenInFull, "Scale", 0f) {
                    Slider(
                        value = touchControllerSettings.scale,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(scale = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Horizontal Margin", 90f) {
                    Slider(
                        value = touchControllerSettings.marginX,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginX = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Vertical Margin", 0f) {
                    Slider(
                        value = touchControllerSettings.marginY,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginY = it),
                            )
                        },
                    )
                }
                if (controllerConfig.allowTouchRotation) {
                    MenuEditTouchControlRow(Icons.Default.RotateLeft, "Rotate", 0f) {
                        Slider(
                            value = touchControllerSettings.rotation,
                            onValueChange = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(rotation = it),
                                )
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { viewModel.resetTouchControls() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_reset))
                    }
                    TextButton(
                        onClick = { viewModel.showEditControls(false) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEditTouchControlRow(
    icon: ImageVector,
    label: String,
    rotation: Float,
    slider: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.rotate(rotation),
            imageVector = icon,
            contentDescription = label,
        )
        slider()
    }
}
