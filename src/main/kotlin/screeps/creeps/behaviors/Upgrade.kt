package screeps.creeps.behaviors

import screeps.TargetType
import screeps.api.*
import screeps.api.structures.StructureController
import screeps.creeps.*
import screeps.sayMessage

object Upgrade : Behavior() {
    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.UPGRADING
        }
    }

    override fun plan(creep: Creep) {

        if (creep.memory.targetType != TargetType.NONE) return

        val target = creep.room.controller
        if (target != null) {
            creep.memory.targetType = TargetType.STRUCTURE
            creep.memory.targetStructureType = STRUCTURE_CONTROLLER
            creep.memory.targetId = target.id
        } else {
            creep.memory.targetType == TargetType.NONE
            creep.memory.targetId = null
        }
    }

    override fun execute(creep: Creep) {
        val target = Game.getObjectById<StructureController>(creep.memory.targetId)
        if (target != null) {
            val upgradeResult = creep.upgradeController(target)
            when (upgradeResult) {
                OK -> Unit
                ERR_NOT_IN_RANGE -> creep.moveTo(target)
                else -> creep.sayMessage("Failed to upgrade controller $target due to $upgradeResult")
            }
        } else {
            creep.memory.targetType == TargetType.NONE
        }
    }
}
