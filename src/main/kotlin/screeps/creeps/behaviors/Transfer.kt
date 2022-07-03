package screeps.creeps.behaviors

import screeps.*
import screeps.api.*
import screeps.creeps.*

val DELIVERY_PRIORITIES: Map<StructureConstant, Int> = mapOf(
    STRUCTURE_TOWER to 1,
    STRUCTURE_EXTENSION to 3,
    STRUCTURE_SPAWN to 5,
    STRUCTURE_STORAGE to 10
)

object Transfer : Behavior() {
    override fun update(creep: Creep): CreepState {
        return when (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) {
            0 -> CreepState.IDLE
            else -> CreepState.TRANSFERRING_ENERGY
        }
    }

    private fun toStructurePriority(structureType: StructureConstant) :Int {
        return DELIVERY_PRIORITIES.getOrElse(structureType) { 37 }
    }

    override fun plan(creep: Creep) {
        if (creep.memory.targetType != TargetType.NONE && creep.memory.targetId != null) return

        val targets = creep.room.find(FIND_MY_STRUCTURES)
            .filter { (it.structureType == STRUCTURE_TOWER || it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN || it.structureType == STRUCTURE_STORAGE) }
            .map { Triple(it, toStructurePriority(it.structureType), creep.pos.getRangeTo(it)) }
            .sortedWith(compareBy({it.second}, {it.third}))
            .map { Pair(it.first.unsafeCast<StoreOwner>(), it.first.structureType) }
            .filter { it.first.store[RESOURCE_ENERGY] < it.first.store.getCapacity(RESOURCE_ENERGY) }

        if (targets.isNotEmpty()) {
            creep.memory.targetId = targets[0].first.id
            creep.memory.targetType = TargetType.STRUCTURE
            creep.memory.targetStructureType = targets[0].second
            return
        }

        creep.memory.state = CreepState.IDLE
    }

    override fun execute(creep: Creep) {
        var target = Game.getObjectById<StoreOwner>(creep.memory.targetId)
        if (target != null) {
            when (val result = creep.transfer(target, RESOURCE_ENERGY)) {
                OK -> Unit
                ERR_NOT_IN_RANGE -> creep.moveToTarget(target)
                ERR_FULL -> creep.memory.targetType = TargetType.NONE
                else -> creep.sayMessage("Failed to transfer energy to $target due to $result")
            }
        }
    }
}

