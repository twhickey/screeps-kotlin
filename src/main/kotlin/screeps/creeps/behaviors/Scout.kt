package screeps.creeps.behaviors

import screeps.*
import screeps.api.*
import screeps.creeps.*

object Scout: Behavior() {
    override fun update(creep: Creep): CreepState {
        return CreepState.SCOUT
    }

    override fun plan(creep: Creep) {
        if (creep.memory.targetType != TargetType.NONE && creep.memory.targetId != null) { return }
        if (creep.memory.homeRoom == creep.room.name) {
            val exits = creep.room.find(FIND_EXIT)
            if (exits.isEmpty()) {
                creep.sayMessage("Found no exits from room ${creep.room}")
                creep.resetTarget()
            } else {
                creep.memory.targetId = exits[0].toString()
                creep.memory.targetX = exits[0].x
                creep.memory.targetY = exits[0].y
                creep.memory.targetRoom = exits[0].roomName
                creep.memory.targetType = TargetType.EXIT
            }
        } else {
            creep.resetTarget()
            val homeRoom = Game.rooms.values.find { it.name == creep.memory.homeRoom }
            if (homeRoom == null) {
                creep.sayMessage("Can't find home room")
            } else {
                val exit = creep.room.findExitTo(homeRoom)
                console.log("Exit to home room = $exit")
            }
        }
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetType == TargetType.EXIT && creep.memory.targetRoom != null) {
            creep.moveToTarget(RoomPosition(25, 25, creep.memory.targetRoom!!), opts = options { range = 24})
        }
    }
}