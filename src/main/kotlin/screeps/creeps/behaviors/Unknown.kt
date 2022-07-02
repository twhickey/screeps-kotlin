package screeps.creeps.behaviors

import screeps.api.Creep
import screeps.creeps.CreepState

object Unknown: Behavior() {
    override fun update(creep: Creep): CreepState {
        return CreepState.UNKNOWN
    }

    override fun plan(creep: Creep) {
    }

    override fun execute(creep: Creep) {
    }
}