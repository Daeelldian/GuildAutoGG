package guildautogg.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;

public class GuildAutoGGClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Hypixel often sends custom chat as GAME (system) messages, not standard player CHAT.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleMessage(message.getString());
        });

        // We also register for standard CHAT messages just to be safe.
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleMessage(message.getString());
        });
    }

    private void handleMessage(String text) {
        // Check if the message contains both "Guild >" and "!"
        if (text.contains("Guild >") && text.contains("!")) {
            MinecraftClient client = MinecraftClient.getInstance();

            // Ensure we are fully in-game before attempting to send a command
            if (client.getNetworkHandler() != null) {
                // sendCommand automatically adds the "/" prefix in modern Fabric
                client.getNetworkHandler().sendCommand("gc gg");
            }
        }
    }
}