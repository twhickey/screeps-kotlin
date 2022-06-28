package screeps.creeps.behaviors

import screeps.Context
import screeps.api.*
import screeps.api.structures.*
import screeps.creeps.*
import screeps.harvestClosestSource
import screeps.pause

object Refill: Behavior() {

    override fun update(creep: Creep): CreepState {

        if (creep.store[RESOURCE_ENERGY] < creep.store.getCapacity(RESOURCE_ENERGY)) {
            return CreepState.GETTING_ENERGY
        } else {
            return CreepState.TRANSFERRING_ENERGY
        }
     }

    override fun plan(creep: Creep) {
        if (creep.memory.targetId != null) return

        val possibleSources: MutableList<StoreOwner> = Context.myStuctures
            .map { it.value }
            .filter { (it.structureType == STRUCTURE_STORAGE) ||
                    (it.structureType == STRUCTURE_CONTAINER ) }
            .map { unsafeCast<StoreOwner>()}
            .toMutableList()

        possibleSources .addAll(creep.room.find(FIND_SOURCES).map { unsafeCast<StoreOwner>() })
        if (possibleSources.isNotEmpty()) {
            t
        }

    }

    override fun execute(creep: Creep) {

        val target: StoreOwner? = when (creep.memory.targetType) {
            STRUCTURE_STORAGE -> Game.getObjectById<StructureStorage>(creep.memory.targetId)
            STRUCTURE_EXTENSION -> Game.getObjectById<StructureExtension>(creep.memory.targetId)
            STRUCTURE_TOWER -> Game.getObjectById<StructureTower>(creep.memory.targetId)
            STRUCTURE_SPAWN -> Game.getObjectById<StructureSpawn>(creep.memory.targetId)
            else -> null
        }
        if (target != null) {
            val result = creep.transfer(target, RESOURCE_ENERGY)
            if (result == ERR_NOT_IN_RANGE) {
                val moveResult = creep.moveTo(target)
                when (moveResult)
                {
                    ERR_TIRED -> creep.pause()
                }
            }
        }
    }
}