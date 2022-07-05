package screeps.creeps.behaviors

import screeps.*
import screeps.api.*
import screeps.creeps.*

object Build : Behavior() {

    private val CONSTRUCTION_SITE_PRIORITY = listOf(STRUCTURE_TOWER, STRUCTURE_CONTAINER, STRUCTURE_EXTENSION, STRUCTURE_WALL, STRUCTURE_RAMPART )

    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.BUILDING
        }
    }

    override fun plan(creep: Creep) {
        if (creep.memory.targetType != TargetType.NONE && creep.memory.targetId != null) return

        val targets = creep.room.find(FIND_MY_CONSTRUCTION_SITES)
        if (targets.isNotEmpty()) {
            var firstTarget = targets[0]
            for (sc in CONSTRUCTION_SITE_PRIORITY) {
                val typedTargets = targets.filter { it.structureType == sc }
                if (typedTargets.isNotEmpty()) {
                    firstTarget = typedTargets[0]
                    break;
                }
            }
            creep.memory.targetType = TargetType.CONSTRUCTION_SITE
            creep.memory.targetStructureType = firstTarget.structureType
            creep.memory.targetId = firstTarget.id
        } else {
            creep.goIdle()
        }
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetType == TargetType.CONSTRUCTION_SITE) {
            val target = Game.getObjectById<ConstructionSite>(creep.memory.targetId)
            if (target != null) {
                // creep.sayMessage("Building $target: ${target.structureType}")
                val buildResult = creep.build(target)
                when (buildResult) {
                    OK -> Unit
                    ERR_NOT_IN_RANGE -> creep.moveToTarget(target)
                    else -> creep.sayMessage("Failed to build $target due to $buildResult")
                }
            } else {
                creep.resetTarget()
            }
        }
    }
}
