package screeps.creeps.behaviors

import screeps.Context
import screeps.TargetType
import screeps.api.*
import screeps.api.structures.*
import screeps.creeps.*
import screeps.resetTarget
import screeps.sayMessage

object Refill: Behavior() {

    override fun update(creep: Creep): CreepState {
        if (creep.store[RESOURCE_ENERGY] < creep.store.getCapacity(RESOURCE_ENERGY)) {
            return CreepState.GETTING_ENERGY
        } else if (creep.memory.nextState != CreepState.UNKNOWN) {
            return CreepState.getState(creep.memory.nextState)
        } else {
            return CreepState.TRANSFERRING_ENERGY
        }
     }

    override fun plan(creep: Creep) {
        if (creep.memory.targetType != TargetType.NONE && creep.memory.targetId != null) return

        if (CreepState.getState(creep.memory.nextState) != CreepState.TRANSFERRING_ENERGY) {
            val storages = Context.myStuctures
                .map { it.value }
                .filter { it.structureType == STRUCTURE_STORAGE }
                .map { it.unsafeCast<StructureStorage>() }
                .filter { it.store.getUsedCapacity(RESOURCE_ENERGY) > creep.store.getCapacity() }
                .sortedBy { it.pos.getRangeTo(creep) }

            if (storages.isNotEmpty()) {
                creep.memory.targetId = storages[0].id
                creep.memory.targetType = TargetType.STRUCTURE
                creep.memory.targetStructureType = STRUCTURE_STORAGE
                return
            }
        }

        val dropped = creep.room.find(FIND_DROPPED_RESOURCES)
            .filter { it.resourceType == RESOURCE_ENERGY && (it.amount > creep.store.getCapacity()) }
            .sortedBy { it.pos.getRangeTo(creep) }

        if (dropped.isNotEmpty()) {
            creep.memory.targetId = dropped[0].id
            creep.memory.targetType = TargetType.DROPPED_RESOURCE
            return
        }

        val containers = Context.myStuctures
            .map { it.value }
            .filter { it.structureType == STRUCTURE_CONTAINER }
            .map { it.unsafeCast<StructureContainer>() }
            .filter { it.store.getUsedCapacity(RESOURCE_ENERGY) > creep.store.getCapacity() }
            .sortedBy { it.pos.getRangeTo(creep) }

        if (containers.isNotEmpty()) {
            creep.memory.targetId = containers[0].id
            creep.memory.targetType = TargetType.STRUCTURE
            creep.memory.targetStructureType = STRUCTURE_CONTAINER
            return
        }

        val sources = creep.room.find(FIND_SOURCES)
            .filter { it.energy > creep.store.getCapacity() }
            .sortedBy { it.pos.getRangeTo(creep) }

        if (sources.isNotEmpty()) {
            creep.memory.targetId = sources[0].id
            creep.memory.targetType = TargetType.SOURCE
            return
        }

        creep.memory.targetType = TargetType.NONE
    }

    private fun<T: HasPosition> getEnergy(creep: Creep, target: T?, executor: (creep: Creep, target: T) -> ScreepsReturnCode) {
        // creep.sayMessage("Getting Energy from $target")
        if (target == null) return
        when (val getResult = executor.invoke(creep, target)) {
            OK -> Unit
            ERR_NOT_IN_RANGE -> creep.moveTo(target)
            else -> creep.sayMessage("Failed to get energy from $target due to $getResult")
        }
    }

    override fun execute(creep: Creep) {
        // creep.sayMessage("TargetType: ${creep.memory.targetType}; TargetStructureType: ${creep.memory.targetStructureType}; TargetId: ${creep.memory.targetId}")
        when (creep.memory.targetType) {
            TargetType.STRUCTURE -> {
                val target = Game.getObjectById<StoreOwner>(creep.memory.targetId)
                getEnergy(creep, target) { c, t -> c.withdraw(t, RESOURCE_ENERGY) }
            }
            TargetType.SOURCE -> {
                var target = Game.getObjectById<Source>(creep.memory.targetId)
                getEnergy(creep, target) { c, s -> c.harvest(s)}
            }
            TargetType.DROPPED_RESOURCE -> {
                val target = Game.getObjectById<Resource>(creep.memory.targetId)
                getEnergy(creep, target) { c, r -> creep.pickup(r) }
            }
            else -> creep.resetTarget()
        }
    }
}