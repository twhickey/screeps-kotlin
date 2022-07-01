package screeps

import screeps.flags.handleFlags
import screeps.api.*
import screeps.api.structures.StructureContainer
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

    runTowers(Game.creeps.values, mainSpawn)

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

private fun runTowers(creeps: Array<Creep>, spawn: StructureSpawn) {
    for (tower in Context.towers) {
        val attackers = spawn.room.find(FIND_HOSTILE_CREEPS)
        if (attackers.isNotEmpty()) {
                val attackResult = tower.attack(attackers[0])
            if (attackResult != OK) {
                console.log("Tower $tower failed to attack ${attackers[0]} due to $attackResult")
            }
        } else if(tower.store.getUsedCapacity(RESOURCE_ENERGY) > spawn.room.memory.towerRepairThreshold) {
            val damagedCreeps = creeps
                .filter { it.hits < it.hitsMax }
                .map { Pair(it, (it.hitsMax - it.hits)) }
                .sortedByDescending { it.second }
                .map { it.first }

            if (damagedCreeps.isNotEmpty()) {
                val healResult = tower.heal(damagedCreeps[0])
                if (healResult != OK) {
                    console.log("Tower $tower failed to heal creep ${damagedCreeps[0]} due to $healResult")
                }
            } else if (tower.store.getUsedCapacity(RESOURCE_ENERGY) > spawn.room.memory.towerBuildThreshold){
                val damagedStructures = spawn.room.find(FIND_STRUCTURES)
                    .filter { it.hits < it.hitsMax }
                    .map { Pair(it, (it.hitsMax - it.hits)) }
                    .sortedByDescending { it.second }
                    .map { it.first }

                if (damagedStructures.isNotEmpty()) {
                    val repairResult = tower.repair(damagedStructures[0])
                    if (repairResult != OK) {
                        console.log("Tower $tower failed to repair structure ${damagedStructures[0]} due to $repairResult")
                    }
                }
            }
        }
    }
}

private fun spawnCreeps(creeps: Array<Creep>, spawn: StructureSpawn) {

    spawn.room.memory.debugMessages = false;
    if ((Game.time % 20) != 0) {
        return
    }

    val availableEnergyFromContainerss = Context.myStuctures
        .map { it.value }
        .filter { it.structureType == STRUCTURE_CONTAINER }
        .map { it.unsafeCast<StructureContainer>()}
        .sumOf { it.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 }

    val availableEnergyFromResources = spawn.room.find(FIND_DROPPED_RESOURCES)
        .filter { it.resourceType == RESOURCE_ENERGY }
        .sumOf {it.amount }

    val availableEnergy = availableEnergyFromResources + availableEnergyFromContainerss

    val availableCarry = creeps
        .filter {it.memory.role == Role.HARVESTER }
        .sumOf { it.store.getCapacity() ?: 0}

    val neededUpgraders = when (spawn.room.controller?.ticksToDowngrade ?: 40000) {
        in (0 .. 10000) -> 4
        in (10000 .. 20000) -> 3
        in (20000 .. 30000) -> 2
        else -> 0
    }

    val neededHarvester = if (availableCarry < availableEnergy) { 1 } else { 0 }

    val neededGuardians = spawn.room.find(FIND_HOSTILE_CREEPS).count()

    val minersSupported = spawn.room.find(FIND_STRUCTURES).count { it.structureType == STRUCTURE_CONTAINER }

    val cSites = spawn.room.find(FIND_CONSTRUCTION_SITES).count()
    val supportedBuilders = when(cSites) {
        0 -> 0
        in (1..4) -> 1
        else -> (cSites / 5) + 1
    }

    val neededRoles: Map<Role, Int> = mapOf(
        Role.BUILDER to max(0, supportedBuilders - creeps.count {it.memory.role == Role.BUILDER}),
        Role.HARVESTER to neededHarvester,
        Role.UPGRADER to max(0, neededUpgraders - creeps.count {it.memory.role == Role.UPGRADER}),
        Role.GUARDIAN to max(0, neededGuardians - creeps.count {it.memory.role == Role.GUARDIAN}),
        Role.MINER to max(0, minersSupported - creeps.count() {it.memory.role == Role.MINER}),
        Role.REPAIRER to max(0, 2 - creeps.count() {it.memory.role == Role.REPAIRER}),
        Role.DEFENSE_BUILDER to max(0, 2 - creeps.count() {it.memory.role == Role.DEFENSE_BUILDER})
    )

    var neededTypes: MutableSet<MinionType> = HashSet()
    for (mt in MinionType.values()) {
        val supportedRoles = mt.validRoles
        for (vr in supportedRoles) {
            if (neededRoles.containsKey(vr) && neededRoles[vr] > 0) {
                neededTypes.add(mt)
            }
        }
    }

    var minionType: MinionType = MinionType.GENERIC
    var role: Role = Role.HARVESTER
    var maxNeeded = 0
    var buildBody: Array<BodyPartConstant>? = null

    for (mt in neededTypes) {
        val body = getCreepParts(mt, spawn.room.energyAvailable)
        if (body == null || body.isEmpty()) continue
        for (vr in mt.validRoles) {
            if (neededRoles.getOrElse(vr) {0} > maxNeeded) {
                role = vr
                maxNeeded = neededRoles[vr]!!
                minionType = mt
                buildBody = body
                break;
            }
        }
    }

    console.log("Spawn selections - minionType: $minionType; body: $buildBody; neededRoles: $neededRoles; role: $role")


    if (buildBody != null && buildBody.isNotEmpty()) {
        val newName = "${role.name}_${Game.time}"
        val code = spawn.spawnCreep(buildBody, newName, options {
            memory = jsObject<CreepMemory> { this.role = role; this.minionType = minionType }
        })

        when (code) {
            OK -> console.log("Spawned $newName; type $minionType; role $role; body ${buildBody.toList()}")
            ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
            else -> console.log("unhandled error code $code for body $buildBody and name $newName")
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
