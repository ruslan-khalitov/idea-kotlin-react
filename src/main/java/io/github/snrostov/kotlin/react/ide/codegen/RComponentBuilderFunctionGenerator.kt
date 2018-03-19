package io.github.snrostov.kotlin.react.ide.codegen

import io.github.snrostov.kotlin.react.ide.analyzer.RComponentClass
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

class RComponentBuilderFunctionGenerator(
  val psiFactory: KtPsiFactory,
  val component: RComponentClass,
  val generateBodyParameter: Boolean = true
) {
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
      append("fun RBuilder.${component.builderFunctionName.quoteIfNeeded()}(")
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
      append(") = child(${component.kotlinClass.fqNameSafe.asString()}::class) {")
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

    return psiFactory.createFunction(text)
  }

  fun StringBuilder.bodyCall() {
    appendln("$bodyParameterName()")
  }

  fun StringBuilder.assigment(it: RJsObjInterface.Property) {
    append("attrs.")
    append(it.name?.quoteIfNeeded())
    append(" = ")
    append(it.name?.quoteIfNeeded())
  }

  fun StringBuilder.parameter(it: RJsObjInterface.Property) {
    append(it.name?.quoteIfNeeded())
    append(": ")
    append(createType(it.declaration.type))
  }

  fun StringBuilder.bodyParameter() {
    append("$bodyParameterName: RHandler<${createType(component.propsType)}> = {}")
  }

  fun createType(type: KotlinType?): String =
    if (type == null) "?"
    else type.toString() // todo: prop way to render type to string with fqn
}