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

import io.github.snrostov.kotlin.react.ide.analyzer.RStateInitAnalyzer
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.source.getPsi

class RComponentStateInitFunction(
  val componentClass: RComponentClass,
  val function: FunctionDescriptor,
  val propsParameter: ValueParameterDescriptor?
) {
  val psi
    get() = function.source.getPsi() as? KtFunction

  fun collectMissedStatePropsAssignments(): List<RJsObjInterface.Property> {
    val stateInterface = componentClass.findStateInterface() ?: return listOf()
    val stateProps = stateInterface.analyze().properties
    val psi = psi ?: return listOf()
    val context = psi.analyzeWithContent()

    val pseudoCode = psi.bodyExpression?.getContainingPseudocode(context) ?: return stateProps

    val propsState =
      RStateInitAnalyzer(stateProps, function, componentClass.stateType)
        .getPropsAssigments(pseudoCode.sinkInstruction)

    return stateProps.filter { it !in propsState.byProperty.keys }
  }
}