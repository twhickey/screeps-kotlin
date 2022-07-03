package screeps.creeps.behaviors

import screeps.api.*
import screeps.api.structures.Structure
import screeps.creeps.CreepState

object RepairWalls : RepairBase(listOf(STRUCTURE_WALL, STRUCTURE_RAMPART)) {
    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.REPAIR_WALLS
        }
    }

    override fun getRepairPriority(structure: Structure): Int {
        return when (structure.hits) {
            in (0 .. 100000) -> 1
            in (100000 .. 500000) -> 2
            in (500000 .. 1000000) -> 3
            else -> 4
        }
    }
}