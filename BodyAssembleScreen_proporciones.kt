package com.m.adacolorytrazo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.m.adacolorytrazo.R
import kotlin.math.roundToInt

private data class BodyPart(
    val id: String,
    val drawableName: String,
    // Rectángulo normalizado x/y/ancho/alto medido sobre el arte de referencia 1440x2560.
    val trayRect: androidx.compose.ui.geometry.Rect
)

private val bodyOffsetsSaver = listSaver<Map<String, Offset>, Any>(
    save = { offsets ->
        offsets.flatMap { (id, point) -> listOf(id, point.x, point.y) }
    },
    restore = { saved ->
        saved.chunked(3).mapNotNull { row ->
            val id = row.getOrNull(0) as? String ?: return@mapNotNull null
            val x = (row.getOrNull(1) as? Number)?.toFloat() ?: return@mapNotNull null
            val y = (row.getOrNull(2) as? Number)?.toFloat() ?: return@mapNotNull null
            id to Offset(x, y)
        }.toMap()
    }
)

@Composable
fun BodyAssembleScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val parts = remember {
        // Tamaños relativos al lienzo original de 1440 x 2560.
        // Se mantienen las piezas sueltas en la bandeja para que el niño las arrastre libremente.
        listOf(
            BodyPart("pierna_izq", "body_demo_leg_left_boot", androidx.compose.ui.geometry.Rect(.590f, .751f, .728f, .950f)),
            BodyPart("pierna_der", "body_demo_leg_right_boot", androidx.compose.ui.geometry.Rect(.790f, .751f, .928f, .950f)),
            BodyPart("brazo_izq", "body_demo_arm_left", androidx.compose.ui.geometry.Rect(.018f, .780f, .180f, .929f)),
            BodyPart("brazo_der", "body_demo_arm_right", androidx.compose.ui.geometry.Rect(.463f, .780f, .594f, .935f)),
            BodyPart("torso", "body_demo_torso", androidx.compose.ui.geometry.Rect(.200f, .774f, .453f, .944f)),
            BodyPart("oreja_izq", "ada_ear_left", androidx.compose.ui.geometry.Rect(.608f, .684f, .648f, .721f)),
            BodyPart("oreja_der", "ada_ear_right", androidx.compose.ui.geometry.Rect(.650f, .684f, .690f, .721f)),
            BodyPart("cara", "ada_face_base", androidx.compose.ui.geometry.Rect(.052f, .637f, .305f, .804f)),
            BodyPart("cabello", "ada_hair", androidx.compose.ui.geometry.Rect(.285f, .603f, .608f, .787f)),
            BodyPart("ojos", "ada_eyes", androidx.compose.ui.geometry.Rect(.740f, .670f, .921f, .719f)),
            BodyPart("nariz", "ada_nose", androidx.compose.ui.geometry.Rect(.723f, .728f, .773f, .755f)),
            BodyPart("boca", "ada_smile", androidx.compose.ui.geometry.Rect(.808f, .730f, .905f, .763f))
        )
    }

    ApprovedPrototypeScreen(
        imageRes = R.drawable.approved_05_body_clean_no_arm,
        screenDescription = "Ada vacía en el jardín, lista para armar con las piezas",
        hotspots = listOf(
            PrototypeHotspot(
                left = 0f,
                top = 0f,
                right = 0.14f,
                bottom = 0.10f,
                label = "Volver a Jugar con Ada",
                onClick = onBack
            )
        ),
        modifier = modifier
    ) {
        BodyPartsOverlay(parts = parts, density = density, context = context)
    }
}

@Composable
private fun BoxWithConstraintsScope.BodyPartsOverlay(
    parts: List<BodyPart>,
    density: androidx.compose.ui.unit.Density,
    context: android.content.Context
) {
    val widthPx = with(density) { maxWidth.toPx() }.roundToInt().coerceAtLeast(1)
    val heightPx = with(density) { maxHeight.toPx() }.roundToInt().coerceAtLeast(1)
    // Escala uniforme: el tamaño de las piezas no cambia de proporción
    // por usar un teléfono más ancho o más alto.
    val referenceScale = minOf(widthPx / 1440f, heightPx / 2560f)
    val initialOffsets = remember(widthPx, heightPx, parts) {
        parts.associate { part ->
            part.id to Offset(part.trayRect.left * widthPx, part.trayRect.top * heightPx)
        }
    }
    var offsets by rememberSaveable(widthPx, heightPx, "body-layout-v7", stateSaver = bodyOffsetsSaver) {
        mutableStateOf(initialOffsets)
    }

    parts.forEach { part ->
        val resourceId = remember(part.drawableName) {
            context.resources.getIdentifier(part.drawableName, "drawable", context.packageName)
                .takeIf { it != 0 } ?: R.drawable.ic_launcher_foreground
        }
        val position = offsets[part.id] ?: initialOffsets[part.id] ?: Offset.Zero
        val partWidthPx = (part.trayRect.width * 1440f * referenceScale).roundToInt().coerceAtLeast(1)
        val partHeightPx = (part.trayRect.height * 2560f * referenceScale).roundToInt().coerceAtLeast(1)
        val maxDragX = (widthPx - partWidthPx).coerceAtLeast(0).toFloat()
        val maxDragY = (heightPx - partHeightPx).coerceAtLeast(0).toFloat()

        Box(
            modifier = Modifier
                .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                .size(
                    width = with(density) { partWidthPx.toDp() },
                    height = with(density) { partHeightPx.toDp() }
                )
                .pointerInput(part.id, maxDragX, maxDragY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsets = offsets.toMutableMap().also { map ->
                            val current = map[part.id] ?: position
                            map[part.id] = Offset(
                                x = (current.x + dragAmount.x).coerceIn(0f, maxDragX),
                                y = (current.y + dragAmount.y).coerceIn(0f, maxDragY)
                            )
                        }
                    }
                }
        ) {
            Image(
                painter = painterResource(resourceId),
                contentDescription = "Pieza: ${part.id.replace('_', ' ')}. Arrastra para colocarla en Ada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
fun BodyAssembleMenuEntry(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD5B7FF), Color(0xFF8C4FE0))),
                shape
            )
            .border(2.dp, Color.White.copy(alpha = 0.94f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Armar el cuerpo. Toca para jugar." }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.cara_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(42.dp)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(2.dp))
        androidx.compose.material3.Text(
            text = playfulMulticolorLetters("ARMAR EL CUERPO"),
            color = Color.Unspecified,
            fontFamily = PlayfulAdaRounded,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
