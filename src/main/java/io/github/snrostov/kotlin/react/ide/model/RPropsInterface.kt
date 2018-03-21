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

package io.github.snrostov.kotlin.react.ide.model

import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.inspections.SafeDeleteFix
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix

val ClassDescriptor.isReactProps
  get() = RPropsInterface.canWrap(this)

val ClassDescriptor.asReactProps
  get() = RPropsInterface.tryWrap(this)

class RPropsInterface(kotlinClass: ClassDescriptor) : RJsObjInterface(kotlinClass) {
  override val kind: Kind<*>
    get() = Companion


  override fun validateMember(declaration: DeclarationDescriptor, problemsHolder: ProblemsHolder?) =
    super.validateMember(declaration, problemsHolder).also {
      val psi = it?.psi
      val nameIdentifier = psi?.nameIdentifier
      if (nameIdentifier != null && it.name in reservedPropNames) {
        problemsHolder?.registerProblem(
          nameIdentifier,
          "\"${it.name}\" is reserved for React Special Property and cannot be used",
          RenameIdentifierFix(),
          SafeDeleteFix(psi)
        )
      }
    }

  companion object : RJsObjInterface.Kind<RPropsInterface>(React.RProps, "RProps") {
    override val orderInFile: Int = 1
    override val suffix: String = "Props"
    override val rComponentTypeArgument = React.RComponent.P

    override fun create(c: ClassDescriptor) = RPropsInterface(c)

    // https://reactjs.org/warnings/special-props.html
    val reservedPropNames = setOf("key", "ref", "children")
  }
}