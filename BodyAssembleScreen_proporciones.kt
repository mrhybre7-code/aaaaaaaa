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
