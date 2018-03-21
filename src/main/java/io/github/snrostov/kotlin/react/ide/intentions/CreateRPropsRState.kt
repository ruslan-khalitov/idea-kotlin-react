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

package io.github.snrostov.kotlin.react.ide.intentions

import com.intellij.codeInsight.FileModificationService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import io.github.snrostov.kotlin.react.ide.codegen.setPropsConstructorArgument
import io.github.snrostov.kotlin.react.ide.codegen.setTypeArgument
import io.github.snrostov.kotlin.react.ide.model.RPropsInterface
import io.github.snrostov.kotlin.react.ide.model.RStateInterface
import io.github.snrostov.kotlin.react.ide.model.asReactComponent
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class CreateRProps : CreateRJsObjInterface(RPropsInterface.Companion)
class CreateRState : CreateRJsObjInterface(RStateInterface.Companion)

abstract class CreateRJsObjInterface(val kind: RJsObjInterface.Kind<*>) : SelfTargetingRangeIntention<KtClass>(
  KtClass::class.java,
  "Create ${kind.title} interface"
) {
  override fun applicabilityRange(element: KtClass): TextRange? {
    val reactComponent = element.asReactComponent ?: return null
    // only if RProps/RState not exists
    if (reactComponent.getPropsOrStateInterface(kind) != null) return null
    return element.textRange
  }

  override fun applyTo(element: KtClass, editor: Editor?) {
    val project = element.project
    val ktClass = element.descriptor as? ClassDescriptor ?: return
    val reactComponent = ktClass.asReactComponent ?: return
    val componentName = reactComponent.cls.name.identifier
    val interfaceName = "$componentName${kind.suffix}"

    // file should contain entries in order: RProps, RState, RComponent
    // so:
    //  - RProps should be before RState
    //  - RState should be before RComponent
    val anchor = when (kind) {
      RPropsInterface -> reactComponent.findStateInterface()?.psi ?: reactComponent.psi
      RStateInterface -> reactComponent.psi
      else -> null
    }
    val parent = anchor?.parent ?: return
    if (!FileModificationService.getInstance().preparePsiElementForWrite(parent)) return

    val psiFactory = KtPsiFactory(project)
    val declaration = buildString {
      appendln("interface $interfaceName: ${kind.interfaceType.fqName} {")
      appendln("}")
    }
    val ktInterface = psiFactory.createClass(declaration)

    val insertedInterface = parent.addBefore(ktInterface, anchor) as KtClass
    insertedInterface.addToShorteningWaitSet()

    reactComponent.setTypeArgument(kind, interfaceName)

    if (kind == RPropsInterface) {
      reactComponent.setPropsConstructorArgument(interfaceName)
    }

    // set cursor at created interface
    val body = insertedInterface.getBody()?.lBrace?.nextSibling
    if (body != null && editor != null) {
      editor.moveCaret(body.startOffset)
    }

    // todo:  actualize bulider function body parameter `body: RHandler<RProps> = {}`

    performDelayedRefactoringRequests(project)
  }
}