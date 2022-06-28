package screeps

import screeps.api.*
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureController
import screeps.creeps.behaviors.Mine
import screeps.creeps.building
import screeps.creeps.pause
import screeps.creeps.role
import screeps.creeps.roles.Role.DEFENSE_BUILDER
import screeps.creeps.roles.Role.REPAIRER
import screeps.creeps.upgrading

fun Creep.mine(controller: StructureController) {
    Mine.update(this)
    Mine.execute(this)
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

val CONSTRUCTION_SITE_PRIORITY = listOf(STRUCTURE_TOWER, STRUCTURE_CONTAINER, STRUCTURE_EXTENSION, STRUCTURE_WALL, STRUCTURE_RAMPART )

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
            var firstTarget = targets[0]
            for (sc in CONSTRUCTION_SITE_PRIORITY) {
                val typedTargets = targets.filter { it.structureType == sc }
                if (typedTargets.isNotEmpty()) {
                    firstTarget = typedTargets[0]
                    break;
                }
            }
            val buildResult = build(firstTarget)
            if (buildResult == ERR_NOT_IN_RANGE) {
                moveTo(firstTarget.pos)
            } else if (buildResult != OK) {
                sayMessage("Failed to build target ${targets[0]} with result $buildResult")
            }
        }
    } else {
        harvestClosestSource()
    }
}

val NORMAL_REPAIR_PRIORITIES = listOf(STRUCTURE_SPAWN, STRUCTURE_TOWER, STRUCTURE_CONTAINER, STRUCTURE_ROAD, STRUCTURE_EXTENSION)
val DEFENSE_REPAIR_PRIORITIES = listOf(STRUCTURE_WALL, STRUCTURE_RAMPART)

fun Creep.repair(assignedRoom: Room = this.room) {
    if (memory.building && store[RESOURCE_ENERGY] == 0) {
        memory.building = false
        sayMessage("is harvesting")
    }
    if (!memory.building && store[RESOURCE_ENERGY] == store.getCapacity()) {
        memory.building = true
        sayMessage("is repairing")
    }

    val priorities = when (memory.role) {
        REPAIRER -> NORMAL_REPAIR_PRIORITIES
        DEFENSE_BUILDER -> DEFENSE_REPAIR_PRIORITIES
        else -> NORMAL_REPAIR_PRIORITIES
    }

    if (memory.building) {
        val repairPriorities = assignedRoom.find(FIND_STRUCTURES)
            .asSequence()
            .filter { priorities.contains(it.structureType) }
            .filter { it.hits < it.hitsMax }
            .map { Triple(it, it.hitsMax.toFloat() / it.hits.toFloat(), it.pos.getRangeTo(this.pos).toFloat()) }
            .sortedByDescending { it.second / it.third }
            .toList()

        val repairTargets = repairPriorities.map { it.first }
        if(repairTargets.isNotEmpty()) {
            if (repair(repairTargets[0]) == ERR_NOT_IN_RANGE) {
                moveTo(repairTargets[0])
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
            .filter { (it.structureType == STRUCTURE_TOWER || it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
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
            .map { it.unsafeCast<StructureContainer>()}
            .filter { it.store.getUsedCapacity() > 50}
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