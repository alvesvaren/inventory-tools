package dev.svaren.inventorytools

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.min

val orderer = Orderer()

private fun combineStacks(screen: HandledScreen<*>) {
    val slots = screen.screenHandler.slots.filter { it.inventory !is PlayerInventory }

    for (i in slots.indices) {
        val slot = slots[i]
        if (!slot.stack.isEmpty) {
            for (j in i + 1 until slots.size) {
                val targetSlot = slots[j]
                if (!targetSlot.stack.isEmpty && canCombine(slot.stack, targetSlot.stack)) {
                    mergeStacks(screen, slot, targetSlot)
                }
            }
        }
    }
}

private fun canCombine(stack1: ItemStack, stack2: ItemStack): Boolean {
    return stack1.item == stack2.item && stack1.count + stack2.count <= stack1.maxCount
}

private fun mergeStacks(screen: HandledScreen<*>, sourceSlot: Slot, targetSlot: Slot) {
    val sourceStack = sourceSlot.stack
    val targetStack = targetSlot.stack

    val transferableAmount =
        min((sourceStack.maxCount - sourceStack.count).toDouble(), targetStack.count.toDouble()).toInt()
    if (transferableAmount > 0) {
        simulateSlotClick(screen, targetSlot.id, 0, SlotActionType.PICKUP)
        simulateSlotClick(screen, sourceSlot.id, 0, SlotActionType.PICKUP)
        simulateSlotClick(screen, targetSlot.id, 0, SlotActionType.PICKUP)
    }

    if (!screen.screenHandler.cursorStack.isEmpty) {
        val firstEmptySlot = screen.screenHandler.slots.firstOrNull { it.stack.isEmpty }
        if (firstEmptySlot != null) {
            simulateSlotClick(screen, firstEmptySlot.id, 0, SlotActionType.PICKUP)
        }
    }
}

private fun simulateSlotClick(
    screen: HandledScreen<*>, slotId: Int, mouseButton: Int, actionType: SlotActionType
) {
    val client = MinecraftClient.getInstance()
    val interactionManager = client.interactionManager
    val player = client.player
    if (interactionManager != null && player != null) {
        interactionManager.clickSlot(screen.screenHandler.syncId, slotId, mouseButton, actionType, player)
    }
}

private fun sortInventory(screen: HandledScreen<*>) {
    val slots = screen.screenHandler.slots.filter { it.inventory !is PlayerInventory }
    val totalCount: Map<Item, Int> = slots.map { it.stack.item }.associateWith { item ->
        slots.filter { it.stack.item == item }.sumOf { it.stack.count }
    }

    // when controlDown shouldSwap should be random
    val controlDown = InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, GLFW.GLFW_KEY_LEFT_CONTROL)

    // when shift is down, shouldSwap should use totalCount, tiebreaker is creative
    val shiftDown = InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, GLFW.GLFW_KEY_LEFT_SHIFT)

    fun shouldSwap(slot1: Slot, slot2: Slot): Boolean {
        // Air is always last
        if (slot1.stack.isEmpty) {
            return true
        } else if (slot2.stack.isEmpty) {
            return false
        }

        // If the items are the same, emptier slots should come last
        if (slot1.stack.item == slot2.stack.item) {
            println("slot1: ${slot1.stack.item}, ${slot1.stack.count}")
            println("slot2: ${slot2.stack.item}, ${slot2.stack.count}")
            return slot1.stack.count < slot2.stack.count
        }

        if (controlDown && shiftDown) {
            return Math.random() < 0.5
        }

        val compare = orderer.getCreativeOrder(slot1.stack.item) > orderer.getCreativeOrder(slot2.stack.item)

        if (shiftDown) {
            val count1 = totalCount[slot1.stack.item] ?: 0
            val count2 = totalCount[slot2.stack.item] ?: 0
            return if (count1 == count2) {
                compare
            } else {
                count1 < count2
            }
        } else {
            return compare
        }
    }

    // Bubble sort the slots
    for (i in slots.indices) {
        for (j in 0 until slots.size - i - 1) {
            val slot1 = slots[j]
            val slot2 = slots[j + 1]
            if (shouldSwap(slot1, slot2)) {
                moveStack(screen, slot1, slot2)
            }
        }
    }
}


private fun moveStack(screen: HandledScreen<*>, slot1: Slot, slot2: Slot) {
    val highestCountSlot = if (slot1.stack.count > slot2.stack.count) slot1 else slot2
    val lowestCountSlot = if (slot1.stack.count > slot2.stack.count) slot2 else slot1
    if (slot1.id != slot2.id) {
        simulateSlotClick(screen, highestCountSlot.id, 0, SlotActionType.PICKUP)
        simulateSlotClick(screen, lowestCountSlot.id, 0, SlotActionType.PICKUP)
        simulateSlotClick(screen, highestCountSlot.id, 0, SlotActionType.PICKUP)
    }
}

class InventoryTools : ModInitializer {
    companion object {
        const val MOD_ID = "inventory-tools"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }

    private var wasMiddleMouseClicked = false

    override fun onInitialize() {
        LOGGER.info("Hello from inventory tools!")

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            MinecraftClient.getInstance().let { client ->
                val middleDown =
                    GLFW.glfwGetMouseButton(client.window.handle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS
                if (middleDown) {
                    if (wasMiddleMouseClicked) {
                        return@EndTick
                    }
                    wasMiddleMouseClicked = true
                    handleMiddleClick()
                } else {
                    wasMiddleMouseClicked = false
                }
            }
        })
    }

    private fun handleMiddleClick() {
        val currentScreen = MinecraftClient.getInstance().currentScreen;

        if (currentScreen is HandledScreen<*>) {
            combineStacks(currentScreen)
            sortInventory(currentScreen)
        }
    }
}
