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

package io.github.snrostov.kotlin.react.ide.insepctions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import io.github.snrostov.kotlin.react.ide.model.RPropsInterface
import io.github.snrostov.kotlin.react.ide.model.RStateInterface
import io.github.snrostov.kotlin.react.ide.quickfixes.DeleteRJsObjInterface
import io.github.snrostov.kotlin.react.ide.utils.RJsObjInterface
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.classVisitor

/**
 * Reports problems for:
 *   - invalid members (only var properties are allowed)
 *   - empty interfaces
 *
 * @see [RComponentInspection] for state properties initialization inspections.
 * @see [RComponentBuilderExpressionsInspection] for props initialization inspections.
 */
abstract class RJsObjInterfaceInspection(val kind: RJsObjInterface.Kind<*>) : AbstractKotlinInspection() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classVisitor { rpropsPsi ->
    val classDescriptor = rpropsPsi.resolveToDescriptorIfAny() ?: return@classVisitor
    val rObjInterface = kind.tryWrap(classDescriptor)?.analyze(holder)
    if (rObjInterface != null) {
      if (rObjInterface.isEmpty) {
        holder.registerProblem(
          rpropsPsi,
          "There are no props in ${kind.title}",
          ProblemHighlightType.LIKE_UNUSED_SYMBOL,
          DeleteRJsObjInterface(kind)
        )
      }
    }
  }
}

class RPropsInspection : RJsObjInterfaceInspection(RPropsInterface)

class RStateInspection : RJsObjInterfaceInspection(RStateInterface)