package starter

import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import kotlin.math.max

enum class FlagTypes(val prefix: String) {
    NONE("None"),
    CONTAINER("Container"),
    EXTENSION("Extension"),
    DESTROY("Destroy"),
    ROAD("Road"),
    TOWER("Tower")
}

fun gameLoop() {
    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return

    //delete memories of creeps that have passed away
    houseKeeping(Game.creeps)

    handleFlags(mainSpawn)

    // just an example of how to use room memory
    mainSpawn.room.memory.numberOfCreeps = mainSpawn.room.find(FIND_CREEPS).count()

    //make sure we have at least some creeps
    spawnCreeps(Game.creeps.values, mainSpawn)

    for ((_, creep) in Game.creeps) {
        when (creep.memory.role) {
            Role.HARVESTER -> creep.harvest()
            Role.BUILDER -> creep.build()
            Role.UPGRADER -> creep.upgrade(mainSpawn.room.controller!!)
            Role.GUARDIAN -> creep.guard(mainSpawn.room.controller!!)
            Role.MINER -> creep.mine(mainSpawn.room.controller!!)
            else -> creep.pause()
        }
    }
}

private fun handleFlags(mainSpawn: StructureSpawn) {
    if ((Game.time % 50) != 0) {
        return
    }

    val flags = mainSpawn.room.find(FIND_FLAGS)
    for (flag in flags) {
        val ft = getFlagType(flag)
        var removeFlag: Boolean = when(ft) {
            FlagTypes.NONE -> false
            FlagTypes.CONTAINER -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_CONTAINER)
            FlagTypes.EXTENSION -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_EXTENSION)
            FlagTypes.TOWER -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_TOWER)
            FlagTypes.ROAD -> createConstructionSite(mainSpawn, flag.pos, STRUCTURE_ROAD)
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

private fun createConstructionSite(mainSpawn: StructureSpawn, pos: RoomPosition, type: StructureConstant): Boolean {
    val result = mainSpawn.room.createConstructionSite(pos, type)
    if (result != OK) {
        console.log("Failed to create $type at $pos with error $result")
    }
    return (result == OK)
}

private fun getFlagType(flag: Flag) : FlagTypes {
    for (ft in FlagTypes.values()) {
        if (flag.name.startsWith(ft.prefix, true)) {
            return ft
        }
    }
    return FlagTypes.NONE
}

private fun spawnCreeps(
        creeps: Array<Creep>,
        spawn: StructureSpawn
) {
    if ((Game.time % 50) != 25) {
        return
    }

    val neededGuardians = if (spawn.room.find(FIND_HOSTILE_CREEPS).isNotEmpty()) {
        2
    } else {
        0
    }

    val minersSupported = spawn.room.find(FIND_STRUCTURES).count { it.structureType == STRUCTURE_CONTAINER }

    val minionType: MinionType = when {
        creeps.count() {it.memory.minionType == MinionType.GUARDIAN} < neededGuardians -> MinionType.GUARDIAN
        creeps.count() {it.memory.minionType == MinionType.MINER } < minersSupported -> MinionType.MINER
        creeps.count() {it.memory.minionType == MinionType.GENERIC } < 10 -> MinionType.GENERIC
        //creeps.count() {it.memory.minionType == MinionType.HAULER} < 2 -> MinionType.HAULER
        else -> MinionType.UNASSIGNED
    }

    val body = getCreepParts(minionType, spawn.room.energyAvailable)

    val neededRoles: Map<Role, Int> = mapOf(
        Role.BUILDER to max(0, 4 - creeps.count {it.memory.role == Role.BUILDER}),
        Role.HARVESTER to max(0, 3 - creeps.count {it.memory.role == Role.HARVESTER}),
        Role.UPGRADER to max(0, 4 - creeps.count {it.memory.role == Role.UPGRADER}),
        Role.GUARDIAN to max(0, neededGuardians - creeps.count {it.memory.role == Role.GUARDIAN}),
        Role.MINER to max(0, minersSupported - creeps.count() {it.memory.role == Role.MINER})
    )

    var role: Role = Role.HARVESTER
    for (vr in minionType.validRoles) {
        if (neededRoles.getOrElse(vr) {0} > 0) {
            role = vr
            break
        }
    }

    val spawnCost = body.sumOf { BODYPART_COST[it]!! }
    if (spawn.room.energyAvailable <  spawnCost) {
        console.log("Not spawning type $minionType; role $role; body $body due to only having ${spawn.room.energyAvailable} of $spawnCost energy")
        return
    }

    if (!body.isEmpty()) {
        val newName = "${role.name}_${Game.time}"
        val code = spawn.spawnCreep(body, newName, options {
            memory = jsObject<CreepMemory> { this.role = role; this.minionType = minionType }
        })

        when (code) {
            OK -> console.log("Spawned $newName; type $minionType; role $role; body $body")
            ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
            else -> console.log("unhandled error code $code for body $body and name $newName")
        }
    }
}

private fun houseKeeping(creeps: Record<String, Creep>) {
    if (Game.creeps.isEmpty()) return  // this is needed because Memory.creeps is undefined

    for ((creepName, _) in Memory.creeps) {
        if (creeps[creepName] == null) {
            console.log("deleting obsolete memory entry for creep $creepName")
            delete(Memory.creeps[creepName])
        }
    }
}

private fun getCreepParts(type: MinionType, availableEnergy: Int): Array<BodyPartConstant> {
    val baseCost = calculateCost(type.bodyPartPriorities)
    val reps = availableEnergy / baseCost
    if (reps > 0) {
        val bodyPartCounts = type.bodyPartPriorities.map { (bp, c) -> Pair(bp, c * reps) }.toMap()
        return bodyPartCounts.map { (bp, c) -> Array(c) {bp}}.toTypedArray().flatten().toTypedArray()
    }
    return emptyArray()
}

private fun calculateCost(bodyPartPriorities: Map<BodyPartConstant, Int>): Int {
    return bodyPartPriorities.map { (bp, c) -> c * BODYPART_COST[bp]!! }.sum()
}
