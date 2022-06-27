package screeps.rooms

import screeps.api.FIND_STRUCTURES
import screeps.api.Room
import screeps.api.structures.Structure
import screeps.utils.lazyPerTick

val Room.getAllStructures: Map<String, Structure> by lazyPerTick { this.find(FIND_STRUCTURES).map { Pair(it.id, it) }.toMap() }
