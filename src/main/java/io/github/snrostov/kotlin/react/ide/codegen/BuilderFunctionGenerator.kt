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

import io.github.snrostov.kotlin.react.ide.model.RComponentClass
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

fun RComponentClass.generateBuilderFunction(): KtNamedFunction? {
  val psi = psi ?: return null
  val function = BuilderFunctionGenerator(this).generate()
  return psi.parent.addAfter(function, psi) as? KtNamedFunction
}

class BuilderFunctionGenerator(
  val component: RComponentClass,
  val generateBodyParameter: Boolean = true
) {
  val psiFactory: KtPsiFactory = KtPsiFactory(component.psi!!)
  val propsInterface = component.findPropsInterface()
  val props = propsInterface?.analyze()?.properties ?: listOf()
  val propsByName = props.associateBy { it.name }
  val bodyParameterName = chooseBodyParameterName()

  fun chooseBodyParameterName(): String {
    var name = "body"
    var i = 1
    while (name in propsByName && i < 10) {
      name = "body$i"
      i++
    }
    return name
  }

  fun generate(): KtNamedFunction {
    val text = buildString {
      appendln()
      appendln()
      append("fun react.RBuilder.${component.builderFunctionName.quoteIfNeeded()}(")
      appendln()
      var hasParameters = false
      props.forEach {
        if (hasParameters) appendln(", ")
        else hasParameters = true

        parameter(it)
      }
      if (generateBodyParameter) {
        if (hasParameters) appendln(", ")
        bodyParameter()
      }
      appendln()
      append(") = child(${component.cls.fqNameSafe.asString()}::class) {")
      appendln()
      props.forEach {
        assigment(it)
        appendln()
      }
      if (generateBodyParameter) {
        bodyCall()
      }
      appendln("}")
    }

    val function = psiFactory.createFunction(text)
    function.addToShorteningWaitSet()
    return function
  }

  fun StringBuilder.parameter(it: RJsObjInterface.Property) {
    append(it.name?.quoteIfNeeded())
    append(": ")
    append(renderType(it.declaration.type))
  }

  fun StringBuilder.bodyParameter() {
    append("$bodyParameterName: react.RHandler<${renderType(component.propsType)}> = {}")
  }

  fun StringBuilder.assigment(it: RJsObjInterface.Property) {
    append("attrs.")
    append(it.name?.quoteIfNeeded())
    append(" = ")
    append(it.name?.quoteIfNeeded())
  }

  fun StringBuilder.bodyCall() {
    appendln("$bodyParameterName()")
  }

  fun renderType(type: KotlinType?): String =
    if (type == null) "?"
    else IdeDescriptorRenderers.SOURCE_CODE.renderType(type)
}