package starter

import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureController

enum class Role {
    UNASSIGNED,
    HARVESTER,
    BUILDER,
    MINER,
    HAULER,
    UPGRADER,
    GUARDIAN
}

enum class Delivery {
    UNASSIGNED,
    SPAWNER,
    UPGRADE,
    BUILD
}

enum class MinionType(val bodyPartPriorities: Map<BodyPartConstant, Int>, val validRoles: List<Role>) {
    UNASSIGNED(emptyMap(), emptyList()),
    GENERIC(mapOf(
        WORK to 1,
        CARRY to 1,
        MOVE to 1
    ), listOf(Role.HARVESTER, Role.BUILDER, Role.UPGRADER)),
    HAULER(mapOf(
        WORK to 1,
        CARRY to 1,
        MOVE to 2
    ), listOf(Role.HARVESTER, Role.BUILDER, Role.UPGRADER)),
    MINER(mapOf(
        WORK to 4,
        MOVE to 1
    ), listOf(Role.MINER)),
    GUARDIAN(mapOf(
        ATTACK to 2,
        MOVE to 1
    ), listOf(Role.GUARDIAN))
}

fun Creep.mine(controller: StructureController) {

    val containers = controller.room.find(FIND_STRUCTURES)
        .filter { it.structureType == STRUCTURE_CONTAINER }
        .filter { !isContainerClaimed(it) }
        .toTypedArray()

    val closestContainer = this.pos.findClosestByPath(containers)
    if (closestContainer != null) {
          if (memory.building && !this.pos.isEqualTo(closestContainer.pos)) {
            memory.building = false
              sayMessage("Is moving to container")
        } else if (!memory.building && this.pos.isEqualTo(closestContainer.pos)) {
            sayMessage("Building now. My pos: $pos; closestContainer: $closestContainer at ${closestContainer.pos}")
            memory.building = true
        }
    } else {
        pause()
        sayMessage("Pausing, no container found")
    }

    if (memory.building) {
        val sources = controller.room.find(FIND_SOURCES)
        val closestSource = this.pos.findClosestByRange(sources)
        if (closestSource != null) {
            val result = harvest(closestSource)
            if (result != OK) {
                sayMessage("Failed to harvest source $closestSource with result $result")
            }
        }
    } else if (closestContainer != null) {
        moveTo(closestContainer)
    } else {
        pause()
    }
}

private fun Creep.isContainerClaimed(container: Structure): Boolean {
    val creepsOnContainer = container.room.find(FIND_MY_CREEPS).filter { (it.memory.role == Role.MINER) && (it.id != this.id) && (it.pos.isEqualTo(container.pos)) }
    return creepsOnContainer.count() > 1
}

fun Creep.guard(controller: StructureController) {
    val targets = controller.room.find(FIND_HOSTILE_CREEPS).filter { it.hits > 0 }
    if (targets.isNotEmpty()) {
        val result = attack(targets[0])
        sayMessage("Attacking ${targets[0]} with result $result")
        when (result) {
            ERR_NOT_IN_RANGE -> moveTo(targets[0].pos)
            else -> Unit
        }
    } else {
        moveTo(Game.spawns.values.firstOrNull()!!)
    }
}

fun Creep.upgrade(controller: StructureController) {

    if (memory.upgrading && store[RESOURCE_ENERGY] == 0) {
        memory.upgrading = false
        sayMessage("is harvesting")
    }
    if (!memory.upgrading && store[RESOURCE_ENERGY] == store.getCapacity()) {
        memory.upgrading = true
        sayMessage("is upgrading")
    }

    if (memory.upgrading) {
        if (upgradeController(controller) == ERR_NOT_IN_RANGE) {
            moveTo(controller.pos)
        }
    } else {
        harvestClosestSource()
    }
}

fun Creep.pause() {
    if (memory.pause < 10) {
        //blink slowly
        if (memory.pause % 3 != 0) say("\uD83D\uDEAC")
        memory.pause++
    } else {
        memory.pause = 0
    }
}

fun Creep.build(assignedRoom: Room = this.room) {
    if (memory.building && store[RESOURCE_ENERGY] == 0) {
        memory.building = false
        sayMessage("is harvesting")
    }
    if (!memory.building && store[RESOURCE_ENERGY] == store.getCapacity()) {
        memory.building = true
        sayMessage("is building")
    }

    if (memory.building) {
        val targets = assignedRoom.find(FIND_MY_CONSTRUCTION_SITES)
        if (targets.isNotEmpty()) {
            val buildResult = build(targets[0])
            if (buildResult == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos)
            } else if (buildResult != OK) {
                sayMessage("Failed to build target ${targets[0]} with result $buildResult")
            }
        }
    } else {
        harvestClosestSource()
    }
}

fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {

    if (memory.building && store[RESOURCE_ENERGY] == 0) {
        memory.building = false
        sayMessage("is harvesting")
    }
    if (!memory.building && store[RESOURCE_ENERGY] == store.getCapacity()) {
        memory.building = true
        sayMessage("is delivering")
    }

    if (memory.building) {
        val targets = toRoom.find(FIND_MY_STRUCTURES)
            .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
            .map { it.unsafeCast<StoreOwner>() }
            .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity(RESOURCE_ENERGY) }

        if (targets.isNotEmpty()) {
            val result = transfer(targets[0], RESOURCE_ENERGY)
            if (result == ERR_NOT_IN_RANGE) {
                val moveResult = moveTo(targets[0].pos)
                when (moveResult)
                {
                    ERR_TIRED -> pause()
                }
            }
        } else {
            val targets = toRoom.find(FIND_MY_CONSTRUCTION_SITES)
            if (targets.isNotEmpty()) {
                val result = build(targets[0])
                if (result == ERR_NOT_IN_RANGE) {
                    moveTo(targets[0].pos)
                }
            }
        }
    } else {
        harvestClosestSource()
    }
}

private fun Creep.harvestClosestSource() {
    val resourcesOnGround = this.room.find(FIND_DROPPED_RESOURCES)
        .filter { it.resourceType == RESOURCE_ENERGY }
        .toTypedArray()

    if (resourcesOnGround.isNotEmpty()) {
        val closestResource = this.pos.findClosestByPath(resourcesOnGround)
        if (closestResource != null) {
            if (pickup(closestResource) == ERR_NOT_IN_RANGE) {
                moveTo(closestResource)
            }
        }
    } else {
        val containers = this.room.find(FIND_STRUCTURES)
            .filter { it.structureType == STRUCTURE_CONTAINER }
            .toTypedArray()

        if (containers.isNotEmpty()) {
            val closestContainer = this.pos.findClosestByRange(containers).unsafeCast<StructureContainer>()
            if (closestContainer != null) {
                if (withdraw(closestContainer, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    moveTo(closestContainer)
                }
            }
        } else {
            val closestActiveSource = this.pos.findClosestByRange(FIND_SOURCES_ACTIVE)
            if (closestActiveSource != null) {
                if (harvest(closestActiveSource) == ERR_NOT_IN_RANGE) {
                    moveTo(closestActiveSource.pos)
                }
            }
        }
    }
}

private fun Creep.sayMessage(msg: String) {
    console.log("Creep ${this.name}: $msg")
}