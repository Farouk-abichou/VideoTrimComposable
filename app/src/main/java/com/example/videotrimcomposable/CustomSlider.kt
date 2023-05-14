package com.example.videotrimcomposable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


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

        val rawOffsetStart = remember{ mutableStateOf(scaleToOffset(values.start)) }
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

/**
 * Object to hold defaults used by [Slider]
 */
object SliderDefaults {

    /**
     * Creates a [SliderColors] that represents the different colors used in parts of the
     * [Slider] in different states.
     *
     * For the name references below the words "active" and "inactive" are used. Active part of
     * the slider is filled with progress, so if slider's progress is 30% out of 100%, left (or
     * right in RTL) 30% of the track will be active, the rest is not active.
     *
     * @param thumbColor thumb color when enabled
     * @param disabledThumbColor thumb colors when disabled
     * @param activeTrackColor color of the track in the part that is "active", meaning that the
     * thumb is ahead of it
     * @param inactiveTrackColor color of the track in the part that is "inactive", meaning that the
     * thumb is before it
     * @param disabledActiveTrackColor color of the track in the "active" part when the Slider is
     * disabled
     * @param disabledInactiveTrackColor color of the track in the "inactive" part when the
     * Slider is disabled
     * @param activeTickColor colors to be used to draw tick marks on the active track, if `steps`
     * is specified
     * @param inactiveTickColor colors to be used to draw tick marks on the inactive track, if
     * `steps` are specified on the Slider is specified
     * @param disabledActiveTickColor colors to be used to draw tick marks on the active track
     * when Slider is disabled and when `steps` are specified on it
     * @param disabledInactiveTickColor colors to be used to draw tick marks on the inactive part
     * of the track when Slider is disabled and when `steps` are specified on it
     */
    @Composable
    fun colors(
        thumbColor: Color = MaterialTheme.colors.primary,
        disabledThumbColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        activeTrackColor: Color = MaterialTheme.colors.primary,
        inactiveTrackColor: Color = activeTrackColor.copy(alpha = InactiveTrackAlpha),
        disabledActiveTrackColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = DisabledActiveTrackAlpha),
        disabledInactiveTrackColor: Color =
            disabledActiveTrackColor.copy(alpha = DisabledInactiveTrackAlpha),
        activeTickColor: Color = contentColorFor(activeTrackColor).copy(alpha = TickAlpha),
        inactiveTickColor: Color = activeTrackColor.copy(alpha = TickAlpha),
        disabledActiveTickColor: Color = activeTickColor.copy(alpha = DisabledTickAlpha),
        disabledInactiveTickColor: Color = disabledInactiveTrackColor
            .copy(alpha = DisabledTickAlpha)
    ): SliderColors = DefaultSliderColors(
        thumbColor = thumbColor,
        disabledThumbColor = disabledThumbColor,
        activeTrackColor = activeTrackColor,
        inactiveTrackColor = inactiveTrackColor,
        disabledActiveTrackColor = disabledActiveTrackColor,
        disabledInactiveTrackColor = disabledInactiveTrackColor,
        activeTickColor = activeTickColor,
        inactiveTickColor = inactiveTickColor,
        disabledActiveTickColor = disabledActiveTickColor,
        disabledInactiveTickColor = disabledInactiveTickColor
    )

    /**
     * Default alpha of the inactive part of the track
     */
    const val InactiveTrackAlpha = 0.24f

    /**
     * Default alpha for the track when it is disabled but active
     */
    const val DisabledInactiveTrackAlpha = 0.12f

    /**
     * Default alpha for the track when it is disabled and inactive
     */
    const val DisabledActiveTrackAlpha = 0.32f

    /**
     * Default alpha of the ticks that are drawn on top of the track
     */
    const val TickAlpha = 0.54f

    /**
     * Default alpha for tick marks when they are disabled
     */
    const val DisabledTickAlpha = 0.12f
}

/**
 * Represents the colors used by a [Slider] and its parts in different states
 *
 * See [SliderDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@Stable
interface SliderColors {

    /**
     * Represents the color used for the sliders's thumb, depending on [enabled].
     *
     * @param enabled whether the [Slider] is enabled or not
     */
    @Composable
    fun thumbColor(enabled: Boolean): State<Color>

    /**
     * Represents the color used for the sliders's track, depending on [enabled] and [active].
     *
     * Active part is filled with progress, so if sliders progress is 30% out of 100%, left (or
     * right in RTL) 30% of the track will be active, the rest is not active.
     *
     * @param enabled whether the [Slider] is enabled or not
     * @param active whether the part of the track is active of not
     */
    @Composable
    fun trackColor(enabled: Boolean, active: Boolean): State<Color>

    /**
     * Represents the color used for the sliders's tick which is the dot separating steps, if
     * they are set on the slider, depending on [enabled] and [active].
     *
     * Active tick is the tick that is in the part of the track filled with progress, so if
     * sliders progress is 30% out of 100%, left (or right in RTL) 30% of the track and the ticks
     * in this 30% will be active, the rest is not active.
     *
     * @param enabled whether the [Slider] is enabled or not
     * @param active whether the part of the track this tick is in is active of not
     */
    @Composable
    fun tickColor(enabled: Boolean, active: Boolean): State<Color>
}


@Composable
private fun RangeSliderImpl(
    enabled: Boolean,
    positionFractionStart: Float,
    positionFractionEnd: Float,
    tickFractions: List<Float>,
    colors: SliderColors,
    width: Float,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    modifier: Modifier,
    startThumbSemantics: Modifier,
    endThumbSemantics: Modifier
) {

    val startContentDescription = getString(Strings.SliderRangeStart)
    val endContentDescription = getString(Strings.SliderRangeEnd)
    Box(modifier.then(DefaultSliderConstraints)) {
        val trackStrokeWidth: Float
        val thumbPx: Float
        val widthDp: Dp
        with(LocalDensity.current) {
            trackStrokeWidth = TrackHeight.toPx()
            thumbPx = ThumbRadius.toPx()
            widthDp = width.toDp()
        }

        val thumbSize = ThumbRadius * 2
        val offsetStart = widthDp * positionFractionStart
        val offsetEnd = widthDp * positionFractionEnd
        Track(
            Modifier.align(Alignment.CenterStart).fillMaxSize(),
            colors,
            enabled,
            positionFractionStart,
            positionFractionEnd,
            tickFractions,
            thumbPx,
            trackStrokeWidth
        )

        SliderThumb(
            Modifier
                .semantics(mergeDescendants = true) { contentDescription = startContentDescription }
                .focusable(true, startInteractionSource)
                .then(startThumbSemantics),
            offsetStart,
            startInteractionSource,
            colors,
            enabled,
            thumbSize
        )
        SliderThumb(
            Modifier
                .semantics(mergeDescendants = true) { contentDescription = endContentDescription }
                .focusable(true, endInteractionSource)
                .then(endThumbSemantics),
            offsetEnd,
            endInteractionSource,
            colors,
            enabled,
            thumbSize
        )
    }
}

@Composable
private fun BoxScope.SliderThumb(
    modifier: Modifier,
    offset: Dp,
    interactionSource: MutableInteractionSource,
    colors: SliderColors,
    enabled: Boolean,
    thumbSize: Dp
) {
    Box(Modifier.padding(start = offset).align(Alignment.CenterStart)) {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> interactions.add(interaction)
                    is PressInteraction.Release -> interactions.remove(interaction.press)
                    is PressInteraction.Cancel -> interactions.remove(interaction.press)
                    is DragInteraction.Start -> interactions.add(interaction)
                    is DragInteraction.Stop -> interactions.remove(interaction.start)
                    is DragInteraction.Cancel -> interactions.remove(interaction.start)
                }
            }
        }

        val elevation = if (interactions.isNotEmpty()) {
            ThumbPressedElevation
        } else {
            ThumbDefaultElevation
        }
        Spacer(
            modifier
                .size(thumbSize, thumbSize)
                .indication(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, radius = ThumbRippleRadius)
                )
                .hoverable(interactionSource = interactionSource)
                .shadow(if (enabled) elevation else 0.dp, CircleShape, clip = false)
                .background(colors.thumbColor(enabled).value, CircleShape)
        )
    }
}

@Composable
private fun Track(
    modifier: Modifier,
    colors: SliderColors,
    enabled: Boolean,
    positionFractionStart: Float,
    positionFractionEnd: Float,
    tickFractions: List<Float>,
    thumbPx: Float,
    trackStrokeWidth: Float
) {
    val inactiveTrackColor = colors.trackColor(enabled, active = false)
    val activeTrackColor = colors.trackColor(enabled, active = true)
    val inactiveTickColor = colors.tickColor(enabled, active = false)
    val activeTickColor = colors.tickColor(enabled, active = true)
    Canvas(modifier) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(thumbPx, center.y)
        val sliderRight = Offset(size.width - thumbPx, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        drawLine(
            inactiveTrackColor.value,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValueEnd = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * positionFractionEnd,
            center.y
        )

        val sliderValueStart = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * positionFractionStart,
            center.y
        )

        drawLine(
            activeTrackColor.value,
            sliderValueStart,
            sliderValueEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        tickFractions.groupBy { it > positionFractionEnd || it < positionFractionStart }
            .forEach { (outsideFraction, list) ->
                drawPoints(
                    list.map {
                        Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                    },
                    PointMode.Points,
                    (if (outsideFraction) inactiveTickColor else activeTickColor).value,
                    trackStrokeWidth,
                    StrokeCap.Round
                )
            }
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

@Composable
private fun CorrectValueSideEffect(
    scaleToOffset: (Float) -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    trackRange: ClosedFloatingPointRange<Float>,
    valueState: MutableState<Float>,
    value: Float
) {
    SideEffect {
        val error = (valueRange.endInclusive - valueRange.start) / 1000
        val newOffset = scaleToOffset(value)
        if (abs(newOffset - valueState.value) > error) {
            if (valueState.value in trackRange) {
                valueState.value = newOffset
            }
        }
    }
}

private fun Modifier.sliderSemantics(
    value: Float,
    tickFractions: List<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
): Modifier {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    return semantics {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                val newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                val resolvedValue = if (steps > 0) {
                    tickFractions
                        .map { lerp(valueRange.start, valueRange.endInclusive, it) }
                        .minByOrNull { abs(it - newValue) } ?: newValue
                } else {
                    newValue
                }
                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue)
                    true
                }
            }
        )
    }.progressSemantics(value, valueRange, steps)
}


private fun Modifier.rangeSliderPressDragModifier(
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    rawOffsetStart: State<Float>,
    rawOffsetEnd: State<Float>,
    enabled: Boolean,
    isRtl: Boolean,
    maxPx: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gestureEndAction: State<(Boolean) -> Unit>,
    onDrag: State<(Boolean, Float) -> Unit>,
): Modifier =
    if (enabled) {
        pointerInput(startInteractionSource, endInteractionSource, maxPx, isRtl, valueRange) {
            val rangeSliderLogic = RangeSliderLogic(
                startInteractionSource,
                endInteractionSource,
                rawOffsetStart,
                rawOffsetEnd,
                onDrag
            )
            coroutineScope {
                forEachGesture {
                    awaitPointerEventScope {
                        val event = awaitFirstDown(requireUnconsumed = false)
                        val interaction = DragInteraction.Start()
                        var posX = if (isRtl) maxPx - event.position.x else event.position.x
                        val compare = rangeSliderLogic.compareOffsets(posX)
                        var draggingStart = if (compare != 0) {
                            compare < 0
                        } else {
                            rawOffsetStart.value > posX
                        }

                        awaitSlop(event.id, event.type)?.let {
                            val slop = viewConfiguration.pointerSlop(event.type)
                            val shouldUpdateCapturedThumb = abs(rawOffsetEnd.value - posX) < slop &&
                                    abs(rawOffsetStart.value - posX) < slop
                            if (shouldUpdateCapturedThumb) {
                                val dir = it.second
                                draggingStart = if (isRtl) dir >= 0f else dir < 0f
                                posX += it.first.positionChange().x
                            }
                        }

                        rangeSliderLogic.captureThumb(
                            draggingStart,
                            posX,
                            interaction,
                            this@coroutineScope
                        )

                        val finishInteraction = try {
                            val success = horizontalDrag(pointerId = event.id) {
                                val deltaX = it.positionChange().x
                                onDrag.value.invoke(draggingStart, if (isRtl) -deltaX else deltaX)
                            }
                            if (success) {
                                DragInteraction.Stop(interaction)
                            } else {
                                DragInteraction.Cancel(interaction)
                            }
                        } catch (e: CancellationException) {
                            DragInteraction.Cancel(interaction)
                        }

                        gestureEndAction.value.invoke(draggingStart)
                        launch {
                            rangeSliderLogic
                                .activeInteraction(draggingStart)
                                .emit(finishInteraction)
                        }
                    }
                }
            }
        }
    } else {
        this
    }

private class RangeSliderLogic(
    val startInteractionSource: MutableInteractionSource,
    val endInteractionSource: MutableInteractionSource,
    val rawOffsetStart: State<Float>,
    val rawOffsetEnd: State<Float>,
    val onDrag: State<(Boolean, Float) -> Unit>,
) {
    fun activeInteraction(draggingStart: Boolean): MutableInteractionSource =
        if (draggingStart) startInteractionSource else endInteractionSource

    fun compareOffsets(eventX: Float): Int {
        val diffStart = abs(rawOffsetStart.value - eventX)
        val diffEnd = abs(rawOffsetEnd.value - eventX)
        return diffStart.compareTo(diffEnd)
    }

    fun captureThumb(
        draggingStart: Boolean,
        posX: Float,
        interaction: Interaction,
        scope: CoroutineScope
    ) {
        onDrag.value.invoke(
            draggingStart,
            posX - if (draggingStart) rawOffsetStart.value else rawOffsetEnd.value
        )
        scope.launch {
            activeInteraction(draggingStart).emit(interaction)
        }
    }
}

@Immutable
private class DefaultSliderColors(
    private val thumbColor: Color,
    private val disabledThumbColor: Color,
    private val activeTrackColor: Color,
    private val inactiveTrackColor: Color,
    private val disabledActiveTrackColor: Color,
    private val disabledInactiveTrackColor: Color,
    private val activeTickColor: Color,
    private val inactiveTickColor: Color,
    private val disabledActiveTickColor: Color,
    private val disabledInactiveTickColor: Color
) : SliderColors {

    @Composable
    override fun thumbColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) thumbColor else disabledThumbColor)
    }

    @Composable
    override fun trackColor(enabled: Boolean, active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (active) activeTrackColor else inactiveTrackColor
            } else {
                if (active) disabledActiveTrackColor else disabledInactiveTrackColor
            }
        )
    }

    @Composable
    override fun tickColor(enabled: Boolean, active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (active) activeTickColor else inactiveTickColor
            } else {
                if (active) disabledActiveTickColor else disabledInactiveTickColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSliderColors

        if (thumbColor != other.thumbColor) return false
        if (disabledThumbColor != other.disabledThumbColor) return false
        if (activeTrackColor != other.activeTrackColor) return false
        if (inactiveTrackColor != other.inactiveTrackColor) return false
        if (disabledActiveTrackColor != other.disabledActiveTrackColor) return false
        if (disabledInactiveTrackColor != other.disabledInactiveTrackColor) return false
        if (activeTickColor != other.activeTickColor) return false
        if (inactiveTickColor != other.inactiveTickColor) return false
        if (disabledActiveTickColor != other.disabledActiveTickColor) return false
        if (disabledInactiveTickColor != other.disabledInactiveTickColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thumbColor.hashCode()
        result = 31 * result + disabledThumbColor.hashCode()
        result = 31 * result + activeTrackColor.hashCode()
        result = 31 * result + inactiveTrackColor.hashCode()
        result = 31 * result + disabledActiveTrackColor.hashCode()
        result = 31 * result + disabledInactiveTrackColor.hashCode()
        result = 31 * result + activeTickColor.hashCode()
        result = 31 * result + inactiveTickColor.hashCode()
        result = 31 * result + disabledActiveTickColor.hashCode()
        result = 31 * result + disabledInactiveTickColor.hashCode()
        return result
    }
}

// Internal to be referred to in tests
internal val ThumbRadius = 10.dp
private val ThumbRippleRadius = 24.dp
private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp

// Internal to be referred to in tests
internal val TrackHeight = 4.dp
private val SliderHeight = 48.dp
private val SliderMinWidth = 144.dp // TODO: clarify min width
private val DefaultSliderConstraints =
    Modifier.widthIn(min = SliderMinWidth)
        .heightIn(max = SliderHeight)

private val SliderToTickAnimation = TweenSpec<Float>(durationMillis = 100)




@OptIn(ExperimentalMaterialApi::class)
@Suppress("ModifierInspectorInfo")
internal fun Modifier.minimumTouchTargetSize(): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "minimumTouchTargetSize"
        // TODO: b/214589635 - surface this information through the layout inspector in a better way
        //  - for now just add some information to help developers debug what this size represents.
        properties["README"] = "Adds outer padding to measure at least 48.dp (default) in " +
                "size to disambiguate touch interactions if the element would measure smaller"
    }
) {
    if (LocalMinimumTouchTargetEnforcement.current) {
        // TODO: consider using a hardcoded value of 48.dp instead to avoid inconsistent UI if the
        // LocalViewConfiguration changes across devices / during runtime.
        val size = LocalViewConfiguration.current.minimumTouchTargetSize
        MinimumTouchTargetModifier(size)
    } else {
        Modifier
    }
}


private class MinimumTouchTargetModifier(val size: DpSize) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val placeable = measurable.measure(constraints)

        // Be at least as big as the minimum dimension in both dimensions
        val width = maxOf(placeable.width, size.width.roundToPx())
        val height = maxOf(placeable.height, size.height.roundToPx())

        return layout(width, height) {
            val centerX = ((width - placeable.width) / 2f).roundToInt()
            val centerY = ((height - placeable.height) / 2f).roundToInt()
            placeable.place(centerX, centerY)
        }
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? MinimumTouchTargetModifier ?: return false
        return size == otherModifier.size
    }

    override fun hashCode(): Int {
        return size.hashCode()
    }
}


@Composable
inline fun <T> remember(calculation: @DisallowComposableCalls () -> T): T =
    currentComposer.cache(false, calculation)

