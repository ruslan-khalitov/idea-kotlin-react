package io.github.snrostov.kotlin.react.ide.utils

import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.SafeDelete
import io.github.snrostov.kotlin.react.ide.ToVar
import io.github.snrostov.kotlin.react.ide.analyzer.RPropsInterface
import io.github.snrostov.kotlin.react.ide.analyzer.RStateInterface
import io.github.snrostov.kotlin.react.ide.insepctions.RComponentBuilderExpressionsInspection
import io.github.snrostov.kotlin.react.ide.insepctions.RComponentInspection
import io.github.snrostov.kotlin.react.ide.insepctions.RPropsInspection
import io.github.snrostov.kotlin.react.ide.insepctions.RStateInspection
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeSubstitution

/**
 * Base for wrappers of user defined RProps/RState interface declarations.
 *
 * Collects valid valid declarations (only `var x: T` members are allowed, no vals, not functions, etc)
 *
 * Note that same RProps/RState interface may be used in more than one RComponent class.
 *
 * [RJsObjInterface] is used in two places:
 * 1. [RPropsInspection]/[RStateInspection]: to report for all invalid members
 * 2. [RComponentInspection.checkStatePropsInit]/[RComponentBuilderExpressionsInspection]:
 *    to validate props & state usage (i.e check that all properies is initialized in builder function / component state initalizer)
 *
 * @see [RPropsInterface]
 * @see [RStateInterface].
 */
abstract class RJsObjInterface(val kClass: ClassDescriptor) {
  abstract val kind: Kind<*>

  /**
   * @param holder If specified, all invalid members will be registered as problems
   *               (see [RJsObjInterfaceInspection]).
   */
  fun analyze(holder: ProblemsHolder? = null) = Analyzed(holder)

  protected open fun validateMember(
    declaration: DeclarationDescriptor,
    problemsHolder: ProblemsHolder?
  ): Property? {
    if (declaration !is CallableMemberDescriptor) return null // ignore non callable members
    if (declaration.kind != CallableMemberDescriptor.Kind.DECLARATION) return null
    val psi = (declaration as? DeclarationDescriptorWithSource)?.source?.getPsi()

    if (declaration !is VariableDescriptor || declaration.isExtension) {
      if (psi != null && problemsHolder != null) {
        problemsHolder.registerProblem(
          psi,
          invalidDeclarationMessage(),
          SafeDelete("invalid ${kind.title} member")
        )
      }

      return null
    }

    if (!declaration.isVar) {
      if (psi != null && problemsHolder != null) {
        val valOrVarKeyword = (psi as KtValVarKeywordOwner).valOrVarKeyword
        val message = invalidDeclarationMessage()
        if (valOrVarKeyword != null) problemsHolder.registerProblem(
          valOrVarKeyword, message,
          ToVar
        )
        else problemsHolder.registerProblem(psi, message)
      }

      return null
    }

    return Property(declaration)
  }

  private fun invalidDeclarationMessage() = "Only var properties are allowed in ${kind.title} interfaces"

  inner class Analyzed(holder: ProblemsHolder?) {
    val properties = kClass.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors().mapNotNull {
      validateMember(it, holder)
    }

    val isEmpty
      get() = properties.isEmpty()
  }

  class Property(val declaration: VariableDescriptor) {
    val name: String?
      get() = if (declaration.name.isSpecial) null else declaration.name.identifier

    val psi get() = (declaration as? DeclarationDescriptorWithSource)?.source?.getPsi() as? KtProperty
  }

  /** RProps or RState type reference and [RJsObjInterface] factory */
  abstract class Kind<out T : RJsObjInterface>(val interfaceType: ClassMatcher, val title: String) {
    fun canWrap(c: ClassDescriptor) = c.getSuperInterfaces().find { interfaceType.matches(it) } != null

    fun tryWrap(c: ClassDescriptor) = if (canWrap(c)) create(c) else null

    abstract fun create(c: ClassDescriptor): T
  }
} 