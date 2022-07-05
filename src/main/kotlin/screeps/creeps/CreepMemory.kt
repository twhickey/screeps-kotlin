package screeps.creeps

import screeps.TargetType
import screeps.api.CreepMemory
import screeps.api.STRUCTURE_EXTENSION
import screeps.api.StructureConstant
import screeps.utils.memory.memory

var CreepMemory.state by memory(CreepState.UNKNOWN)
var CreepMemory.nextState by memory(CreepState.UNKNOWN)
var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.minionType by memory(MinionType.UNASSIGNED)
var CreepMemory.homeRoom: String? by memory()

// Targeting
var CreepMemory.targetType by memory(TargetType.NONE)
var CreepMemory.targetStructureType: StructureConstant by memory { STRUCTURE_EXTENSION }
var CreepMemory.targetId: String? by memory()
var CreepMemory.targetX: Int by memory { 0 }
var CreepMemory.targetY: Int by memory { 0 }
var CreepMemory.targetRoom: String? by memory()