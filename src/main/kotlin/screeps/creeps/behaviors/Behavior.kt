package screeps.creeps.behaviors

import screeps.api.Creep

abstract class Behavior {
    abstract fun update(creep: Creep)
    abstract fun execute(creep: Creep)
}