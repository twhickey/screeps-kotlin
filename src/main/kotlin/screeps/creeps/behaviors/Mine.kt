package screeps.creeps.behaviors

import screeps.sayMessage
import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.creeps.*
import screeps.creeps.roles.Role
import screeps.pause
import screeps.rooms.getAllStructures

object Mine: Behavior() {
    override fun update(creep: Creep): CreepState {
        return CreepState.MINE
    }

    override fun plan(creep: Creep) {

        val containers = creep.room.getAllStructures
            .map { it.value }
            .filter { it.structureType == STRUCTURE_CONTAINER }
            .filter { !isContainerClaimed(it, creep.id) }
            .toTypedArray()

        val closestContainer = creep.pos.findClosestByPath(containers)
        if (closestContainer != null) {
            creep.memory.targetId = closestContainer.id
            creep.memory.targetType = STRUCTURE_CONTAINER
        } else {
            creep.memory.targetId = null
            creep.memory.targetType = STRUCTURE_CONTAINER
        }
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetId == null) return
        if (creep.memory.targetType != STRUCTURE_CONTAINER) {
            creep.sayMessage("Attempting to mine with Target Type not structure: ${creep.memory.targetType}")
            return
        }
        val targetContainer = Game.getObjectById<StructureContainer>(creep.memory.targetId)
        if (targetContainer == null) {
            creep.sayMessage("TargetContainer unexpectedly null")
            return
        }

        if (creep.pos.isEqualTo(targetContainer.pos)) {
            val sources = creep.room.find(FIND_SOURCES)
            val closestSource = creep.pos.findClosestByRange(sources)
            if (closestSource != null) {
                val result = creep.harvest(closestSource)
                if (result != OK) {
                    creep.sayMessage("Failed to harvest source $closestSource with result $result")
                }
            }
        } else {
            val moveResult = creep.moveTo(targetContainer)
            if (moveResult == ERR_TIRED) {
                creep.pause()
            } else {
                creep.sayMessage("Moving to target $targetContainer at ${targetContainer.pos} failed due to $moveResult")
            }
        }
    }

    private fun isContainerClaimed(container: Structure, myId: String): Boolean {
        val creepsOnContainer = container.room.find(FIND_MY_CREEPS).filter { (it.memory.role == Role.MINER) && (it.id != myId) && (it.memory.targetId == container.id) }
        return creepsOnContainer.isNotEmpty()
    }
}