package io.github.snrostov.kotlin.react.ide.analyzer

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.find.findUsages.FindUsagesOptions
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.findUsages.processAllExactUsages
import org.jetbrains.kotlin.idea.inspections.SafeDeleteFix
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.source.getPsi

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