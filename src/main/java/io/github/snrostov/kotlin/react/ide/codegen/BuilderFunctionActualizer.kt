/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.snrostov.kotlin.react.ide.codegen

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import io.github.snrostov.kotlin.react.ide.analyzer.PropValue
import io.github.snrostov.kotlin.react.ide.model.RComponentBuilderFunction
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.referenceExpressionRecursiveVisitor

fun RComponentBuilderFunction.actualize(): Boolean {
  val psi = psi ?: return false
  val psiFactory = KtPsiFactory(psi)
  val codegen = BuilderFunctionGenerator(componentClass)
  val props = componentClass.findPropsInterface()?.analyze()?.properties ?: return false
  val expression = expression ?: return false
  val propAssignments = expression.getPropAssignments() ?: return false

  val valueParameterList = psi.valueParameterList ?: return false
  var prevParameter: KtParameter? = null

  val lambdaBody = expression.lamdaExpression?.bodyExpression ?: return false
  var prevAssignmentExpr: PsiElement? = null

  // actualize parameter types
  with(codegen) {
    propAssignments.findPropsWithOutdatedParameterTypes().forEach { (prop, assignment) ->
      if (assignment.value is PropValue.Parameter) {
        val typeReference = psiFactory.createType(codegen.renderType(prop.declaration.type))
        assignment.value.parameter.setTypeReference(typeReference)?.addToShorteningWaitSet()
      }
    }
  }

  // add new properties
  with(codegen) {
    props.forEach { prop ->
      val assignment = propAssignments.byProperty[prop]
      if (assignment == null) {
        val parameter = psiFactory.createParameter(buildString { parameter(prop) })
        val instertedParameter = valueParameterList.addParameterAfter(parameter, prevParameter)
        val newLine = psiFactory.createNewLine()
        if (prevParameter == null) valueParameterList.addAfter(newLine, instertedParameter.nextSibling)
        else valueParameterList.addBefore(newLine, instertedParameter)
        instertedParameter.addToShorteningWaitSet()
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

fun RComponentBuilderFunction.removeProperty(property: RJsObjInterface.Property) {
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

private fun RComponentBuilderFunction.removeAssignment(it: PsiElement) {
  val nextSibling = it.nextSibling
  if (nextSibling is PsiWhiteSpace) nextSibling.delete()
  it.delete()
}

private fun RComponentBuilderFunction.removeUnusedParameters() {
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