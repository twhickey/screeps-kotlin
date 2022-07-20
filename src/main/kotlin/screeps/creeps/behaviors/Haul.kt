package screeps.creeps.behaviors

import screeps.*
import screeps.api.*
import screeps.creeps.*


object Haul : Behavior() {
    override fun update(creep: Creep): CreepState {
        val newState = when (creep.store[RESOURCE_ENERGY] ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.HAUL
        }

        return newState
    }

    override fun plan(creep: Creep) {

        if (creep.memory.targetType != TargetType.NONE && creep.memory.targetId != null) return

        val targets = creep.room.find(FIND_MY_STRUCTURES)
            .filter { (it.structureType == STRUCTURE_STORAGE) }
            .map { Pair(it, creep.pos.getRangeTo(it)) }
            .sortedBy { it.second }
            .map { Pair(it.first.unsafeCast<StoreOwner>(), it.first.structureType) }
            .filter { it.first.store[RESOURCE_ENERGY] < it.first.store.getCapacity(RESOURCE_ENERGY) }

        // creep.sayMessage("Transfer targets: $targets")
        if (targets.isNotEmpty()) {
            creep.memory.targetId = targets[0].first.id
            creep.memory.targetType = TargetType.STRUCTURE
            creep.memory.targetStructureType = targets[0].second
            return
        }

        creep.goIdle()
    }

    override fun execute(creep: Creep) {
        var target = Game.getObjectById<StoreOwner>(creep.memory.targetId)
        if (target != null) {
            when (val result = creep.transfer(target, RESOURCE_ENERGY)) {
                OK, ERR_BUSY -> Unit
                ERR_NOT_IN_RANGE -> creep.moveToTarget(target)
                ERR_FULL -> creep.memory.targetType = TargetType.NONE
                else -> creep.sayMessage("Failed to transfer energy to $target due to $result")
            }
        }
    }
}

