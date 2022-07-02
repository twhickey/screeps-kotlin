package screeps.creeps.behaviors

import screeps.api.Creep
import screeps.creeps.CreepState

object Busy: Behavior() {
    override fun update(creep: Creep): CreepState {
        return CreepState.BUSY
    }

    override fun plan(creep: Creep) {
    }

    override fun execute(creep: Creep) {
    }
}