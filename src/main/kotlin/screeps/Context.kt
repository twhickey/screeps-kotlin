package screeps

import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureTower
import screeps.creeps.targetId
import screeps.rooms.getAllStructures
import screeps.utils.lazyPerTick
import screeps.utils.toMap

object Context {
    //built-in
    val creeps: Map<String, Creep> by lazyPerTick { Game.creeps.toMap() }
    val rooms: Map<String, Room> = Game.rooms.toMap()
    val myStuctures: Map<String, Structure> by lazyPerTick { Game.structures.toMap() }
    val constructionSites: Map<String, ConstructionSite> by lazyPerTick { Game.constructionSites.toMap() }

    //synthesized
    val targets: Map<String, Creep> by lazyPerTick { creepsByTarget() }
    val towers: List<StructureTower> by lazyPerTick { myStuctures.values.filter { it.structureType == STRUCTURE_TOWER } as List<StructureTower> }

    private fun creepsByTarget(): Map<String, Creep> {
        return creeps.filter { it.value.memory.targetId != null }
            .mapKeys { (_, creep) -> creep.memory.targetId!! }
    }
}

fun <T> Iterable<Structure>.filterIsStrucure(structureType: StructureConstant): List<Structure> {
    return this.filter { it.structureType == structureType }
}