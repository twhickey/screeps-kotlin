package screeps.creeps.behaviors

import screeps.api.Creep
import screeps.api.FIND_MY_SPAWNS
import screeps.creeps.CreepState

object Idle: Behavior() {
    override fun update(creep: Creep): CreepState {
        return CreepState.IDLE
    }

    override fun plan(creep: Creep) {
    }

    override fun execute(creep: Creep) {
        val spawn = creep.room.find(FIND_MY_SPAWNS).first()
        if (spawn != null) {
            creep.moveTo(spawn)
        }
    }
}