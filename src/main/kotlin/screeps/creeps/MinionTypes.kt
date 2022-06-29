package screeps.creeps

import screeps.api.*
import screeps.creeps.roles.Role

class BodyPartSpec(val min: Int, val max: Int, val factor: Double)

enum class MinionType(val bodyPartPriorities: Map<BodyPartConstant, BodyPartSpec>, val validRoles: List<Role>) {
    UNASSIGNED(emptyMap(), emptyList()),
    GENERIC(mapOf(
        WORK to BodyPartSpec(1, 10, 1.0),
        CARRY to BodyPartSpec(1, 10, 1.0),
        MOVE to BodyPartSpec(1, 5, 0.3333333)
    ), listOf(Role.HARVESTER, Role.BUILDER, Role.UPGRADER, Role.REPAIRER, Role.DEFENSE_BUILDER)),
    HAULER(mapOf(
        WORK to BodyPartSpec(1, 5, 0.5),
        CARRY to BodyPartSpec(1, 10, 1.0),
        MOVE to BodyPartSpec(1, 5, 1.0)
    ), listOf(Role.HARVESTER, Role.BUILDER, Role.UPGRADER)),
    MINER(mapOf(
        WORK to BodyPartSpec(1, 5, 1.0),
        MOVE to BodyPartSpec(1, 5, 1.0)
    ), listOf(Role.MINER)),
    GUARDIAN(mapOf(
        TOUGH to BodyPartSpec(20, 40, 5.0),
        RANGED_ATTACK to BodyPartSpec(3, 6, 1.0),
        MOVE to BodyPartSpec(2, 4, 0.4)
    ), listOf(Role.GUARDIAN))
}