package screeps.creeps

import screeps.api.*
import screeps.creeps.roles.Role

enum class MinionType(val bodyPartPriorities: Map<BodyPartConstant, Int>, val validRoles: List<Role>) {
    UNASSIGNED(emptyMap(), emptyList()),
    GENERIC(mapOf(
        WORK to 1,
        CARRY to 1,
        MOVE to 1
    ), listOf(Role.HARVESTER, Role.BUILDER, Role.UPGRADER, Role.REPAIRER, Role.DEFENSE_BUILDER)),
    HAULER(mapOf(
        WORK to 1,
        CARRY to 1,
        MOVE to 2
    ), listOf(Role.HARVESTER, Role.BUILDER, Role.UPGRADER)),
    MINER(mapOf(
        WORK to 4,
        MOVE to 1
    ), listOf(Role.MINER)),
    GUARDIAN(mapOf(
        ATTACK to 2,
        MOVE to 1
    ), listOf(Role.GUARDIAN))
}