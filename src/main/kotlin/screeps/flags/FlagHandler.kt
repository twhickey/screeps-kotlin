package screeps.flags

import screeps.createConstructionSite
import screeps.api.*
import screeps.api.structures.StructureSpawn

fun handleFlags(mainSpawn: StructureSpawn) {
    if ((Game.time % 50) != 0) return

    val flags = mainSpawn.room.find(FIND_FLAGS)
    for (flag in flags) {
        val ft = getFlagType(flag)
        val removeFlag: Boolean = when(ft) {
            FlagTypes.NONE -> false
            FlagTypes.CONTAINER -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_CONTAINER)
            FlagTypes.EXTENSION -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_EXTENSION)
            FlagTypes.TOWER -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_TOWER)
            FlagTypes.ROAD -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_ROAD)
            FlagTypes.RAMPART -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_RAMPART)
            FlagTypes.WALL -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_WALL)
            FlagTypes.DESTROY -> {
                val structuresAtFlag = mainSpawn.room.getPositionAt(flag.pos.x, flag.pos.y)!!.lookFor(LOOK_STRUCTURES)
                var result = OK
                for (s in structuresAtFlag!!) {
                    result = s.destroy()
                    if (result != OK) {
                        console.log("Result of destroying ${s.structureType} at ${flag.pos} is $result")
                        break
                    }
                }
                result == OK
            }
        }
        if (removeFlag) {
            flag.remove()
        }
    }
}