package io.github.snrostov.kotlin.react.ide.analyzer

import com.intellij.psi.PsiElement
import io.github.snrostov.kotlin.react.ide.utils.toLowerCaseFirst
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType

val ClassDescriptor.isReactComponent: Boolean
  get() = React.RComponent.matches(getSuperClassNotAny())

val ClassDescriptor.asReactComponent
  get() = if (isReactComponent) RComponentClass(this) else null

val KotlinType.asReactComponent
  get() = (constructor.declarationDescriptor as ClassDescriptor?)?.asReactComponent

/** Wrapper for user defined RComponent class */
class RComponentClass(val kotlinClass: ClassDescriptor) {
  val psi
    get() = kotlinClass.source.getPsi() as? KtClass

  val builderFunctionName
    get() = kotlinClass.name.identifier.toLowerCaseFirst()

  fun findBuilderFunction() =
    kotlinClass.getContainingScope()?.parent?.getContributedFunctions(
      Name.identifier(builderFunctionName),
      NoLookupLocation.FROM_IDE
    )?.find {
      React.RBuilder.matches(it.extensionReceiverParameter?.type)
    }?.let {
      RComponentBuilderFunction(this, it)
    }

  fun findStateInitFunctions(): Collection<RComponentStateInitFunction> =
    React.RComponent.stateInitFunction.findOverride(kotlinClass).map {
      RComponentStateInitFunction(this, it, null)
    } + React.RComponent.stateInitFromPropsFunction.findOverride(kotlinClass).map {
      RComponentStateInitFunction(this, it, it.valueParameters[0])
    }

  // todo: support inherited components
  val rComponentType
    get() = kotlinClass.defaultType.constructor.supertypes.find {
      React.RComponent.matches(it.constructor)
    }

  val propsType
    get() = React.RComponent.P.getProjectionValue(rComponentType)

  val propsTypeSimpleName: String?
    get() = simpleTypeName(propsType) ?: "RProps"

  fun findPropsInterface(): RPropsInterface? =
    (propsType?.constructor?.declarationDescriptor as? ClassDescriptor)?.asReactProps

  val stateType
    get() = React.RComponent.S.getProjectionValue(rComponentType)

  val stateTypeSimpleName: String?
    get() = simpleTypeName(stateType) ?: "RState"

  fun findStateInterface(): RStateInterface? =
    (stateType?.constructor?.declarationDescriptor as? ClassDescriptor)?.asReactState

  fun findChildrenPropUsages(): List<PsiElement> = listOf()

  fun isPropsPassedInConstructor(): Boolean = false // todo:

  fun simpleTypeName(type: KotlinType?): String? {
    val name = type?.constructor?.declarationDescriptor?.name ?: return null
    if (name.isSpecial) return null
    else return name.identifier
  }
}