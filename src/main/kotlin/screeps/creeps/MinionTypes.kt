package screeps.creeps

import screeps.api.*

class BodyPartSpec(val min: Int, val max: Int, val factor: Double)

enum class MinionType(val bodyPartPriorities: Map<BodyPartConstant, BodyPartSpec>, val validStates: List<CreepState>) {
    UNASSIGNED(emptyMap(), emptyList()),
    GENERIC(mapOf(
        WORK to BodyPartSpec(1, 10, 1.0),
        CARRY to BodyPartSpec(1, 10, 1.0),
        MOVE to BodyPartSpec(1, 5, 0.3333333)
    ), listOf(CreepState.REPAIR, CreepState.REPAIR_WALLS, CreepState.BUILDING, CreepState.TRANSFERRING_ENERGY, CreepState.GETTING_ENERGY, CreepState.UPGRADING)),
    HAULER(mapOf(
        WORK to BodyPartSpec(1, 5, 0.5),
        CARRY to BodyPartSpec(1, 10, 1.0),
        MOVE to BodyPartSpec(1, 5, 1.0)
    ), listOf(CreepState.GETTING_ENERGY, CreepState.TRANSFERRING_ENERGY, CreepState.BUILDING, CreepState.UPGRADING)),
    MINER(mapOf(
        WORK to BodyPartSpec(1, 5, 1.0),
        MOVE to BodyPartSpec(1, 5, 1.0)
    ), listOf(CreepState.MINE)),
    GUARDIAN(mapOf(
        TOUGH to BodyPartSpec(20, 40, 5.0),
        RANGED_ATTACK to BodyPartSpec(3, 6, 1.0),
        MOVE to BodyPartSpec(2, 4, 0.4)
    ), listOf(CreepState.GUARDING))
}