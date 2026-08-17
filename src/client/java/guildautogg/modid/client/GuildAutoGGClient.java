package guildautogg.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GuildAutoGGClient implements ClientModInitializer {

    private long lastTriggerTime = 0;
    private String lastWord = "";

    private final String[] GG_WORDS = {"gg", "ggs", "w", "nice"};
    private final Random random = new Random();

    private static final String[][] TRIGGER_RULES = {
            {"➜"},
            {"!", "(+"},
            {"WOW!", "Dye"},
            {"TROPHY", "You caught"},
            {"OFFER ACCEPTED", ","},
            {"[SkyHanni]"},
            {"You Supercrafted", "!"},
            {"➡"},
            {"EXPORTATION"},
            {"UPGRADE!","to"},
            {"SHINING!","caught their first"}
    };

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        // Fixed: Removed the { } brackets to use expression lambdas
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                handleMessage(message.getString())
        );

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                handleMessage(message.getString())
        );
    }

    private void handleMessage(String text) {
        if (!isGGTrigger(text)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        int colonIndex = text.indexOf(":");
        if (colonIndex != -1) {
            String senderInfo = text.substring(0, colonIndex);
            String myName = client.player.getName().getString();

            if (senderInfo.contains(myName)) {
                return;
            }

            String actualMessage = text.substring(colonIndex + 1).trim();
            if (actualMessage.length() < 11) {
                return;
            }
        } else {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTriggerTime < 4000) {
            return;
        }

        String messageToSend;
        if (random.nextInt(1000) == 0) {
            messageToSend = "Not that impressive";
        } else {
            String chosenWord;
            do {
                chosenWord = GG_WORDS[random.nextInt(GG_WORDS.length)];
            } while (chosenWord.equals(lastWord));

            lastWord = chosenWord;
            messageToSend = chosenWord;
        }

        // Fixed: Stored the connection in a 'var' to satisfy the NullPointerException warning
        var connection = client.getConnection();
        if (connection != null) {
            lastTriggerTime = currentTime;

            ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

            if (config.delayResponse && config.delayAmount > 0) {
                // Fixed: Replaced the background statement lambda with an expression lambda
                CompletableFuture.runAsync(() ->
                                client.execute(() -> {
                                    var delayedConnection = client.getConnection();
                                    if (delayedConnection != null) {
                                        delayedConnection.sendCommand("gc " + messageToSend);
                                    }
                                })
                        , CompletableFuture.delayedExecutor(config.delayAmount, TimeUnit.SECONDS));
            } else {
                connection.sendCommand("gc " + messageToSend);
            }
        }
    }

    private boolean isGGTrigger(String text) {
        if (!text.contains("Guild >")) {
            return false;
        }
        for (String[] ruleSet : TRIGGER_RULES) {
            boolean allKeywordsMatch = true;
            for (String keyword : ruleSet) {
                if (!text.contains(keyword)) {
                    allKeywordsMatch = false;
                    break;
                }
            }
            if (allKeywordsMatch) {
                return true;
            }
        }
        return false;
    }
}