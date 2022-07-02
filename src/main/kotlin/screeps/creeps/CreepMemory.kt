package screeps.creeps

import screeps.TargetType
import screeps.api.CreepMemory
import screeps.api.STRUCTURE_EXTENSION
import screeps.api.StructureConstant
import screeps.utils.memory.memory

var CreepMemory.state by memory(CreepState.UNKNOWN)
var CreepMemory.nextState by memory(CreepState.UNKNOWN)
var CreepMemory.targetId: String? by memory()

var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.minionType by memory(MinionType.UNASSIGNED)
var CreepMemory.targetStructureType: StructureConstant by memory { STRUCTURE_EXTENSION}
var CreepMemory.targetType by memory { TargetType.NONE }