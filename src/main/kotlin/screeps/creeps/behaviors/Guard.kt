package screeps.creeps.behaviors

import screeps.Context
import screeps.api.*
import screeps.creeps.CreepState
import screeps.creeps.state
import screeps.creeps.targetId
import screeps.sayMessage

object Guard : Behavior() {
    override fun update(creep: Creep): CreepState {
        return CreepState.GUARDING
    }

    override fun plan(creep: Creep) {
        val targets = creep.room.find(FIND_HOSTILE_CREEPS).filter { it.hits > 0 }
        if (targets.isEmpty()) {
            creep.memory.targetId = null
        }

        if ((creep.memory.targetId != null) && (targets.any {it.id == creep.memory.targetId})) {
            return
        }

        val alreadyEngagedTargets = Context.targets
            .filter { it.value.memory.state == CreepState.GUARDING }
            .filter { it.value.id != creep.id }
            .map { Game.getObjectById<Creep>(it.key) }

        val target = alreadyEngagedTargets.first() ?: targets.first()
        creep.memory.targetId = target.id
    }

    override fun execute(creep: Creep) {
        if (creep.memory.targetId == null) {
            creep.moveTo(Game.spawns.values.first())
        } else {
            val target = Game.getObjectById<Creep>(creep.memory.targetId)
            if (target != null) {
                val attackResult = creep.attack(target)
                if (attackResult == ERR_NOT_IN_RANGE) {
                    creep.moveTo(target)
                } else if (attackResult != OK) {
                    creep.sayMessage("Failed to attack target $target at ${target.pos} due to $attackResult")
                }
            }
        }
    }
}
