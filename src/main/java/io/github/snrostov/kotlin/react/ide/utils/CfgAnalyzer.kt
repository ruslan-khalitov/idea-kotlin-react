package io.github.snrostov.kotlin.react.ide.utils

import com.intellij.psi.PsiElement
import io.github.snrostov.kotlin.react.ide.utils.PropAssignments.Companion.BACK_EDGE
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtReferenceExpression

/**
 * Visits all control flow graph instructions from sink to enter.
 * and collects props initialization state ([PropAssignments]).
 */
abstract class CfgAnalyzer {
  val visited = mutableMapOf<Instruction, PropAssignments>()

  fun getPropsAssigments(instruction: Instruction?): PropAssignments =
    if (instruction == null) PropAssignments.EMPTY
    else visited.getOrPut(instruction) {
      // first, temporary mark this instruction for cycle cases
      visited[instruction] = PropAssignments.BACK_EDGE

      PropAssignmentsBuilder().also { result ->
        instruction.previousInstructions.forEach { prev ->
          if (!prev.dead && !(prev is SubroutineExitInstruction && prev.isError)) {
            result.addBranch(getPropsAssigments(prev))
          }
        }

        visitInstruction(result, instruction)
      }.build()
    }

  /** Should call [propsState].[PropAssignmentsBuilder.addInitializedProp] on prop write */
  abstract fun visitInstruction(propsState: PropAssignmentsBuilder, instruction: Instruction)

  fun getPropValue(instruction: WriteValueInstruction): PropAssignment {
    val assignment = instruction.element
    if (assignment is KtBinaryExpression) {
      val right = assignment.right
      if (right is KtReferenceExpression) {
        val target = right.mainReference.resolve()
        if (target is KtParameter) {
          return PropAssignment(PropValue.Parameter(target), assignment)
        }
      }
    }

    return PropAssignment(PropValue.Mixed, assignment)
  }
}

/**
 * State of [RJsObjInterface] properties values initialization
 *
 * @property byProperty Set on initialized props
 * @property symbol For symbolic inequality (see [BACK_EDGE])
 **/
data class PropAssignments(
  val byProperty: Map<RJsObjInterface.Property, PropAssignment>,
  val unknownPropsAssignments: List<PropAssignment>,
  private val symbol: Any? = null
) {
  companion object {
    val EMPTY = PropAssignments(mapOf(), listOf())

    /** Temporary mark this instruction for cycle cases */
    val BACK_EDGE = PropAssignments(mapOf(), listOf(), Any())
  }
}

class PropAssignment(
  val value: PropValue,
  val commonParentPsi: PsiElement?
)

class PropAssignmentsBuilder {
  private var byProperty: Map<RJsObjInterface.Property, PropAssignment>? = null
  private val unknownPropsAssignments = mutableListOf<PropAssignment>()

  fun addInitializedProp(property: RJsObjInterface.Property?, value: PropAssignment) {
    if (property != null) {
      byProperty =
          if (byProperty == null) mapOf(property to value)
          else byProperty!! + (property to value)
    } else {
      unknownPropsAssignments.add(value)
    }
  }

  /** Intersects current set of initialized props (if any) and given [propAssignments] */
  fun addBranch(propAssignments: PropAssignments) {
    // todo(optimize): collection allocations
    if (propAssignments != PropAssignments.BACK_EDGE) { // skip back edges (cycles)
      byProperty =
          if (byProperty == null) propAssignments.byProperty.toMap()
          else mutableMapOf<RJsObjInterface.Property, PropAssignment>().also { result ->
            val values = byProperty!!
            propAssignments.byProperty.forEach { (prop, assignment) ->
              val prevValue = values[prop]
              if (prevValue != null) {
                result[prop] = PropAssignment(
                  prevValue.value.intersect(assignment.value),
                  firstCommonParent(prevValue.commonParentPsi, assignment.commonParentPsi)
                )
              }
            }
          }

      unknownPropsAssignments.addAll(propAssignments.unknownPropsAssignments)
    }
  }

  fun build() = PropAssignments(byProperty?.toMap() ?: mapOf(), unknownPropsAssignments)
}

sealed class PropValue {
  class Parameter(val parameter: KtParameter) : PropValue()
  object Mixed : PropValue()
}

fun PropValue.intersect(other: PropValue): PropValue = when (this) {
  is PropValue.Parameter -> when {
    other is PropValue.Parameter && other.parameter == parameter -> this
    else -> PropValue.Mixed
  }
  PropValue.Mixed -> PropValue.Mixed
}