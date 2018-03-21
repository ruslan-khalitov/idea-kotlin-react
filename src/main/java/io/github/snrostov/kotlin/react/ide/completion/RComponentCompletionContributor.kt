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

package io.github.snrostov.kotlin.react.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import io.github.snrostov.kotlin.react.ide.codegen.generateBuilderFunction
import io.github.snrostov.kotlin.react.ide.intentions.CreateRProps
import io.github.snrostov.kotlin.react.ide.model.RComponentClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.source.getPsi

class RComponentCompletionContributor : CompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    guessRComponentSuperType(parameters, result)
  }

  private fun guessRComponentSuperType(
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val element = parameters.position.unwrapped ?: return
    val typeReference = element.getParentOfType<KtTypeReference>(true) ?: return
    val superTypeEntry = typeReference.parent as? KtSuperTypeEntry ?: return
    val ktClass = superTypeEntry.containingClass() ?: return

    result.addElement(RComponentSuperClass(ktClass))
  }

  class RComponentSuperClass(var ktClass: KtClass) : LookupElement() {
    val componentName = ktClass.name
    val propsInterfaceName = "${componentName}Props"
    val ktClassPtr = ktClass.createSmartPointer()

    override fun getLookupString(): String = "RComponent<$propsInterfaceName, RState>(props) { ... }"

    override fun handleInsert(context: InsertionContext) {
      val project = context.project

      context.document.replaceString(
        context.startOffset, context.tailOffset,
        """react.RComponent<react.RProps, react.RState>() {
  override fun react.RBuilder.render() {
    TODO()
  }
}"""
      )
      commit(context)

      generateBoilerplate(context, project)
      ensureBuilderFunctionExists(context)

      commit(context)
      selectRenderFunctionBody(context)
    }

    private fun commit(context: InsertionContext) {
      performDelayedRefactoringRequests(context.project)
      val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
      psiDocumentManager.commitAllDocuments()
      psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
    }

    private fun findActualKtClass(context: InsertionContext): Boolean {
      ktClass = context.file.findElementAt(ktClass.startOffset)?.parents?.find { it is KtClass } as KtClass? ?:
          return false
      return true
    }


    val reactComponent: RComponentClass?
      get() {
        val classDescriptor = ktClass.descriptor as? ClassDescriptor ?: return null
        return RComponentClass(classDescriptor)
      }

    private fun generateBoilerplate(context: InsertionContext, project: Project) {
      if (!findActualKtClass(context)) return
      ktClass.addToShorteningWaitSet()

      val reactComponent = reactComponent ?: return

      // create empty RProps interface
      CreateRProps().create(reactComponent, project)
    }

    private fun ensureBuilderFunctionExists(context: InsertionContext) {
      findActualKtClass(context)
      val reactComponent = reactComponent ?: return

      if (reactComponent.findBuilderFunction() == null) {
        reactComponent.generateBuilderFunction()?.addToShorteningWaitSet()
      }
    }

    private fun selectRenderFunctionBody(context: InsertionContext) {
      if (!findActualKtClass(context)) return
      val reactComponent = reactComponent ?: return

      val renderFunction =
        reactComponent.findRenderFunction().firstOrNull()?.source?.getPsi()  as? KtNamedFunction ?: return

      renderFunction.accept(callExpressionRecursiveVisitor {
        context.editor.selectionModel.setSelection(it.startOffset, it.endOffset)
        context.editor.moveCaret(it.startOffset)
      })
    }
  }
}