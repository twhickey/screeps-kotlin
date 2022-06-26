package Flags

import screeps.api.Flag

enum class FlagTypes(val prefix: String) {
    NONE("None"),
    CONTAINER("Container"),
    EXTENSION("Extension"),
    DESTROY("Destroy"),
    ROAD("Road"),
    TOWER("Tower"),
    RAMPART("Rampart"),
    WALL("Wall")
}

fun getFlagType(flag: Flag) : FlagTypes {
    for (ft in FlagTypes.values()) {
        if (flag.name.startsWith(ft.prefix, true)) {
            return ft
        }
    }
    return FlagTypes.NONE
}