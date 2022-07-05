package screeps.creeps.behaviors

import screeps.*
import screeps.api.*
import screeps.api.structures.Structure
import screeps.creeps.*

abstract class RepairBase(private val priorities: List<StructureConstant>) : Behavior() {

    override fun plan(creep: Creep) {
        if (creep.memory.targetType == TargetType.STRUCTURE && creep.memory.targetId != null) return

        val repairTargets = creep.room.find(FIND_STRUCTURES)
            .asSequence()
            .filter { priorities.contains(it.structureType) }
            .filter { it.hits < it.hitsMax }
            .map { Pair(it, getRepairPriority(it)) }
            .sortedWith(compareBy({it.second}, {it.first.pos.getRangeTo(creep)}))
            .toList()

        if(repairTargets.isNotEmpty()) {
            val target = repairTargets.first().first
            creep.memory.targetType = TargetType.STRUCTURE
            creep.memory.targetStructureType = target.structureType
            creep.memory.targetId = target.id
        } else {
            creep.goIdle()
        }
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetType != TargetType.STRUCTURE) {
            creep.goIdle()
        } else {
            val target = Game.getObjectById<Structure>(creep.memory.targetId)
            if (target != null && (target.hits < target.hitsMax)) {
                when (val repairResult = creep.repair(target)) {
                    OK, ERR_BUSY -> Unit
                    ERR_NOT_IN_RANGE -> creep.moveToTarget(target)
                    else -> creep.sayMessage("Failed to repair $target due to $repairResult")
                }
            } else {
                creep.resetTarget()
            }
        }
    }

    abstract fun getRepairPriority(structure: Structure): Int

}