package io.github.snrostov.kotlin.react.ide.utils

import io.github.snrostov.kotlin.react.ide.utils.PropsState.Companion.BACK_EDGE
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction

/**
 * Visits all control flow graph instructions from sink to enter.
 * and collects props initialization state ([PropsState]).
 */
abstract class CfgAnalyzer {
  val visited = mutableMapOf<Instruction, PropsState>()

  fun getPropsState(instruction: Instruction?): PropsState =
    if (instruction == null) PropsState.EMPTY
    else visited.getOrPut(instruction) {
      // first, temporary mark this instruction for cycle cases
      visited[instruction] = PropsState.BACK_EDGE

      PropsStateBuilder().also { result ->
        instruction.previousInstructions.forEach { prev ->
          if (!prev.dead && !(prev is SubroutineExitInstruction && prev.isError)) {
            result.addBranch(getPropsState(prev))
          }
        }

        visitInstruction(result, instruction)
      }.build()
    }

  /** Should call [propsState].[PropsStateBuilder.addInitializedProp] on prop write */
  abstract fun visitInstruction(propsState: PropsStateBuilder, instruction: Instruction)
}

/**
 * State of [RJsObjInterface] properties values initialization
 *
 * @property initialized Set on initialized props
 * @property symbol For symbolic inequality (see [BACK_EDGE])
 **/
data class PropsState(
  val initialized: Set<RJsObjInterface.Property>,
  private val symbol: Any? = null
) {
  companion object {
    val EMPTY = PropsState(setOf())

    /** Temporary mark this instruction for cycle cases */
    val BACK_EDGE = PropsState(setOf(), Any())
  }
}

class PropsStateBuilder {
  private var initializedProps: Set<RJsObjInterface.Property>? = null

  fun addInitializedProp(property: RJsObjInterface.Property?) {
    if (property != null) {
      initializedProps =
          if (initializedProps == null) setOf(property)
          else initializedProps!! + property
    }
  }

  /** Intersects current set of initialized props (if any) and given [propsState] */
  fun addBranch(propsState: PropsState) {
    if (propsState != PropsState.BACK_EDGE) { // skip back edges (cycles)
      initializedProps =
          if (initializedProps == null) propsState.initialized.toSet()
          else propsState.initialized.filterTo(mutableSetOf()) { it in initializedProps!! }
    }
  }

  fun build() = PropsState(initializedProps?.toSet() ?: setOf())
}