package screeps.creeps.behaviors

import screeps.api.*
import screeps.creeps.CreepState

object RepairWalls : RepairBase(listOf(STRUCTURE_WALL, STRUCTURE_RAMPART)) {
    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.REPAIR_WALLS
        }
    }
}