package io.github.snrostov.kotlin.react.ide.model

import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.inspections.SafeDeleteFix
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix

val ClassDescriptor.isReactProps
  get() = RPropsInterface.canWrap(this)

val ClassDescriptor.asReactProps
  get() = RPropsInterface.tryWrap(this)

class RPropsInterface(kotlinClass: ClassDescriptor) : RJsObjInterface(kotlinClass) {
  override val kind: Kind<*>
    get() = Companion

  override fun validateMember(declaration: DeclarationDescriptor, problemsHolder: ProblemsHolder?) =
    super.validateMember(declaration, problemsHolder).also {
      val psi = it?.psi
      val nameIdentifier = psi?.nameIdentifier
      if (nameIdentifier != null && it.name in reservedPropNames) {
        problemsHolder?.registerProblem(
          nameIdentifier,
          "\"${it.name}\" is reserved for React Special Property and cannot be used",
          RenameIdentifierFix(),
          SafeDeleteFix(psi)
        )
      }
    }

  companion object : RJsObjInterface.Kind<RPropsInterface>(React.RProps, "RProps") {
    override fun create(c: ClassDescriptor) = RPropsInterface(c)

    // https://reactjs.org/warnings/special-props.html
    val reservedPropNames = setOf("key", "ref", "children")
  }
}