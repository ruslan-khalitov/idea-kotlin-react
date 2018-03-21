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

import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.model.RComponentClass
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createPrimaryConstructorParameterListIfAbsent

fun RComponentClass.newPsiFactory() = KtPsiFactory(psi!!)

fun RComponentClass.setTypeArgument(
  kind: RJsObjInterface.Kind<*>,
  interfaceName: String
) {
  psi?.superTypeListEntries?.forEach {
    val userType = it.typeAsUserType
    if (React.RComponent.matches(userType)) {
      val typeProjection = userType!!.typeArguments.getOrNull(kind.rComponentTypeArgument.index)
      if (typeProjection != null) {
        val typeRef = newPsiFactory().createTypeArgument(interfaceName)
        (typeProjection.replace(typeRef) as? KtElement)
        userType.addToShorteningWaitSet()
      }
      return
    }
  }
}

fun RComponentClass.removePropsConstructorArgument() {
  psi?.primaryConstructor?.delete()
}

fun RComponentClass.setPropsConstructorArgument(propsType: String) {
  val psi = psi ?: return

  removePropsConstructorArgument()

  psi.createPrimaryConstructorParameterListIfAbsent()
    .addParameter(newPsiFactory().createParameter("props: $propsType"))
    .addToShorteningWaitSet()

  if (!isPropsPassedInConstructor()) {
    psi.superTypeListEntries.forEach {
      if (React.RComponent.matches(it.typeReference)) {
        val type = it.typeReference?.text
        (it.replace(newPsiFactory().createSuperTypeCallEntry("$type(props)")) as KtElement)
      }
    }
  }
}

fun RComponentClass.removePropsConstructorArgumentAndSuperTypeCall() {
  val psi = psi ?: return

  removePropsConstructorArgument()

  psi.superTypeListEntries.forEach {
    if (React.RComponent.matches(it.typeReference)) {
      val stateType = stateType?.let {
        IdeDescriptorRenderers.SOURCE_CODE.renderType(it)
      } ?: "?"
      val type = "react.RComponent<react.RProps, $stateType>()"
      (it.replace(newPsiFactory().createSuperTypeCallEntry(type)) as KtElement).addToShorteningWaitSet()
    }
  }
}

