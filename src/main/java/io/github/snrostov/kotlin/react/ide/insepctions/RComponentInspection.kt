package io.github.snrostov.kotlin.react.ide.insepctions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import io.github.snrostov.kotlin.react.ide.model.RComponentClass
import io.github.snrostov.kotlin.react.ide.model.asReactComponent
import io.github.snrostov.kotlin.react.ide.quickfixes.AddRComponentPropsConstructorParameter
import io.github.snrostov.kotlin.react.ide.quickfixes.GenerateRComponentBuilderFunction
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.SafeDeleteFix
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.classVisitor
import org.jetbrains.kotlin.resolve.source.getPsi

/**
 * @see [RComponentBuilderExpressionsInspection] for props initialization inspections.
 */
class RComponentInspection : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    classVisitor {
      val nameIdentifier = it.nameIdentifier ?: return@classVisitor
      val classDescriptor = it.resolveToDescriptorIfAny() ?: return@classVisitor
      val reactComponentClass = classDescriptor.asReactComponent
      if (reactComponentClass != null) {
        if (reactComponentClass.hasProps && !reactComponentClass.isPropsPassedInConstructor()) {
          holder.registerProblem(
            nameIdentifier, "Props not passed to constructor",
            AddRComponentPropsConstructorParameter
          )
        }

        checkStatePropsInit(holder, reactComponentClass)

        val builderFunction = reactComponentClass.findBuilderFunction()
        if (builderFunction == null) {
          holder.registerProblem(
            nameIdentifier, "Missed builder function",
            ProblemHighlightType.WEAK_WARNING,
            GenerateRComponentBuilderFunction(reactComponentClass)
          )

          reactComponentClass.findPropsInterface()?.psi?.nameIdentifier?.let {
            holder.registerProblem(
              it, "Missed builder function",
              ProblemHighlightType.WEAK_WARNING,
              GenerateRComponentBuilderFunction(reactComponentClass)
            )
          }
        } else {
          // see [RComponentBuilderExpressionsInspection] for props initialization inspections.
        }
      }
    }

  private fun checkStatePropsInit(holder: ProblemsHolder, reactComponentClass: RComponentClass) {
    val stateInitFunctions = reactComponentClass.findStateInitFunctions()

    val stateInitWithProps = stateInitFunctions.filter { it.propsParameter != null }
    val stateInitWithoutProps = stateInitFunctions.filter { it.propsParameter == null }

    if (stateInitWithProps.isNotEmpty() && stateInitWithoutProps.isNotEmpty()) {
      stateInitFunctions.forEach {
        val stateInitFunctionPsi = it.psi
        stateInitFunctionPsi?.nameIdentifier?.let {
          holder.registerProblem(
            it,
            "Both \"State.init\" and \"State.init(props)\" is overridden",
            SafeDeleteFix(stateInitFunctionPsi)
          )
        }
      }
    }

    if (stateInitWithProps.isEmpty() && stateInitWithoutProps.isEmpty()) {
      // No state init functions. Let check there is no state
      val rStateInterface = reactComponentClass.findStateInterface()
      if (rStateInterface != null) {
        if (!rStateInterface.analyze().isEmpty) {
          reactComponentClass.psi?.nameIdentifier?.let {
            holder.registerProblem(
              it, "Component has state that should be initialized"
              // todo(quick fix): Add all missed state initializers from properties
            )
          }
        }
      }
    }

    if (stateInitWithProps.isNotEmpty() && !reactComponentClass.isPropsPassedInConstructor()) {
      stateInitWithProps.forEach {
        (it.propsParameter?.source?.getPsi() as? KtParameter)?.nameIdentifier?.let {
          holder.registerProblem(
            it,
            "${reactComponentClass.propsTypeSimpleName} is not passed to component constructor",
            AddRComponentPropsConstructorParameter
          )
        }
      }
    }

    stateInitFunctions.forEach { stateInitFunction ->
      val missedStatePropsAssignments = stateInitFunction.collectMissedStatePropsAssignments()

      missedStatePropsAssignments.forEach {
        it.psi?.nameIdentifier?.let { namePsi ->
          holder.registerProblem(
            namePsi,
            "Value is not initialized in component state init function"
          )
          // todo(quick fix): Create/find property `name` and initialize state `name` with it
          // todo(quick fix): Add all missed state initializers from properties
          // todo(quick fix): Initialize in state init function
        }
      }

      if (missedStatePropsAssignments.isNotEmpty()) {
        stateInitFunction.psi?.nameIdentifier?.let {
          holder.registerProblem(
            it,
            "All ${reactComponentClass.stateTypeSimpleName} vars should be initialized"
          )
          // todo(quick fix): Add all missed state initializers from properties
        }
      }
    }
  }
}