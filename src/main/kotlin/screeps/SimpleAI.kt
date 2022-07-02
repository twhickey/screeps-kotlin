package screeps

import screeps.flags.handleFlags
import screeps.api.*
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureSpawn
import screeps.creeps.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import kotlin.math.min

fun gameLoop() {
    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return

    //delete memories of creeps that have passed away
    houseKeeping(Game.creeps)

    handleFlags(mainSpawn)

    runTowers(Game.creeps.values, mainSpawn)

    val neededStates = planCreeps(Game.creeps.values, mainSpawn)

    //make sure we have at least some creeps
    spawnCreeps(Game.creeps.values, mainSpawn, neededStates)

    for ((_, creep) in Game.creeps) {
        val creepStateImpl = creep.memory.state.unsafeCast<CreepState>().impl
        val nextState = creepStateImpl.update(creep)
        creep.memory.state = nextState
        creepStateImpl.plan(creep)
        creepStateImpl.execute(creep)
    }
}

private fun planCreeps(creeps: Array<Creep>, spawn: StructureSpawn): MutableMap<CreepState, Int> {

    creeps.filter { it.memory.state == CreepState.UNKNOWN || it.memory.state == CreepState.BUSY }
        .forEach { it.memory.state = CreepState.IDLE }

    val availableCreeps = creeps.filter { it.memory.state == CreepState.IDLE }
    if (Game.time % 20 == 0) console.log("Idle Creeps: ${availableCreeps.count()}")

    val availableEnergyFromContainers = spawn.room.find(FIND_STRUCTURES)
        .filter { it.structureType == STRUCTURE_CONTAINER }
        .map { it.unsafeCast<StructureContainer>()}
        .sumOf { it.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 }

    val availableEnergyFromResources = spawn.room.find(FIND_DROPPED_RESOURCES)
        .filter { it.resourceType == RESOURCE_ENERGY }
        .sumOf {it.amount }

    val availableEnergy = availableEnergyFromResources + availableEnergyFromContainers

    val currentCarry = creeps
        .filter {it.memory.state == CreepState.GETTING_ENERGY }
        .sumOf { it.store.getCapacity() ?: 0}

    val neededHarvesters = when (val energy = availableEnergy - currentCarry) {
        in (Int.MIN_VALUE .. 100) -> 0
        in (100 .. 1000) -> 1
        in (1000 .. 2000) -> 2
        in (2000 .. 5000) -> 4
        else -> min(15, (energy / 1200))
    }

    val neededUpgraders = when (spawn.room.controller?.ticksToDowngrade ?: 40000) {
        in (0 .. 10000) -> 4
        in (10000 .. 20000) -> 3
        in (20000 .. 30000) -> 2
        else -> 0
    }

    val neededGuardians = spawn.room.find(FIND_HOSTILE_CREEPS).count()

    val minersSupported = spawn.room.find(FIND_STRUCTURES).count { it.structureType == STRUCTURE_CONTAINER }

    val cSites = spawn.room.find(FIND_CONSTRUCTION_SITES).count()
    val supportedBuilders = when(cSites) {
        0 -> 0
        in (1..4) -> 1
        else -> (cSites / 5) + 1
    }

    val neededStates: MutableMap<CreepState, Int> = mutableMapOf(
        CreepState.BUILDING to supportedBuilders,
        CreepState.TRANSFERRING_ENERGY to neededHarvesters,
        CreepState.UPGRADING to neededUpgraders,
        CreepState.GUARDING to neededGuardians,
        CreepState.MINE to minersSupported,
        CreepState.REPAIR to 2,
        CreepState.REPAIR_WALLS to 2
    )

    for (ac in availableCreeps) {
        val validStates = ac.memory.minionType.unsafeCast<MinionType>().validStates
        val newState = neededStates.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .firstOrNull { validStates.contains(it)} ?: CreepState.TRANSFERRING_ENERGY
        if (newState.requiresGetEnergy) {
            ac.memory.state = CreepState.GETTING_ENERGY
            ac.memory.nextState = newState
        } else {
            ac.memory.state = newState
        }
        neededStates[newState] = neededStates[newState]!! - 1
    }
    return neededStates
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

private fun spawnCreep(spawn: StructureSpawn, mt: MinionType, body: Array<BodyPartConstant>, state: CreepState, nextState: CreepState) {

    val newName = "${mt.name}_${Game.time}"
    val code = spawn.spawnCreep(body, newName, options {
        memory = jsObject<CreepMemory> { this.state = state; this.nextState = nextState; this.minionType = mt }
    })

    when (code) {
        OK -> console.log("Spawned $newName; type $mt; state $state; nextState: $nextState; body ${body.toList()}")
        ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
        else -> console.log("unhandled error code $code for body $body and name $newName")
    }
}

private fun spawnCreeps(creeps: Array<Creep>, spawn: StructureSpawn, neededStates: MutableMap<CreepState, Int>) {

    spawn.room.memory.debugMessages = false;
    if ((Game.time % 20) != 0) {
        return
    }

    val newState = neededStates.entries.filter { it.value > 0 }.maxByOrNull { it.value } ?: return

    for (mt in MinionType.values()) {
        if (mt.validStates.contains(newState.key)) {
            val body = getCreepParts(mt, spawn.room.energyAvailable)
            if (body != null && body.isNotEmpty()) {
                val newStates = if (newState.key.requiresGetEnergy) {
                    Pair(CreepState.GETTING_ENERGY, newState.key)
                } else {
                    Pair(newState.key, CreepState.IDLE)
                }
                spawnCreep(spawn, mt, body, newStates.first, newStates.second)
                break
            }
        }
    }
    var creepsByState = creeps.groupBy { Pair(it.memory.state, it.memory.nextState) }.map { Pair(it.key, it.value.count()) }
    console.log("Current States: $creepsByState")
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
