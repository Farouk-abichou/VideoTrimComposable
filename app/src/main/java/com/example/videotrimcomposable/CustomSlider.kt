import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import java.lang.Math.abs


@Composable
@ExperimentalMaterialApi
fun RangeSlider(
    values: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    require(steps >= 0) { "steps should be >= 0" }
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val tickFractions = remember(steps) {
        stepsToTickFractions(steps)
    }

    BoxWithConstraints(
        modifier = modifier
            .minimumTouchTargetSize()
            .requiredSizeIn(minWidth = ThumbRadius * 4, minHeight = ThumbRadius * 2)
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val widthPx = constraints.maxWidth.toFloat()
        val maxPx: Float
        val minPx: Float

        with(LocalDensity.current) {
            maxPx = widthPx - ThumbRadius.toPx()
            minPx = ThumbRadius.toPx()
        }

        fun scaleToUserValue(offset: ClosedFloatingPointRange<Float>) =
            scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

        fun scaleToOffset(userValue: Float) =
            scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

        val rawOffsetStart = remember { mutableStateOf(scaleToOffset(values.start)) }
        val rawOffsetEnd = remember { mutableStateOf(scaleToOffset(values.endInclusive)) }

        CorrectValueSideEffect(
            ::scaleToOffset,
            valueRange,
            minPx..maxPx,
            rawOffsetStart,
            values.start
        )
        CorrectValueSideEffect(
            ::scaleToOffset,
            valueRange,
            minPx..maxPx,
            rawOffsetEnd,
            values.endInclusive
        )

        val scope = rememberCoroutineScope()
        val gestureEndAction = rememberUpdatedState<(Boolean) -> Unit> { isStart ->
            val current = (if (isStart) rawOffsetStart else rawOffsetEnd).value
            // target is a closest anchor to the `current`, if exists
            val target = snapValueToTick(current, tickFractions, minPx, maxPx)
            if (current == target) {
                onValueChangeFinished?.invoke()
                return@rememberUpdatedState
            }

            scope.launch {
                Animatable(initialValue = current).animateTo(
                    target, SliderToTickAnimation,
                    0f
                ) {
                    (if (isStart) rawOffsetStart else rawOffsetEnd).value = this.value
                    onValueChangeState.value.invoke(
                        scaleToUserValue(rawOffsetStart.value..rawOffsetEnd.value)
                    )
                }

                onValueChangeFinished?.invoke()
            }
        }

        val onDrag = rememberUpdatedState<(Boolean, Float) -> Unit> { isStart, offset ->
            val offsetRange = if (isStart) {
                rawOffsetStart.value = (rawOffsetStart.value + offset)
                rawOffsetEnd.value = scaleToOffset(values.endInclusive)
                val offsetEnd = rawOffsetEnd.value
                val offsetStart = rawOffsetStart.value.coerceIn(minPx, offsetEnd)
                offsetStart..offsetEnd
            } else {
                rawOffsetEnd.value = (rawOffsetEnd.value + offset)
                rawOffsetStart.value = scaleToOffset(values.start)
                val offsetStart = rawOffsetStart.value
                val offsetEnd = rawOffsetEnd.value.coerceIn(offsetStart, maxPx)
                offsetStart..offsetEnd
            }

            onValueChangeState.value.invoke(scaleToUserValue(offsetRange))
        }

        val pressDrag = Modifier.rangeSliderPressDragModifier(
            startInteractionSource,
            endInteractionSource,
            rawOffsetStart,
            rawOffsetEnd,
            enabled,
            isRtl,
            widthPx,
            valueRange,
            gestureEndAction,
            onDrag,
        )

        // The positions of the thumbs are dependant on each other.
        val coercedStart = values.start.coerceIn(valueRange.start, values.endInclusive)
        val coercedEnd = values.endInclusive.coerceIn(values.start, valueRange.endInclusive)
        val fractionStart = calcFraction(valueRange.start, valueRange.endInclusive, coercedStart)
        val fractionEnd = calcFraction(valueRange.start, valueRange.endInclusive, coercedEnd)
        val startThumbSemantics = Modifier.sliderSemantics(
            coercedStart,
            tickFractions,
            enabled,
            { value -> onValueChangeState.value.invoke(value..coercedEnd) },
            valueRange.start..coercedEnd,
            steps
        )
        val endThumbSemantics = Modifier.sliderSemantics(
            coercedEnd,
            tickFractions,
            enabled,
            { value -> onValueChangeState.value.invoke(coercedStart..value) },
            coercedStart..valueRange.endInclusive,
            steps
        )

        RangeSliderImpl(
            enabled,
            fractionStart,
            fractionEnd,
            tickFractions,
            colors,
            maxPx - minPx,
            startInteractionSource,
            endInteractionSource,
            modifier = pressDrag,
            startThumbSemantics,
            endThumbSemantics
        )
    }
}





private fun snapValueToTick(
    current: Float,
    tickFractions: List<Float>,
    minPx: Float,
    maxPx: Float
): Float {
    // target is a closest anchor to the `current`, if exists
    return tickFractions
        .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
        ?.run { lerp(minPx, maxPx, this) }
        ?: current
}

private suspend fun AwaitPointerEventScope.awaitSlop(
    id: PointerId,
    type: PointerType
): Pair<PointerInputChange, Float>? {
    var initialDelta = 0f
    val postPointerSlop = { pointerInput: PointerInputChange, offset: Float ->
        pointerInput.consume()
        initialDelta = offset
    }
    val afterSlopResult = awaitHorizontalPointerSlopOrCancellation(id, type, postPointerSlop)
    return if (afterSlopResult != null) afterSlopResult to initialDelta else null
}

private fun stepsToTickFractions(steps: Int): List<Float> {
    return if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Scale x.start, x.endInclusive from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x: ClosedFloatingPointRange<Float>, a2: Float, b2: Float) =
    scale(a1, b1, x.start, a2, b2)..scale(a1, b1, x.endInclusive, a2, b2)

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)


@Stable
fun lerp(start: Offset, stop: Offset, fraction: Float): Offset {
    return Offset(
        lerp(start.x, stop.x, fraction),
        lerp(start.y, stop.y, fraction)
    )
}