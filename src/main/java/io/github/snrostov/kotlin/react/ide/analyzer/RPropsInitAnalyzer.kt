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

import io.github.snrostov.kotlin.react.ide.React
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.ReadValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.accessedDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType

/**
 * Collects all <this.>attrs.<prop> = <value> assignments,
 * where `<this>` is implicit receiver for [RComponentBuilderExpression] lambda
 *
 * @see CfgAnalyzer
 */
class RPropsInitAnalyzer(
  val context: BindingContext,
  val pseudocode: Pseudocode,
  props: List<RJsObjInterface.Property>,
  val functionLiteral: DeclarationDescriptor,
  val propsType: KotlinType?
) : CfgAnalyzer() {
  val propsByName = props.associateBy { it.name }

  override fun visitInstruction(
    propsState: PropAssignmentsBuilder,
    instruction: Instruction
  ) {
    when (instruction) {
      is WriteValueInstruction -> {
        // match `<this.>attrs.<prop>` pattern, where `<this>` is implicit receiver for lambda
        val dot = instruction.lValue as? KtDotQualifiedExpression
        if (dot != null) {
          // receiver - <this>
          val receiverExpr = dot.receiverExpression as? KtNameReferenceExpression
          val receiverRead = (pseudocode.getElementValue(receiverExpr)?.createdAt as? ReadValueInstruction)
          val receiverCallTarget = receiverRead?.target?.accessedDescriptor
          if (React.RElementBuilder.attrs.matches(receiverCallTarget)) {

            // check that <this> is receiver of our function literal
            val inputValues = receiverRead!!.inputValues
            if (inputValues.size == 1) {
              val receiver = receiverRead.receiverValues[inputValues[0]]
              if (receiver is ExtensionReceiver) {
                if (receiver.declarationDescriptor == functionLiteral) {

                  // type of `<this.>attrs` should be our RProps interface
                  val type = context[BindingContext.EXPRESSION_TYPE_INFO, receiverExpr]
                  if (type?.type == propsType) {
                    val selector = dot.selectorExpression
                    if (selector is KtSimpleNameExpression) {

                      // find correspond property
                      // todo: match by descriptor rather then by name
                      val property = propsByName[selector.getReferencedName()]
                      propsState.addInitializedProp(property, getPropValue(instruction))
                    }
                  }
                }
              }
            }
          }
        }
      }
      is ReadValueInstruction -> {
        // todo: inspect leaking `attrs`
      }
    }
  }
}