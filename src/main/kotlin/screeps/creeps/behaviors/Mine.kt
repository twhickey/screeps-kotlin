package screeps.creeps.behaviors

import screeps.sayMessage
import screeps.Context
import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.creeps.building
import screeps.creeps.role
import screeps.creeps.roles.Role
import screeps.creeps.targetId
import screeps.pause
import screeps.rooms.getAllStructures

object Mine: Behavior() {
    override fun update(creep: Creep) {

        val containers = creep.room.getAllStructures
            .map { it.value }
            .filter { it.structureType == STRUCTURE_CONTAINER }
            .filter { !isContainerClaimed(it, creep.id) }
            .toTypedArray()

        val closestContainer = creep.pos.findClosestByPath(containers)
        if (closestContainer != null) {
            creep.memory.targetId = closestContainer.id
            if (creep.memory.building && !creep.pos.isEqualTo(closestContainer.pos)) {
                creep.memory.building = false
                creep.sayMessage("Is moving to container")
            } else if (!creep.memory.building && creep.pos.isEqualTo(closestContainer.pos)) {
                creep.sayMessage("Building now. My pos: ${creep.pos}; closestContainer: $closestContainer at ${closestContainer.pos}")
                creep.memory.building = true
            }
        } else {
            creep.pause()
            creep.sayMessage("Pausing, no container found")
        }
    }

    override fun execute(creep: Creep) {

        val closestContainer = Game.getObjectById<StructureContainer>(creep.memory.targetId)

        if (creep.memory.building) {
            val sources = creep.room.find(FIND_SOURCES)
            val closestSource = creep.pos.findClosestByRange(sources)
            if (closestSource != null) {
                val result = creep.harvest(closestSource)
                if (result != OK) {
                    creep.sayMessage("Failed to harvest source $closestSource with result $result")
                }
            }
        } else if (closestContainer != null) {
            creep.moveTo(closestContainer)
        } else {
            creep.pause()
        }
    }

    private fun isContainerClaimed(container: Structure, myId: String): Boolean {
        val creepsOnContainer = container.room.find(FIND_MY_CREEPS).filter { (it.memory.role == Role.MINER) && (it.id != myId) && (it.pos.isEqualTo(container.pos)) }
        return creepsOnContainer.isNotEmpty()
    }
}