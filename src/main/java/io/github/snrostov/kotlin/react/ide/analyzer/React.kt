package io.github.snrostov.kotlin.react.ide.analyzer

import io.github.snrostov.kotlin.react.ide.utils.ClassMatcher
import io.github.snrostov.kotlin.react.ide.utils.ExactTypeMatcher
import io.github.snrostov.kotlin.react.ide.utils.MemberFunctionMatcher
import org.jetbrains.kotlin.name.FqName

/**
 * Objects for matching references to React lib entities.
 */
object React {
  val react = FqName("react")

  val RProps = ClassMatcher(react, "RProps")

  val RState = ClassMatcher(react, "RState")

  /** `typealias RHandler<P>  = react.RElementBuilder<P>.() -> kotlin.Unit` */
  val RHandler = ClassMatcher(react, "RHandler").fqName

  /** `public open class RBuilder` */
  object RBuilder : ClassMatcher(react, "RBuilder") {
    /**
     * ```
     * public final fun <P : react.RProps, C : react.React.Component<P, *>> child(
     *    klazz: kotlin.reflect.KClass<C>,
     *    handler: react.RHandler<P> /* = react.RElementBuilder<P>.() -> kotlin.Unit */
     * ): react.ReactElement { /* compiled code */ }
     * ```
     */
    object childFunction : MemberFunctionMatcher(this, "child") {
      val P = typeParameter(0, "P")
      val C = typeParameter(1, "C")

      /** `klazz: kotlin.reflect.KClass<C>` */
      val klazz = parameter(
        ExactTypeMatcher("kotlin.reflect.KClass", C)
      )

      /** `handler: react.RHandler<P> /* = react.RElementBuilder<P>.() -> kotlin.Unit */` */
      val handler = parameter(ExactTypeMatcher(RHandler, P, abbreviation = true))
    }
  }

  /** `open class RElementBuilder<out P : react.RProps>(attrs: P) : react.RBuilder` */
  object RElementBuilder : ClassMatcher(react, "RElementBuilder") {
    val attrs = property("attrs")
  }

  /** `public abstract class RComponent<P : react.RProps, S : react.RState> : react.React.Component<P, S>` */
  object RComponent : ClassMatcher(react, "RComponent") {
    val P = typeParameter(0, "P")
    val S = typeParameter(1, "S")

    /** `public open fun S.init(): kotlin.Unit` */
    object stateInitFunction : MemberFunctionMatcher(this, "init") {
      override val extensionReceiverType = S
    }

    /** `public open fun S.init(props: P): kotlin.Unit` */
    object stateInitFromPropsFunction : MemberFunctionMatcher(this, "init") {
      override val extensionReceiverType = S
      val props = parameter(P)
    }
  }
}