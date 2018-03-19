package io.github.snrostov.kotlin.react.ide

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import io.github.snrostov.kotlin.react.ide.analyzer.RComponentBuilderFunction
import io.github.snrostov.kotlin.react.ide.analyzer.RComponentClass
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.psi.KtPsiFactory


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

/**
 * All usages in RComponent declaration will be replaced with RProps/RState.
 * All other usages will be reported.
 */
class SafeDeleteRJsObjInterface(val kind: RJsObjInterface.Kind<*>) {

}

class CreateRJsObjInterface(val kind: RJsObjInterface.Kind<*>) {

}

class ActualizeRComponentBuilderFunction(val function: RComponentBuilderFunction) : LocalQuickFix {
  override fun getName(): String = "Actualize React Component builder function"

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
  }
}

class GenerateRComponentBuilderFunction(val componentClass: RComponentClass) : LocalQuickFix {
  override fun getName(): String = "Generate React Component builder function"

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
  }
}