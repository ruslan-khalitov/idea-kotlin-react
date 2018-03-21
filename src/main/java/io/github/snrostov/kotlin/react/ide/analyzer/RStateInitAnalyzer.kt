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

package io.github.snrostov.kotlin.react.ide.analyzer

import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType

/**
 * Collects all <this.><prop> = <value> assignments,
 * where `<this>` is implicit receiver for [stateInitFunction]
 *
 * @see CfgAnalyzer
 */
class RStateInitAnalyzer(
  props: List<RJsObjInterface.Property>,
  val stateInitFunction: DeclarationDescriptor,
  val propsType: KotlinType?
) : CfgAnalyzer() {
  val propsByName = props.associateBy { it.name }

  override fun visitInstruction(
    propsState: PropAssignmentsBuilder,
    instruction: Instruction
  ) {
    when (instruction) {
      is WriteValueInstruction -> {
        // match `<this.><prop>` pattern, where `<this>` is implicit receiver for [stateInitFunction]
        val ref = instruction.lValue as? KtNameReferenceExpression
        val name = ref?.getReferencedName()
        if (name != null) {
          val receiver = instruction.receiverValues.values.singleOrNull() as? ExtensionReceiver
          if (receiver?.declarationDescriptor == stateInitFunction) {
            if (receiver.type == propsType) {
              // todo: match by descriptor rather then by name
              val property = propsByName[name]
              propsState.addInitializedProp(property, getPropValue(instruction))
            }
          }
        }
      }
    }
  }
}