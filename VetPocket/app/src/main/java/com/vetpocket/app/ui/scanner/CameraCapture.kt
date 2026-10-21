package com.vetpocket.app.ui.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Доли кадра (портретный 3:4), в которых нарисована направляющая рамка. */
private const val FRAME_LEFT = 0.07f
private const val FRAME_TOP = 0.30f
private const val FRAME_WIDTH = 0.86f
private const val FRAME_HEIGHT = 0.40f

/** Запас вокруг рамки при обрезке снимка — чтобы не срезать крайние буквы. */
private const val CROP_PADDING = 0.03f

class CameraController internal constructor(private val capture: ImageCapture, private val executor: ExecutorService) {

    fun takePhoto(onBitmap: (Bitmap) -> Unit, onFailure: (String) -> Unit) {
        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = try {
                        cropToGuide(image)
                    } catch (e: Exception) {
                        null
                    } finally {
                        image.close()
                    }
                    if (result != null) onBitmap(result) else onFailure("Не удалось обработать снимок.")
                }

                override fun onError(exception: ImageCaptureException) {
                    onFailure("Ошибка камеры: ${exception.message ?: "неизвестная"}")
                }
            },
        )
    }

    private fun cropToGuide(image: ImageProxy): Bitmap {
        val source = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val upright = if (rotation == 0) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        }
        val left = ((FRAME_LEFT - CROP_PADDING) * upright.width).toInt().coerceAtLeast(0)
        val top = ((FRAME_TOP - CROP_PADDING) * upright.height).toInt().coerceAtLeast(0)
        val width = ((FRAME_WIDTH + 2 * CROP_PADDING) * upright.width).toInt().coerceAtMost(upright.width - left)
        val height = ((FRAME_HEIGHT + 2 * CROP_PADDING) * upright.height).toInt().coerceAtMost(upright.height - top)
        return Bitmap.createBitmap(upright, left, top, width, height)
    }
}

/**
 * Видоискатель CameraX с направляющей рамкой.
 * Предпросмотр и снимок настроены на соотношение 4:3, а контейнер имеет пропорции 3:4,
 * поэтому доли рамки на экране совпадают с долями на снимке.
 */
@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    onReady: (CameraController) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
    }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                onReady(CameraController(imageCapture, executor))
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbindAll() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        GuideOverlay(Modifier.fillMaxSize())
    }
}

@Composable
private fun GuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val left = size.width * FRAME_LEFT
        val top = size.height * FRAME_TOP
        val w = size.width * FRAME_WIDTH
        val h = size.height * FRAME_HEIGHT
        val corner = 24.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            blendMode = BlendMode.Clear,
        )

        // Угловые скобки
        val bracket = 36.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val c = Color.White
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = stroke.width, cap = StrokeCap.Round)
        // верх-лево
        line(left, top + bracket, left, top + corner / 2); line(left, top, left + bracket, top)
        // верх-право
        line(left + w - bracket, top, left + w, top); line(left + w, top, left + w, top + bracket)
        // низ-лево
        line(left, top + h - bracket, left, top + h); line(left, top + h, left + bracket, top + h)
        // низ-право
        line(left + w - bracket, top + h, left + w, top + h); line(left + w, top + h - bracket, left + w, top + h)
    }
}
