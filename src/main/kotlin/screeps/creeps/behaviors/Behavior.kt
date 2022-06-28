package screeps.creeps.behaviors

import screeps.api.Creep
import screeps.creeps.CreepState

abstract class Behavior {
    abstract fun update(creep: Creep): CreepState
    abstract fun plan(creep: Creep)
    abstract fun execute(creep: Creep)
}