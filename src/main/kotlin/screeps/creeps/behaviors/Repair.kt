package screeps.creeps.behaviors

import screeps.api.*
import screeps.creeps.CreepState

object Repair : RepairBase(listOf(STRUCTURE_SPAWN, STRUCTURE_TOWER, STRUCTURE_CONTAINER, STRUCTURE_ROAD, STRUCTURE_EXTENSION)) {
    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.REPAIR
        }
    }
}
