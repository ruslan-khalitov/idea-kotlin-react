package io.github.snrostov.kotlin.react.ide.quickfixes

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import io.github.snrostov.kotlin.react.ide.codegen.removePropsConstructorArgumentAndSuperTypeCall
import io.github.snrostov.kotlin.react.ide.codegen.setTypeArgument
import io.github.snrostov.kotlin.react.ide.model.RPropsInterface
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.source.getPsi

object ToVar : LocalQuickFix {
  override fun getName(): String = "Replace with var"

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val psiElement = descriptor.psiElement
    if (!FileModificationService.getInstance().preparePsiElementForWrite(psiElement)) return

    psiElement.replace(KtPsiFactory(project).createVarKeyword())
  }
}

class SafeDelete(val kind: String) : LocalQuickFix {
  override fun getName(): String = "Safe delete $kind"

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val psiElement = descriptor.psiElement
    if (!FileModificationService.getInstance().preparePsiElementForWrite(psiElement)) return

    ApplicationManager.getApplication().invokeLater(
      { SafeDeleteHandler.invoke(project, arrayOf(psiElement), false) },
      ModalityState.NON_MODAL
    )
  }
}

class DeleteRJsObjInterface(val kind: RJsObjInterface.Kind<*>) : LocalQuickFix {
  override fun getName() = "Delete empty ${kind.title} interface"

  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val ktClass = (descriptor.psiElement as? KtClass)?.descriptor as? ClassDescriptor ?: return
    val rprops = kind.tryWrap(ktClass) ?: return

    if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return

    rprops.findComponents().forEach {
      // remove RProps from constructor, super type constructor call, and state init
      if (kind == RPropsInterface) {
        it.removePropsConstructorArgumentAndSuperTypeCall()
        it.findStateInitFunctions().forEach {
          it.propsParameter?.source?.getPsi()?.delete()
        }
        // todo: delete in builder function
      } else {
        it.setTypeArgument(kind, kind.interfaceType.fqName.toString())
      }
    }

    rprops.psi?.delete()

    performDelayedRefactoringRequests(project)
  }
}