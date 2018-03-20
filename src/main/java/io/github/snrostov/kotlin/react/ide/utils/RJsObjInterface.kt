package io.github.snrostov.kotlin.react.ide.utils

import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.insepctions.RComponentBuilderExpressionsInspection
import io.github.snrostov.kotlin.react.ide.insepctions.RComponentInspection
import io.github.snrostov.kotlin.react.ide.insepctions.RPropsInspection
import io.github.snrostov.kotlin.react.ide.insepctions.RStateInspection
import io.github.snrostov.kotlin.react.ide.model.RComponentClass
import io.github.snrostov.kotlin.react.ide.model.RPropsInterface
import io.github.snrostov.kotlin.react.ide.model.RStateInterface
import io.github.snrostov.kotlin.react.ide.model.asReactComponent
import io.github.snrostov.kotlin.react.ide.quickfixes.SafeDelete
import io.github.snrostov.kotlin.react.ide.quickfixes.ToVar
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllExactUsages
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.parents
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
  val psi
    get() = kClass.source.getPsi() as? KtClass

  abstract val kind: Kind<*>

  fun findComponents(): List<RComponentClass> {
    val result = mutableListOf<RComponentClass>()
    val psi = psi ?: return listOf()
    psi.processAllExactUsages({
      KotlinClassFindUsagesOptions(psi.project)
    }) {
      if (!it.isNonCodeUsage()) {
        val superTypesList = it.element?.parents?.find { it is KtSuperTypeList } as KtSuperTypeList?
        if (superTypesList != null) {
          val componentClass = superTypesList.containingClass()?.asReactComponent
          if (componentClass != null) {
            val componentTypeArgument = kind.rComponentTypeArgument.getProjectionValue(componentClass.rComponentType)
            if (componentTypeArgument?.constructor?.declarationDescriptor == kClass) {
              result.add(componentClass)
            }
          }
        }
      }
    }

    return result
  }

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

  data class Property(val declaration: VariableDescriptor) {
    val name: String?
      get() = if (declaration.name.isSpecial) null else declaration.name.identifier

    val psi get() = (declaration as? DeclarationDescriptorWithSource)?.source?.getPsi() as? KtProperty

    override fun toString() = declaration.toString()
  }

  /** RProps or RState type reference and [RJsObjInterface] factory */
  abstract class Kind<out T : RJsObjInterface>(val interfaceType: ClassMatcher, val title: String) {
    abstract val orderInFile: Int

    abstract val suffix: String

    abstract val rComponentTypeArgument: TypeParameterMatcher

    fun canWrap(c: ClassDescriptor) = c.getSuperInterfaces().find { interfaceType.matches(it) } != null

    fun tryWrap(c: ClassDescriptor?) = if (c != null && canWrap(c)) create(c) else null

    abstract fun create(c: ClassDescriptor): T
  }
} 