package io.github.snrostov.kotlin.react.ide.utils

import io.github.snrostov.kotlin.react.ide.React
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.getAbbreviation

/**
 * Utility classes for matching references to React lib entities.
 *
 * @see React
 */
open class ClassMatcher(parent: FqName, name: String) : SymbolMatcher(parent, name) {
  infix fun matches(type: KotlinType?) =
    matches(type?.constructor)

  infix fun matches(typeConstructor: TypeConstructor?) =
    matches(typeConstructor?.declarationDescriptor)

  infix fun matches(classifier: ClassifierDescriptor?) =
    matches(classifier?.fqNameSafe)

  ////////// builder function (should be used in subclasses)

  protected fun typeParameter(index: Int, name: String) =
    TypeParameterMatcher(index, name)

  protected fun property(name: String) =
    ClassPropertyMatcher(this, name)
}

open class MemberFunctionMatcher(val dispatchReceiver: ClassMatcher, val name: String) :
  SymbolMatcher(dispatchReceiver.fqName, name) {

  open val extensionReceiverType: TypeMatcher? = null

  val typeParameters = mutableListOf<TypeParameterMatcher>()
  val valueParameterTypes = mutableListOf<TypeMatcher>()

  fun matches(resolvedCall: ResolvedCall<out CallableDescriptor>): Boolean =
    matches(resolvedCall.resultingDescriptor)

  fun matches(callable: CallableDescriptor?): Boolean {
    if (callable !is SimpleFunctionDescriptor) return false
    if (callable.kind != CallableMemberDescriptor.Kind.DECLARATION) return false
    if (callable.name.isSpecial) return false
    if (callable.name.identifier != name) return false
    if (callable.typeParameters.size != typeParameters.size) return false

    if (!dispatchReceiver.matches(callable.dispatchReceiverParameter?.type)) return false

    val extensionReceiverType = extensionReceiverType
    if (extensionReceiverType == null) {
      if (callable.extensionReceiverParameter != null) return false
    } else {
      if (!extensionReceiverType.matches(callable.extensionReceiverParameter?.type)) return false
    }

    if (callable.valueParameters.size != valueParameterTypes.size) return false
    // forEachIndexed not worked for some reason (indexes starts with 1)...
    var index = 0
    valueParameterTypes.forEach { typeMatcher ->
      if (!typeMatcher.matches(callable.valueParameters[index++].type)) return false
    }

    return true
  }

  fun findOverride(container: ClassDescriptor) =
    container.unsubstitutedMemberScope
      .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_IDE)
      .filter {
        it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE && it.firstOverridden { matches(it) } != null
      }

  ////////// builder function (should be used in subclasses)

  protected fun parameter(type: TypeMatcher) {
    valueParameterTypes.add(type)
  }

  protected fun typeParameter(index: Int, name: String) =
    TypeParameterMatcher(index, name).also {
      typeParameters.add(it)
    }
}

class ClassPropertyMatcher(cls: ClassMatcher, name: String) : SymbolMatcher(cls.fqName, name) {
  fun matches(callable: CallableDescriptor?) =
    matches(callable?.fqNameOrNull())
}

sealed class TypeMatcher {
  abstract fun matches(type: KotlinType?): Boolean
}

class ExactTypeMatcher(
  val typeConstructor: FqName,
  vararg val typeArguments: TypeMatcher,
  val abbreviation: Boolean = false
) : TypeMatcher() {
  override fun matches(type: KotlinType?): Boolean {
    return if (abbreviation)
      doMatch((type as? AbbreviatedType)?.getAbbreviation())
    else doMatch(type)
  }

  private fun doMatch(type: KotlinType?): Boolean {
    val fqName = type?.constructor?.declarationDescriptor?.fqNameSafe ?: return false
    if (fqName != this.typeConstructor) return false
    if (type.arguments.size != typeArguments.size) return false
    typeArguments.forEachIndexed { index, typeMatcher ->
      if (!typeMatcher.matches(type.arguments[index].type)) return false
    }
    return true
  }
}

fun ExactTypeMatcher(typeConstructorFqName: String, vararg typeArguments: TypeMatcher) =
  ExactTypeMatcher(FqName(typeConstructorFqName), *typeArguments)

class TypeParameterMatcher(val index: Int, val name: String) : TypeMatcher() {
  override fun matches(type: KotlinType?) = true

  fun getProjection(type: KotlinType?) =
    type?.arguments?.getOrNull(index)

  fun getProjectionValue(type: KotlinType?) =
    getProjection(type)?.type

  fun getResolvedType(resolvedCall: ResolvedCall<out CallableDescriptor>): KotlinType? {
    resolvedCall.typeArguments.forEach { (key, value) ->
      if (key.index == index) return value
    }
    return null
  }
}

open class SymbolMatcher(parent: FqName, name: String) {
  val fqName = parent.child(Name.identifier(name))

  infix fun matches(fqName: FqName?) =
    fqName == this.fqName
}