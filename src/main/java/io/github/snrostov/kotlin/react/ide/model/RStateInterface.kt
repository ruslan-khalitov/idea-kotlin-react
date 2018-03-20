package io.github.snrostov.kotlin.react.ide.model

import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.descriptors.ClassDescriptor

val ClassDescriptor.isReactState
  get() = RStateInterface.canWrap(this)

val ClassDescriptor.asReactState
  get() = RStateInterface.tryWrap(this)

class RStateInterface(kotlinClass: ClassDescriptor) : RJsObjInterface(kotlinClass) {
  override val kind: Kind<*>
    get() = Companion


  companion object : RJsObjInterface.Kind<RStateInterface>(React.RState, "RState") {
    override val orderInFile: Int = 2
    override val suffix: String = "State"
    override val rComponentTypeArgument = React.RComponent.S

    override fun create(c: ClassDescriptor) = RStateInterface(c)
  }
}