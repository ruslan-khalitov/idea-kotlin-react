package io.github.snrostov.kotlin.react.ide.model

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.*
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
}