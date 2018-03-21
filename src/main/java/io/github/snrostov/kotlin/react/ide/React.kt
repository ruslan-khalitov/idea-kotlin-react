/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.snrostov.kotlin.react.ide

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
      val handler = parameter(ExactTypeMatcher(
        RHandler,
        P, abbreviation = true))
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