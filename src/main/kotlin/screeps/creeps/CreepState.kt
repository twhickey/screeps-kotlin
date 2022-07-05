package screeps.creeps

import screeps.creeps.behaviors.*

enum class CreepState(val impl: Behavior, val refillState: CreepState) {
    UNKNOWN(Unknown, UNKNOWN),
    IDLE(Idle, UNKNOWN),
    BUSY(Busy, UNKNOWN),
    GETTING_ENERGY(RefillForTransfer, UNKNOWN),
    FILLING_FOR_HAUL(RefillForHaul, UNKNOWN),
    HAUL(Haul, FILLING_FOR_HAUL),
    MINE(Mine, UNKNOWN),
    TRANSFERRING_ENERGY(Transfer, GETTING_ENERGY),
    BUILDING(Build, GETTING_ENERGY),
    UPGRADING(Upgrade, GETTING_ENERGY),
    REPAIR(Repair, GETTING_ENERGY),
    REPAIR_WALLS(RepairWalls, GETTING_ENERGY),
    GUARDING(Guard, UNKNOWN),
    SCOUT(Scout, UNKNOWN);

    companion object {
        fun getState(fromMemory: Enum<CreepState>): CreepState {
            return values().first { it == fromMemory }
        }
    }
}