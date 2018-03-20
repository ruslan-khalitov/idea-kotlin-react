package io.github.snrostov.kotlin.react.ide.model

import com.intellij.psi.PsiElement
import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import io.github.snrostov.kotlin.react.ide.utils.toLowerCaseFirst
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType

val ClassDescriptor.isReactComponent: Boolean
  get() = React.RComponent.matches(getSuperClassNotAny())

val ClassDescriptor.asReactComponent
  get() = if (isReactComponent) RComponentClass(this) else null

val KotlinType.asReactComponent
  get() = (constructor.declarationDescriptor as ClassDescriptor?)?.asReactComponent

val KtClass.asReactComponent
  get() = (descriptor as? ClassDescriptor)?.asReactComponent

/** Wrapper for user defined RComponent class */
class RComponentClass(val cls: ClassDescriptor) {
  val psi
    get() = cls.source.getPsi() as? KtClass

  val builderFunctionName
    get() = cls.name.identifier.toLowerCaseFirst()

  fun findBuilderFunction() =
    cls.getContainingScope()?.parent?.getContributedFunctions(
      Name.identifier(builderFunctionName),
      NoLookupLocation.FROM_IDE
    )?.find {
      React.RBuilder.matches(it.extensionReceiverParameter?.type)
    }?.let {
      RComponentBuilderFunction(this, it)
    }

  fun findStateInitFunctions(): Collection<RComponentStateInitFunction> =
    React.RComponent.stateInitFunction.findOverride(cls).map {
      RComponentStateInitFunction(this, it, null)
    } + React.RComponent.stateInitFromPropsFunction.findOverride(cls).map {
      RComponentStateInitFunction(this, it, it.valueParameters[0])
    }

  // todo: support inherited components
  val rComponentType
    get() = cls.defaultType.constructor.supertypes.find {
      React.RComponent.matches(it.constructor)
    }

  fun <T : RJsObjInterface> getPropsOrStateType(kind: RJsObjInterface.Kind<T>) =
    kind.rComponentTypeArgument.getProjectionValue(rComponentType)

  fun <T : RJsObjInterface> getPropsOrStateInterface(kind: RJsObjInterface.Kind<T>) =
    kind.tryWrap((getPropsOrStateType(kind)?.constructor?.declarationDescriptor as? ClassDescriptor))

  val propsType
    get() = getPropsOrStateType(RPropsInterface)

  val propsTypeSimpleName: String?
    get() = simpleTypeName(propsType) ?: "RProps"

  val hasProps: Boolean
    get() = !React.RProps.matches(propsType)

  fun findPropsInterface(): RPropsInterface? =
    getPropsOrStateInterface(RPropsInterface)

  val stateType
    get() = getPropsOrStateType(RStateInterface)

  val stateTypeSimpleName: String?
    get() = simpleTypeName(stateType) ?: "RState"

  fun findStateInterface(): RStateInterface? =
    getPropsOrStateInterface(RStateInterface)

  fun findChildrenPropUsages(): List<PsiElement> = listOf() // todo: findChildrenPropUsages

  fun isPropsPassedInConstructor(): Boolean {
    psi?.superTypeListEntries?.forEach {
      if (React.RComponent.matches(it.typeReference) && it is KtSuperTypeCallEntry) {
        if (it.valueArguments.getOrNull(0) != null) {
          return true
        }
      }
    }

    return false
  }

  fun simpleTypeName(type: KotlinType?): String? {
    val name = type?.constructor?.declarationDescriptor?.name ?: return null
    if (name.isSpecial) return null
    else return name.identifier
  }
}