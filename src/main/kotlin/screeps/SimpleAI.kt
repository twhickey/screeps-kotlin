package screeps

import screeps.flags.handleFlags
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.creeps.MinionType
import screeps.creeps.minionType
import screeps.creeps.role
import screeps.creeps.roles.Role
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import kotlin.math.max

fun gameLoop() {
    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return

    //delete memories of creeps that have passed away
    houseKeeping(Game.creeps)

    handleFlags(mainSpawn)

    //make sure we have at least some creeps
    spawnCreeps(Game.creeps.values, mainSpawn)

    for ((_, creep) in Game.creeps) {
        when (creep.memory.role) {
            Role.HARVESTER -> creep.harvest()
            Role.BUILDER -> creep.build()
            Role.UPGRADER -> creep.upgrade(mainSpawn.room.controller!!)
            Role.GUARDIAN -> creep.guard(mainSpawn.room.controller!!)
            Role.MINER -> creep.mine(mainSpawn.room.controller!!)
            Role.REPAIRER -> creep.repair()
            Role.DEFENSE_BUILDER -> creep.repair()
            else -> creep.pause()
        }
    }
}

private fun spawnCreeps(
        creeps: Array<Creep>,
        spawn: StructureSpawn
) {
    spawn.room.memory.debugMessages = false;
    if ((Game.time % 20) != 0) {
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
        creeps.count() {it.memory.minionType == MinionType.GENERIC } < 20 -> MinionType.GENERIC
        //creeps.count() {it.memory.minionType == MinionType.HAULER} < 2 -> MinionType.HAULER
        else -> MinionType.UNASSIGNED
    }

    val body = getCreepParts(minionType, spawn.room.energyAvailable)

    val neededRoles: Map<Role, Int> = mapOf(
        Role.BUILDER to max(0, 3 - creeps.count {it.memory.role == Role.BUILDER}),
        Role.HARVESTER to max(0, 3 - creeps.count {it.memory.role == Role.HARVESTER}),
        Role.UPGRADER to max(0, 2 - creeps.count {it.memory.role == Role.UPGRADER}),
        Role.GUARDIAN to max(0, neededGuardians - creeps.count {it.memory.role == Role.GUARDIAN}),
        Role.MINER to max(0, minersSupported - creeps.count() {it.memory.role == Role.MINER}),
        Role.REPAIRER to max(0, 2 - creeps.count() {it.memory.role == Role.REPAIRER}),
        Role.DEFENSE_BUILDER to max(0, 3 - creeps.count() {it.memory.role == Role.DEFENSE_BUILDER})
    )

    var role: Role = Role.HARVESTER
    var maxNeeded = 0
    for (vr in minionType.validRoles) {
        if (neededRoles.getOrElse(vr) {0} > maxNeeded) {
            role = vr
            maxNeeded = neededRoles[vr]!!
        }
    }
    console.log("Spawn selections - minionType: $minionType; body: $body; neededRoles: $neededRoles; role: $role")
    if (spawn.room.memory.debugMessages) {
        console.log("Spawn selections - minionType: $minionType; body: $body; neededRoles: $neededRoles; role: $role")
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
