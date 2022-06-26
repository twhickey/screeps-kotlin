import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.creeps.MinionType

fun getCreepParts(type: MinionType, availableEnergy: Int): Array<BodyPartConstant> {
    val baseCost = calculateCost(type.bodyPartPriorities)
    val reps = availableEnergy / baseCost
    if (reps > 0) {
        val bodyPartCounts = type.bodyPartPriorities.map { (bp, c) -> Pair(bp, c * reps) }.toMap()
        return bodyPartCounts.map { (bp, c) -> Array(c) {bp}}.toTypedArray().flatten().toTypedArray()
    }
    return emptyArray()
}

fun calculateCost(bodyPartPriorities: Map<BodyPartConstant, Int>): Int {
    return bodyPartPriorities.map { (bp, c) -> c * BODYPART_COST[bp]!! }.sum()
}

fun createConstructionSite(mainSpawn: StructureSpawn, pos: RoomPosition, type: StructureConstant): Boolean {
    val result = mainSpawn.room.createConstructionSite(pos, type)
    if (result != OK) {
        console.log("Failed to create $type at $pos with error $result")
    }
    return (result == OK)
}