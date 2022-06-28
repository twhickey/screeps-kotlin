package screeps.creeps

import screeps.creeps.behaviors.*

enum class CreepState(val impl: Behavior) {
    UNKNOWN(Unknown),
    IDLE(Idle),
    BUSY(Busy),
    GETTING_ENERGY(Refill),
    MINE(Mine),
    TRANSFERRING_ENERGY(Transfer),
    BUILDING(Build),
    UPGRADING(Upgrade),
    REPAIR(Repair),
    REPAIR_WALLS(RepairWalls),
    GUARDING(Guard)
}