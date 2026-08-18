package com.pampoukidis.streamcoretv.feature.player.mobile.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerRouteEventEffect
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerViewModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel

@Composable
fun MobilePlayerRoute(
    request: PlaybackRequestModel,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val videoSurface by viewModel.videoSurface.collectAsStateWithLifecycle()
    val isPipSupported = rememberPipSupport(context)

    LaunchedEffect(request, viewModel, isPipSupported) {
        viewModel.onAction(PlayerAction.Load(request, isPipSupported))
    }

    DisposableEffect(activity) {
        if (activity == null) {
            return@DisposableEffect onDispose {}
        }
        val previousOrientation = activity.requestedOrientation
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            activity.requestedOrientation = previousOrientation
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(lifecycleOwner, activity, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAction(PlayerAction.ForegroundChanged(true))
                Lifecycle.Event.ON_STOP -> viewModel.onAction(
                    PlayerAction.ForegroundChanged(
                        MobilePipPolicy.isForegroundOnStop(
                            activity?.isInPictureInPictureMode == true,
                        ),
                    ),
                )

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(activity, state.isPlaying, state.phase) {
        val keepAwake = state.isPlaying || state.phase == PlaybackPhase.Buffering
        if (keepAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {}
    }

    DisposableEffect(activity, state.isPlaying, isPipSupported) {
        if (activity != null && isPipSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.setPictureInPictureParams(
                pipParams(
                    autoEnter = MobilePipPolicy.shouldEnableAutoEnter(
                        apiLevel = Build.VERSION.SDK_INT,
                        isPlaying = state.isPlaying,
                    ),
                ),
            )
        }
        onDispose {}
    }

    DisposableEffect(activity, viewModel) {
        if (activity == null) {
            return@DisposableEffect onDispose {}
        }
        val listener = androidx.core.util.Consumer<PictureInPictureModeChangedInfo> { info ->
            viewModel.onAction(PlayerAction.PipChanged(info.isInPictureInPictureMode))
        }
        activity.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }

    PlayerRouteEventEffect(
        viewModel = viewModel,
        onBack = onBack,
        onEnterPictureInPicture = {
            if (activity != null && MobilePipPolicy.supportsExplicitEntry(Build.VERSION.SDK_INT)) {
                activity.enterPictureInPictureMode(pipParams(autoEnter = false))
            }
        },
    )

    BackHandler { viewModel.onAction(PlayerAction.BackSelected) }

    MobilePlayerScreen(
        state = state,
        videoSurface = videoSurface,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun rememberPipSupport(context: Context): Boolean {
    return MobilePipPolicy.isSupported(
        apiLevel = Build.VERSION.SDK_INT,
        hasSystemFeature = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_PICTURE_IN_PICTURE,
        ),
    )
}

private fun pipParams(autoEnter: Boolean): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
        builder.setSeamlessResizeEnabled(true)
    }
    return builder.build()
}

private tailrec fun Context.findActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
