package io.github.snrostov.kotlin.react.ide.insepctions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.model.RComponentBuilderExpression
import io.github.snrostov.kotlin.react.ide.model.RPropsInterface
import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.model.asReactComponent
import io.github.snrostov.kotlin.react.ide.quickfixes.ActualizeRComponentBuilderFunction
import io.github.snrostov.kotlin.react.ide.utils.reportOnce
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

/**
 * Inspects props initialization in `RBuilder.child(C::class) { /* lambda */ }` calls.
 */
class RComponentBuilderExpressionsInspection : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtTreeVisitorVoid() {
    val visited = mutableSetOf<KtCallExpression>()

    override fun visitCallExpression(callExpression: KtCallExpression) {
      if (callExpression in visited) return
      visited.add(callExpression)

      super.visitCallExpression(callExpression)
      visitCall(callExpression, holder)
    }
  }

  private fun visitCall(it: KtCallExpression, holder: ProblemsHolder) {
    // todo(question): performance?
    val context = it.containingDeclarationForPseudocode?.analyzeWithContent() ?: return
    val resolvedCall = it.getResolvedCall(context) ?: return

    if (!React.RBuilder.childFunction.matches(resolvedCall)) return
    val componentType = React.RBuilder.childFunction.C.getResolvedType(resolvedCall) ?: return
    val reactComponent = componentType.asReactComponent ?: return
    val rComponentBuilderExpression = RComponentBuilderExpression(reactComponent, it)

    checkPropsInit(rComponentBuilderExpression, holder)
  }

  private fun checkPropsInit(builderExpression: RComponentBuilderExpression, holder: ProblemsHolder) {
    val componentClass = builderExpression.componentClass
    val missedPropAssignments = builderExpression.collectMissedPropAssignments()

    val builderFunction = builderExpression.findContainingBuilderFunction()

    if (builderFunction != null) {
      val builderFunctionName = builderFunction.name
      if (builderFunctionName != componentClass.builderFunctionName) {
        builderFunction.psi?.nameIdentifier?.let {
          holder.registerProblem(
            it, "Component builder function should be named as \"${componentClass.builderFunctionName}\"",
            ProblemHighlightType.WEAK_WARNING,
            RenameIdentifierFix()
          )
        }
      }
    }

    val hasUnknownPropsAssignments = missedPropAssignments.assignments?.unknownPropsAssignments?.isNotEmpty() ?: false
    var hasUninitializedProperties = false
    var isUninitialisedChildrenUsed = false

    missedPropAssignments.props.forEach {
      if (it.name !in RPropsInterface.reservedPropNames) {
        // Suggestion to update builder function at RProp property declaration
        if (builderFunction != null && builderFunction.psi?.containingKtFile == it.psi?.containingKtFile) {
          it.psi?.reportOnce { psi ->
            // report at whole element, not name (otherwise it is not noticeable)
            holder.registerProblem(
              psi, "Value is not initialized in component builder function",
              ProblemHighlightType.WEAK_WARNING,
              ActualizeRComponentBuilderFunction(builderFunction)
            )
          }
        }

        hasUninitializedProperties = true
      }
    }

    if (!missedPropAssignments.childrenNotSet) {
      if (builderFunction != null) {
        // Report at children() calls inside render function
        componentClass.findChildrenPropUsages().forEach {
          if (builderFunction.psi?.containingKtFile == it.containingFile) {
            it.reportOnce {
              holder.registerProblem(
                it, "Children is not initialized in component builder function",
                ActualizeRComponentBuilderFunction(builderFunction)
              )
            }
          }
        }
      }

      isUninitialisedChildrenUsed = true
    }

    if (hasUninitializedProperties || isUninitialisedChildrenUsed || hasUnknownPropsAssignments) {
      val message = if (isUninitialisedChildrenUsed) "Children is used in component, but not initialized"
      else if (isUninitialisedChildrenUsed) "All ${componentClass.propsTypeSimpleName} vars should be initialized"
      else "Builder function contains outdated assignments" // hasUnknownPropsAssignments

      if (builderFunction != null) {
        // Report at builder function name
        builderFunction.psi?.nameIdentifier?.let {
          holder.registerProblem(
            it, message,
            ActualizeRComponentBuilderFunction(builderFunction)
          )
        }

        // Suggestion to update builder function at Component class declaration
        if (builderFunction.psi?.containingKtFile == componentClass.psi?.containingKtFile) {
          componentClass.psi?.nameIdentifier?.reportOnce {
            holder.registerProblem(
              it, "Outdated builder function",
              ProblemHighlightType.WEAK_WARNING,
              ActualizeRComponentBuilderFunction(builderFunction)
            )
          }
        }

        if (hasUnknownPropsAssignments) {
          // Suggestion to update builder function at RProps interface declaration
          componentClass.findPropsInterface()?.psi?.nameIdentifier?.let {
            holder.registerProblem(
              it, "Component builder function contains outdated assignments",
              ProblemHighlightType.WEAK_WARNING,
              ActualizeRComponentBuilderFunction(builderFunction)
            )
          }
        }
      } else {
        // Report at RBuilder.child call
        builderExpression.psi?.getCallNameExpression()?.let {
          holder.registerProblem(it, message)
        }
      }
    }
  }
}