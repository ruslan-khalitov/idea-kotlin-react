package io.github.snrostov.kotlin.react.ide.analyzer

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.source.getPsi

/**
 * Wrapper for `fun RBuilder.c( /* parameters */ ) = child(Playground::class) { /* lambda */ }` functions
 */
class RComponentBuilderFunction(
  val componentClass: RComponentClass,
  val function: FunctionDescriptor
) {
  val psi
    get() = function.source.getPsi() as? KtFunction

  val name: String?
    get() = if (function.name.isSpecial) null else function.name.identifier
}