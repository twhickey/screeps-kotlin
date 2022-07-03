package screeps.creeps.behaviors

import screeps.TargetType
import screeps.api.*
import screeps.api.structures.Structure
import screeps.creeps.*
import screeps.goIdle
import screeps.moveToTarget
import screeps.sayMessage

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
            creep.memory.targetType = TargetType.NONE
        }
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetType != TargetType.STRUCTURE) {
            creep.goIdle()
        } else {
            val target = Game.getObjectById<Structure>(creep.memory.targetId)
            if (target != null) {
                when (val repairResult = creep.repair(target)) {
                    OK -> Unit
                    ERR_NOT_IN_RANGE -> creep.moveToTarget(target)
                    else -> creep.sayMessage("Failed to repair $target due to $repairResult")
                }
            } else {
                creep.goIdle()
            }
        }
    }

    abstract fun getRepairPriority(structure: Structure): Int

}