package io.github.snrostov.kotlin.react.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import io.github.snrostov.kotlin.react.ide.codegen.removeProperty
import io.github.snrostov.kotlin.react.ide.model.asReactProps
import io.github.snrostov.kotlin.react.ide.model.isReactProps
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class DeletePropertyIntention :
  SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, "Remove React Property") {
  override fun applicabilityRange(element: KtProperty): TextRange? {
    if ((element.containingClass()?.descriptor as? ClassDescriptor)?.isReactProps != true) return null
    return element.textRange
  }

  override fun applyTo(element: KtProperty, editor: Editor?) {
    val descriptor = element.descriptor as? VariableDescriptor
    if (descriptor != null) {
      val reactProps = (element.containingClass()?.descriptor as? ClassDescriptor)?.asReactProps
      val property = RJsObjInterface.Property(descriptor)
      reactProps?.findComponents()?.forEach {
        it.findBuilderFunction()?.removeProperty(property)
      }
    }

    element.delete()
  }
}