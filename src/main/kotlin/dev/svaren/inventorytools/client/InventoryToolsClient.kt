package dev.svaren.inventorytools.client

import dev.svaren.inventorytools.InventoryTools.Companion.LOGGER
import dev.svaren.inventorytools.InventoryTools.Companion.MOD_ID
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

public class InventoryToolsClient : ClientModInitializer {
    override fun onInitializeClient() {
        LOGGER.info("Hello from client!");
    }
}