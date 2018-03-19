package io.github.snrostov.kotlin.react.ide.model

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import io.github.snrostov.kotlin.react.ide.codegen.RComponentBuilderFunctionGenerator
import io.github.snrostov.kotlin.react.ide.analyzer.PropValue
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.source.getPsi

/**
 * Wrapper for `fun RBuilder.c( /* parameters */ ) = child(Playground::class) { /* lambda */ }` functions
 */
class RComponentBuilderFunction(
  val componentClass: RComponentClass,
  val descriptor: FunctionDescriptor
) {
  val psi
    get() = descriptor.source.getPsi() as? KtFunction

  val name: String?
    get() = if (descriptor.name.isSpecial) null else descriptor.name.identifier

  val expression
    get() = (psi?.bodyExpression as? KtCallExpression)?.let {
      RComponentBuilderExpression(componentClass, it)
    }

  fun actualize(): Boolean {
    val psi = psi ?: return false
    val psiFactory = KtPsiFactory(psi)
    val codegen = RComponentBuilderFunctionGenerator(psiFactory, componentClass)
    val props = componentClass.findPropsInterface()?.analyze()?.properties ?: return false
    val expression = expression ?: return false
    val propAssignments = expression.getPropAssignments() ?: return false

    val valueParameterList = psi.valueParameterList ?: return false
    var prevParameter: KtParameter? = null

    val lambdaBody = expression.lamdaExpression?.bodyExpression ?: return false
    var prevAssignmentExpr: PsiElement? = null

    // add new properties
    with(codegen) {
      props.forEach { prop ->
        val assignment = propAssignments.byProperty[prop]
        if (assignment == null) {
          val parameter = psiFactory.createParameter(buildString { parameter(prop) })
          val instertedParameter = valueParameterList.addParameterAfter(parameter, prevParameter)
          if (prevParameter == null) valueParameterList.addAfter(
            psiFactory.createNewLine(),
            instertedParameter.nextSibling
          )
          else valueParameterList.addBefore(psiFactory.createNewLine(), instertedParameter)
          prevParameter = instertedParameter

          val assignmentExpr = psiFactory.createExpression(buildString { assigment(prop) })
          if (prevAssignmentExpr == null) {
            val insertedExpr = lambdaBody.addBefore(assignmentExpr, lambdaBody.firstChild)
            lambdaBody.addAfter(psiFactory.createNewLine(), insertedExpr)
            prevAssignmentExpr = insertedExpr
          } else {
            val parent = prevAssignmentExpr!!.parent
            val insertedAssignmentExpr = parent.addAfter(assignmentExpr, prevAssignmentExpr!!)
            parent.addBefore(psiFactory.createNewLine(), insertedAssignmentExpr)
            prevAssignmentExpr = insertedAssignmentExpr
          }
        } else {
          // insert parameter for missed element after prev property parameter
          if (assignment.value is PropValue.Parameter && assignment.value.parameter in valueParameterList.parameters) {
            prevParameter = assignment.value.parameter
          }

          // insert assignment after prev property assignment or at start
          val assignmentPsi = assignment.commonParentPsi
          prevAssignmentExpr =
              if (assignmentPsi != null && assignmentPsi.parents.contains(lambdaBody)) assignmentPsi
              else null
        }
      }
    }

    // remove deleted properties
    val unknownPropsAssignments =
      propAssignments.unknownPropsAssignments.mapNotNullTo(mutableSetOf()) { it.commonParentPsi }

    unknownPropsAssignments.forEach {
      removeAssignment(it)
    }

    removeUnusedParameters()

    return true
  }

  private fun removeUnusedParameters() {
    val lambdaBody = expression?.lamdaExpression?.bodyExpression ?: return
    val valueParameterList = psi?.valueParameterList ?: return

    val unusedParameters = valueParameterList.parameters.toMutableSet()
    lambdaBody.accept(referenceExpressionRecursiveVisitor {
      val target = it.mainReference.resolve()
      unusedParameters.remove(target)
    })
    unusedParameters.forEach {
      valueParameterList.removeParameter(it)
    }
  }

  private fun removeAssignment(it: PsiElement) {
    val nextSibling = it.nextSibling
    if (nextSibling is PsiWhiteSpace) nextSibling.delete()
    it.delete()
  }

  fun removeProperty(property: RJsObjInterface.Property) {
    val assignments = mutableListOf<PsiElement>()
    val lamdaExpression = expression?.lamdaExpression ?: return
    val bodyExpression = lamdaExpression.bodyExpression ?: return
    bodyExpression.accept(referenceExpressionRecursiveVisitor {
      val target = it.mainReference.resolve()
      if (target == property.psi) {
        val assignment = it.parents.find { it.parent == bodyExpression }
        if (assignment != null) {
          assignments.add(assignment)
        }
      }
    })

    assignments.forEach {
      removeAssignment(it)
    }

    removeUnusedParameters()
  }
}