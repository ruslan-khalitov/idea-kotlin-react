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
import io.github.snrostov.kotlin.react.ide.analyzer.PropAssignments
import io.github.snrostov.kotlin.react.ide.analyzer.RPropsInitAnalyzer
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Wrapper for `RBuilder.child(C::class) { /* lambda */ }` call.
 */
class RComponentBuilderExpression(
  val componentClass: RComponentClass,
  val rbuilderChildCall: KtCallExpression
) {
  val psi
    get() = rbuilderChildCall.psiOrParent as? KtCallExpression

  val lamdaExpression
    get() = rbuilderChildCall.lambdaArguments.last().getLambdaExpression()

  /**
   * @property [props] List of uninitialized props
   * @property [childrenNotSet] If parameter `body: RHandler<...>` is not called inside `child(C::class){ /* lambds */ }`
   */
  data class UninitializedProps(
    val props: Collection<RJsObjInterface.Property>,
    val childrenNotSet: Boolean = true,
    val assignments: PropAssignments? = null
  )

  fun getPropAssignments(): PropAssignments? {
    val props = componentClass.findPropsInterface()?.analyze()?.properties
        ?: return null

    val context =
      rbuilderChildCall.containingDeclarationForPseudocode?.analyzeWithContent() ?: return null

    val lambdaExpression = lamdaExpression ?: return null
    val functionLiteral =
      context[BindingContext.DECLARATION_TO_DESCRIPTOR, lambdaExpression.functionLiteral]
          ?: return null

    val bodyExpression = lambdaExpression.bodyExpression ?: return null

    val pseudocode = bodyExpression.getContainingPseudocode(context) ?: return null
    val sink = pseudocode.sinkInstruction

    return RPropsInitAnalyzer(context, pseudocode, props, functionLiteral, componentClass.propsType)
      .getPropsAssigments(sink)
  }

  fun collectMissedPropAssignments(): UninitializedProps {
    val props = componentClass.findPropsInterface()?.analyze()?.properties
        ?: return UninitializedProps(listOf())

    val propAssigments = getPropAssignments() ?: return UninitializedProps(props)

    // todo: check `body: RHandler<...>` is not called inside `child(C::class){ /* lambds */ }`
    val childrenNotSet = true

    return UninitializedProps(props.filter { it !in propAssigments.byProperty.keys }, childrenNotSet, propAssigments)
  }

  fun findContainingBuilderFunction(): RComponentBuilderFunction? {
    val parent = rbuilderChildCall.parent as? KtNamedFunction ?: return null
    val parentDescriptor = parent.descriptor as? FunctionDescriptor ?: return null
    val extensionReceiverParameter = parentDescriptor.extensionReceiverParameter ?: return null
    if (!React.RBuilder.matches(extensionReceiverParameter.type)) return null
    return RComponentBuilderFunction(componentClass, parentDescriptor)
  }
}