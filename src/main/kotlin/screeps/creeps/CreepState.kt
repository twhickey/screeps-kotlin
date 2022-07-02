package screeps.creeps

import screeps.creeps.behaviors.*

enum class CreepState(val impl: Behavior, val requiresGetEnergy: Boolean) {
    UNKNOWN(Unknown, false),
    IDLE(Idle, false),
    BUSY(Busy, false),
    GETTING_ENERGY(Refill, true),
    MINE(Mine, false),
    TRANSFERRING_ENERGY(Transfer, true),
    BUILDING(Build, true),
    UPGRADING(Upgrade, true),
    REPAIR(Repair, true),
    REPAIR_WALLS(RepairWalls, true),
    GUARDING(Guard, false);

    companion object {
        fun getState(fromMemory: Enum<CreepState>): CreepState {
            return values().first { it == fromMemory }
        }
    }
}