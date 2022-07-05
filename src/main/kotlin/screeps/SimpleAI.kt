package screeps

import screeps.flags.handleFlags
import screeps.api.*
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureSpawn
import screeps.api.structures.StructureStorage
import screeps.creeps.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import kotlin.math.min

fun gameLoop() {
    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return

    //delete memories of creeps that have passed away
    houseKeeping(Game.creeps)

    roomReport(mainSpawn)

    handleFlags(mainSpawn)

    runTowers(Game.creeps.values, mainSpawn)

    val neededStates = planCreeps(Game.creeps.values, mainSpawn)

    //make sure we have at least some creeps
    spawnCreeps(Game.creeps.values, mainSpawn, neededStates)

    for ((_, creep) in Game.creeps) {
        var creepStateImpl = CreepState.getState(creep.memory.state).impl
        val nextState = creepStateImpl.update(creep)

        if (nextState != creep.memory.state) {
            // creep.sayMessage("Changed states from ${creep.memory.state} to $nextState")
            creep.memory.state = nextState
            creep.memory.nextState = CreepState.IDLE
            creep.resetTarget()
        }

        creepStateImpl = CreepState.getState(creep.memory.state).impl
        creepStateImpl.plan(creep)
        creepStateImpl.execute(creep)
    }
}

fun roomReport(mainSpawn: StructureSpawn) {
    if (Game.time % 20 != 10) return

    for (room in Context.rooms.entries) {
        console.log("${room.value}")
    }
}

private fun activeCreeps(creeps: List<Creep>, state: CreepState): Int {
    return creeps.count {
        (it.memory.state == state) ||
                (it.memory.state == CreepState.GETTING_ENERGY && it.memory.nextState == state) ||
                (it.memory.state == CreepState.FILLING_FOR_HAUL && it.memory.nextState == state) }
}

private fun planCreeps(creeps: Array<Creep>, spawn: StructureSpawn): MutableMap<CreepState, Int> {

    creeps.filter { it.memory.state == CreepState.UNKNOWN || it.memory.state == CreepState.BUSY }
        .forEach { it.memory.state = CreepState.IDLE }

    val partitioned = creeps.partition { it.memory.state == CreepState.IDLE }
    val availableCreeps = partitioned.first
    val activeCreeps = partitioned.second

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
        .filter { (it.memory.state == CreepState.GETTING_ENERGY && it.memory.nextState == CreepState.HAUL) || ( it.memory.state == CreepState.HAUL ) }
        .sumOf { it.store.getCapacity() ?: 0}

    val neededHarvesters = when (val energy = availableEnergy - currentCarry) {
        in (Int.MIN_VALUE .. 100) -> 0
        in (100 .. 1000) -> 1
        in (1000 .. 2000) -> 2
        in (2000 .. 5000) -> 4
        else -> min(15, (energy / 800))
    }

    val storedEnergy = spawn.room.find(FIND_MY_STRUCTURES)
        .filter { it.structureType == STRUCTURE_STORAGE }
        .map { it.unsafeCast<StructureStorage>() }
        .sumBy {it.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0}

    val additionalUpgraders = when (storedEnergy) {
        in (0 .. 10000) -> 0
        in (10000 .. 100000) -> 1
        in (10000 .. 250000) -> 2
        else -> (storedEnergy % 250000) + 2
    }

    val neededUpgraders = when (spawn.room.controller?.ticksToDowngrade ?: 40000) {
        in (0 .. 10000) -> 4
        in (10000 .. 20000) -> 3
        in (20000 .. 30000) -> 2
        else -> 0
    } + additionalUpgraders

    val neededScouts = 0 // if (spawn.room.controller?.level > 2 ) { 1 } else { 0 }

    val neededGuardians = spawn.room.find(FIND_HOSTILE_CREEPS).count()

    val minersSupported = spawn.room.find(FIND_STRUCTURES).count { it.structureType == STRUCTURE_CONTAINER }

    val cSites = spawn.room.find(FIND_CONSTRUCTION_SITES).count()
    val supportedBuilders = when(cSites) {
        0 -> 0
        in (1..4) -> 1
        else -> (cSites / 5) + 1
    }

    val neededStates: MutableMap<CreepState, Int> = mutableMapOf(
        CreepState.BUILDING to supportedBuilders - activeCreeps(activeCreeps, CreepState.BUILDING),
        CreepState.HAUL to neededHarvesters - activeCreeps(activeCreeps, CreepState.HAUL),
        CreepState.TRANSFERRING_ENERGY to 2 - activeCreeps(activeCreeps, CreepState.TRANSFERRING_ENERGY),
        CreepState.UPGRADING to neededUpgraders - activeCreeps(activeCreeps, CreepState.UPGRADING),
        CreepState.GUARDING to neededGuardians - activeCreeps(activeCreeps, CreepState.GUARDING),
        CreepState.MINE to minersSupported - activeCreeps(activeCreeps, CreepState.MINE),
        CreepState.REPAIR to  2 - activeCreeps(activeCreeps, CreepState.REPAIR),
        CreepState.REPAIR_WALLS to 2 - activeCreeps(activeCreeps, CreepState.REPAIR_WALLS),
        CreepState.SCOUT to neededScouts - activeCreeps(activeCreeps, CreepState.SCOUT)
    )

    for (ac in availableCreeps) {
        val validStates = ac.memory.minionType.unsafeCast<MinionType>().validStates
        val newState = neededStates.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .firstOrNull { validStates.contains(it)} ?: CreepState.TRANSFERRING_ENERGY

        if (newState.refillState != CreepState.UNKNOWN) {
            ac.memory.state = newState.refillState
            ac.memory.nextState = newState
        } else {
            ac.memory.state = newState
            ac.memory.nextState = CreepState.IDLE
        }
        console.log("Reassigned Idle Creep $ac to (${ac.memory.state}, ${ac.memory.nextState})")
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
        memory = jsObject<CreepMemory> { this.state = state; this.nextState = nextState; this.minionType = mt; this.homeRoom = spawn.room.name }
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

    console.log("Needed States: $newState")

    for (mt in MinionType.values()) {
        if (mt.validStates.contains(newState.key)) {
            val body = getCreepParts(mt, spawn.room.energyAvailable)
            if (body.isNotEmpty()) {
                val newStates = if (newState.key.refillState == CreepState.UNKNOWN) {
                    Pair(newState.key, CreepState.IDLE)
                } else {
                    Pair(newState.key.refillState, newState.key)
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
