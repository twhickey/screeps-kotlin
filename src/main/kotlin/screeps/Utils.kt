package screeps

import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.creeps.BodyPartSpec
import screeps.creeps.MinionType
import kotlin.math.floor
import kotlin.math.min

fun getCreepParts(type: MinionType, availableEnergy: Int): Array<BodyPartConstant> {
    var level = 1
    var cost = 0
    var prevLevel = 0
    var prevCost = 0
    while (cost < availableEnergy) {
        prevLevel = level
        prevCost = cost
        cost = calculateCost(type.bodyPartPriorities, level)
        // console.log("MinionTyhpe: $type, Level: $level, Cost: $cost, Available Energy: $availableEnergy")
        if (cost == prevCost) { break }
        level++
    }

    level = prevLevel - 1
    return if (level > 0) {
        val bodyPartCounts = type.bodyPartPriorities
            .map { Pair(it.key, calculateCount(level, it.value)) }
            .toMap()
        val result = bodyPartCounts.map { (bp, c) -> Array(c) {bp}}.toTypedArray().flatten().toTypedArray()
        // console.log("Returning MinionType: $type, Level: $level, body: $result")
        result
    } else {
        emptyArray()
    }
}

fun calculateCost(bodyPartPriorities: Map<BodyPartConstant, BodyPartSpec>, level: Int): Int {
    val bodyPartCounts = bodyPartPriorities
        .map { Pair(it.key, calculateCount(level, it.value)) }
        .toMap()
    val cost = bodyPartCounts.map { (bp, c) -> c * BODYPART_COST[bp]!! }.sum()
    return cost
}

fun calculateCount(level: Int, spec: BodyPartSpec): Int {
    return min(spec.max, floor(spec.min + (level - 1) * spec.factor).toInt())
}

fun createConstructionSite(mainSpawn: StructureSpawn, pos: RoomPosition, type: StructureConstant): Boolean {
    val result = mainSpawn.room.createConstructionSite(pos, type)
    if (result != OK) {
        console.log("Failed to create $type at $pos with error $result")
    }
    return (result == OK)
}

fun Creep.sayMessage(msg: String) {
    console.log("Creep ${this.name}: $msg")
}