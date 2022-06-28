package screeps.creeps
import screeps.api.CreepMemory
import screeps.api.STRUCTURE_EXTENSION
import screeps.api.StructureConstant
import screeps.creeps.roles.Role
import screeps.utils.memory.memory

var CreepMemory.state by  memory(CreepState.UNKNOWN)
var CreepMemory.nextState by memory(CreepState.UNKNOWN)
var CreepMemory.targetId: String? by memory()

var CreepMemory.building: Boolean by memory { false }
var CreepMemory.upgrading: Boolean by memory { false }
var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.minionType by memory(MinionType.UNASSIGNED)
var CreepMemory.targetType: StructureConstant by memory { STRUCTURE_EXTENSION}