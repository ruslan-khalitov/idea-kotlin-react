package io.github.snrostov.kotlin.react.ide.insepctions

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.quickfixes.SafeDelete
import io.github.snrostov.kotlin.react.ide.analyzer.RPropsInterface
import io.github.snrostov.kotlin.react.ide.analyzer.RStateInterface
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.psi.classVisitor

/**
 * Reports problems for:
 *   - invalid members (only var properties are allowed)
 *   - empty interfaces
 *
 * @see [RComponentInspection] for state properties initialization inspections.
 * @see [RComponentBuilderExpressionsInspection] for props initialization inspections.
 */
abstract class RJsObjInterfaceInspection(val kind: RJsObjInterface.Kind<*>) : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classVisitor { rpropsPsi ->
    val classDescriptor = rpropsPsi.resolveToDescriptorIfAny() ?: return@classVisitor
    val rObjInterface = kind.tryWrap(classDescriptor)?.analyze(holder)
    if (rObjInterface != null) {
      if (rObjInterface.isEmpty) {
        holder.registerProblem(
          rpropsPsi,
          "${kind.title} interface is empty",
          ProblemHighlightType.LIKE_UNUSED_SYMBOL,
          SafeDelete("empty ${kind.title} interface")
        )
      }
    }
  }
}

class RPropsInspection : RJsObjInterfaceInspection(RPropsInterface)

class RStateInspection : RJsObjInterfaceInspection(RStateInterface)