package fr.mandarine.todolist.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalInputModeManager
import kotlinx.coroutines.launch

private const val PRESS_WASH_ALPHA = 0.04f
private const val NO_WASH = 0f
private const val PAPER_INDICATION_HASH = 0x9A9E5

object PaperIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressWashNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === PaperIndication

    override fun hashCode(): Int = PAPER_INDICATION_HASH
}

private class PressWashNode(private val interactionSource: InteractionSource) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val wash = Animatable(NO_WASH)
    private var focused by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> launch { washTo(1f, PaperMotion.pressWash) }
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> launch {
                        washTo(NO_WASH, PaperMotion.pressRelease)
                    }
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }
            }
        }
    }

    private suspend fun washTo(target: Float, spec: SpringSpec<Float>) {
        wash.animateTo(target, spec) { this@PressWashNode.invalidateDraw() }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val palette = currentValueOf(LocalPaperPalette)
        val pressed = wash.value
        if (pressed > NO_WASH) {
            drawRect(palette.ink.copy(alpha = PRESS_WASH_ALPHA * pressed))
        }
        if (focused && currentValueOf(LocalInputModeManager).inputMode == InputMode.Keyboard) {
            drawRect(palette.pencil, style = Stroke(PaperDimens.rule.toPx()))
        }
    }
}
