package screeps.creeps.behaviors

import screeps.TargetType
import screeps.api.*
import screeps.api.structures.Structure
import screeps.creeps.*
import screeps.moveToTarget
import screeps.sayMessage

open class RepairBase(val priorities: List<StructureConstant>) : Behavior() {

    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.BUILDING
        }
    }

    override fun plan(creep: Creep) {
        if (creep.memory.targetType == TargetType.STRUCTURE && creep.memory.targetId != null) return

        val repairPriorities = creep.room.find(FIND_STRUCTURES)
            .asSequence()
            .filter { priorities.contains(it.structureType) }
            .filter { it.hits < it.hitsMax }
            .map { Triple(it, it.hitsMax.toFloat() / it.hits.toFloat(), it.pos.getRangeTo(creep.pos).toFloat()) }
            .sortedByDescending { it.second / it.third }
            .toList()

        val repairTargets = repairPriorities.map { it.first }
        if(repairTargets.isNotEmpty()) {
            val target = repairTargets.first()
            creep.memory.targetType = TargetType.STRUCTURE
            creep.memory.targetStructureType = target.structureType
            creep.memory.targetId = target.id
        } else {
            creep.memory.targetType = TargetType.NONE
        }
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetType != TargetType.STRUCTURE) {
            creep.memory.state = CreepState.IDLE
        } else {
            val target = Game.getObjectById<Structure>(creep.memory.targetId)
            if (target != null) {
                when (val repairResult = creep.repair(target)) {
                    OK -> Unit
                    ERR_NOT_IN_RANGE -> creep.moveToTarget(target)
                    else -> creep.sayMessage("Failed to repair $target due to $repairResult")
                }
            } else {
                creep.memory.state = CreepState.IDLE
            }
        }
    }
}