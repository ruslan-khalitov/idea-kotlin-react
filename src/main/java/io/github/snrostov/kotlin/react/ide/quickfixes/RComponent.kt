package io.github.snrostov.kotlin.react.ide.quickfixes

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import io.github.snrostov.kotlin.react.ide.codegen.BuilderFunctionGenerator
import io.github.snrostov.kotlin.react.ide.codegen.actualize
import io.github.snrostov.kotlin.react.ide.codegen.setPropsConstructorArgument
import io.github.snrostov.kotlin.react.ide.model.RComponentBuilderFunction
import io.github.snrostov.kotlin.react.ide.model.RComponentClass
import io.github.snrostov.kotlin.react.ide.model.asReactComponent
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.parents

class GenerateRComponentBuilderFunction(val componentClass: RComponentClass) : LocalQuickFix {
  override fun getName(): String = "Generate Component builder function"

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
    val psi = componentClass.psi ?: return
    val function = BuilderFunctionGenerator(KtPsiFactory(project), componentClass).generate()
    (psi.parent.addAfter(function, psi) as? KtNamedFunction)?.addToShorteningWaitSet()
    performDelayedRefactoringRequests(project)
  }
}

class ActualizeRComponentBuilderFunction(val function: RComponentBuilderFunction) : LocalQuickFix {
  override fun getName(): String = "Actualize React Component builder function"

  override fun getFamilyName(): String = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(function.psi)) return
    function.actualize()
    performDelayedRefactoringRequests(project)
  }
}

object AddRComponentPropsConstructorParameter : LocalQuickFix {
  override fun getName() = "Add props constructor parameter"

  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val ktClass = descriptor.psiElement?.parents?.find { it is KtClass } as KtClass? ?: return
    val reactComponent = ktClass.asReactComponent ?: return
    val propsType = reactComponent.propsType ?: return

    if (!FileModificationService.getInstance().preparePsiElementForWrite(ktClass)) return

    reactComponent.setPropsConstructorArgument(
      IdeDescriptorRenderers.SOURCE_CODE.renderType(propsType)
    )

    performDelayedRefactoringRequests(project)
  }
}