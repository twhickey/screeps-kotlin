package screeps.creeps.behaviors

import screeps.api.*
import screeps.api.structures.Structure
import screeps.creeps.CreepState

object Repair : RepairBase(listOf(STRUCTURE_SPAWN, STRUCTURE_TOWER, STRUCTURE_CONTAINER, STRUCTURE_ROAD, STRUCTURE_EXTENSION)) {
    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.REPAIR
        }
    }

    override fun getRepairPriority(structure: Structure): Int {
        return when (structure.hits.toFloat()/(structure.hitsMax.toFloat())) {
            in (0.0 .. 0.1) -> 1
            in (0.1 .. 0.3) -> 2
            in (0.3 .. 0.6) -> 3
            in (0.6 .. 0.8) -> 4
            else -> 5
        }
    }
}
