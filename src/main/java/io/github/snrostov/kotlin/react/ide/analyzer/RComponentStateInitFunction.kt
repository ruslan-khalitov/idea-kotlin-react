package io.github.snrostov.kotlin.react.ide.analyzer

import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.source.getPsi

class RComponentStateInitFunction(
  val componentClass: RComponentClass,
  val function: FunctionDescriptor,
  val propsParameter: ValueParameterDescriptor?
) {
  val psi
    get() = function.source.getPsi() as? KtFunction

  fun collectMissedStatePropsAssignments(): List<RJsObjInterface.Property> {
    val stateInterface = componentClass.findStateInterface() ?: return listOf()
    val stateProps = stateInterface.analyze().properties
    val psi = psi ?: return listOf()
    val context = psi.analyzeWithContent()

    val pseudoCode = psi.bodyExpression?.getContainingPseudocode(context) ?: return stateProps

    val propsState =
      RStateInitAnalyzer(stateProps, function, componentClass.stateType)
        .getPropsAssigments(pseudoCode.sinkInstruction)

    return stateProps.filter { it !in propsState.byProperty.keys }
  }
}