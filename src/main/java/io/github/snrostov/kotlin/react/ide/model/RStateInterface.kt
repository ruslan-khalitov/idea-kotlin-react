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