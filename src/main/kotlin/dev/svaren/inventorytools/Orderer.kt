package dev.svaren.inventorytools

import net.minecraft.item.Item
import net.minecraft.registry.Registries

class Orderer {
    private val itemOrderMap: MutableMap<Item, Int> = HashMap()

    // Get creative inventory order of an item
    public fun getCreativeOrder(item: Item): Int {
        return itemOrderMap[item] ?: Int.MAX_VALUE
    }

    constructor() {
        // Get all items in the game and set in the map
        Registries.ITEM.forEachIndexed { index, item ->
            itemOrderMap[item] = index
        }
    }

}