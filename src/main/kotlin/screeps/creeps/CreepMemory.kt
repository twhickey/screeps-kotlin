package screeps.creeps
import screeps.api.CreepMemory
import screeps.creeps.roles.Role
import screeps.utils.memory.memory
import starter.MinionType

var CreepMemory.state by memory(CreepState.UNKNOWN)
var CreepMemory.targetId: String? by memory()
var CreepMemory.assignedEnergySource: String? by memory()
var CreepMemory.missionId: String? by memory()

var CreepMemory.building: Boolean by memory { false }
var CreepMemory.upgrading: Boolean by memory { false }
var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.minionType by memory(MinionType.UNASSIGNED)

enum class CreepState {
    UNKNOWN, IDLE, BUSY, REFILL, TRANSFERRING_ENERGY, CONSTRUCTING, UPGRADING, REPAIR, CLAIM, MISSION
}