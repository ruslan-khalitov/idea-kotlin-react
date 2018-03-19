package io.github.snrostov.kotlin.react.ide.analyzer

import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Wrapper for `RBuilder.child(C::class) { /* lambda */ }` call.
 */
class RComponentBuilderExpression(
  val componentClass: RComponentClass,
  val rbuilderChildCall: KtCallExpression
) {
  val psi
    get() = rbuilderChildCall.psiOrParent as? KtCallExpression

  /**
   * @property [props] List of uninitialized props
   * @property [childrenNotSet] If parameter `body: RHandler<...>` is not called inside `child(C::class){ /* lambds */ }`
   */
  data class UninitializedProps(
    val props: Collection<RJsObjInterface.Property>,
    val childrenNotSet: Boolean = true
  )

  fun collectMissedPropAssignments(): UninitializedProps {
    val props = componentClass.findPropsInterface()?.analyze()?.properties
        ?: return UninitializedProps(listOf())

    val context =
      rbuilderChildCall.containingDeclarationForPseudocode?.analyzeWithContent() ?: return UninitializedProps(props)

    val lambdaArgument = rbuilderChildCall.lambdaArguments.last()
    val lambdaExpression = lambdaArgument.getLambdaExpression() ?: return UninitializedProps(props)
    val functionLiteral =
      context[BindingContext.DECLARATION_TO_DESCRIPTOR, lambdaExpression.functionLiteral]
          ?: return UninitializedProps(props)

    val bodyExpression = lambdaExpression.bodyExpression ?: return UninitializedProps(props)

    val pseudocode = bodyExpression.getContainingPseudocode(context) ?: return UninitializedProps(props)
    val sink = pseudocode.sinkInstruction

    val propsState =
      RPropsInitCfgAnalyzer(context, pseudocode, props, functionLiteral, componentClass.propsType)
        .getPropsState(sink)

    // todo: check `body: RHandler<...>` is not called inside `child(C::class){ /* lambds */ }`
    val childrenNotSet = true

    return UninitializedProps(props.filter { it !in propsState.initialized }, childrenNotSet)
  }

  fun findContainingBuilderFunction(): RComponentBuilderFunction? {
    val parent = rbuilderChildCall.parent as? KtNamedFunction ?: return null
    val parentDescriptor = parent.descriptor as? FunctionDescriptor ?: return null
    val extensionReceiverParameter = parentDescriptor.extensionReceiverParameter ?: return null
    if (!React.RBuilder.matches(extensionReceiverParameter.type)) return null
    return RComponentBuilderFunction(componentClass, parentDescriptor)
  }
}