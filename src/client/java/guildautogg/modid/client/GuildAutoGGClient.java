package guildautogg.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.Random;

public class GuildAutoGGClient implements ClientModInitializer {

    // Variables to track our cooldown and the last word used
    private long lastTriggerTime = 0;
    private String lastWord = "";

    // The pool of responses
    private final String[] GG_WORDS = {"gg", "ggs", "w", "nice"};
    private final Random random = new Random();

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((message, _) -> handleMessage(message.getString()));

        ClientReceiveMessageEvents.CHAT.register((message, _, _, _, _) -> handleMessage(message.getString()));
    }

    private void handleMessage(String text) {
        // Check if the overall message contains "Guild >" and "!"
        if (text.contains("Guild >") && (text.contains("➜") || (text.contains("!") && text.contains("(+")) || (text.contains("WOW!") && text.contains("Dye")))) {

            // 1. Length Check
            // Find the colon that separates the player's name from their message
            int colonIndex = text.indexOf(":");
            if (colonIndex != -1) {
                // Extract the message after the colon and trim leading/trailing spaces
                String actualMessage = text.substring(colonIndex + 1).trim();

                // If the player's actual message is shorter than 11 characters, ignore it
                if (actualMessage.length() < 11) {
                    return;
                }
            } else {
                // If we somehow can't find a colon, abort to be safe
                return;
            }

            // 2. Cooldown Check
            // Ensure 4000 milliseconds (4 seconds) have passed since the last trigger
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTriggerTime < 4000) {
                return;
            }

            // 3. Select the Response
            String messageToSend;

            // 0.1% chance (1 in 1000) to be unimpressed
            if (random.nextInt(1000) == 0) {
                messageToSend = "Not that impressive";
            } else {
                // Pick a random word from the pool until we get one that isn't the last used word
                String chosenWord;
                do {
                    chosenWord = GG_WORDS[random.nextInt(GG_WORDS.length)];
                } while (chosenWord.equals(lastWord));

                lastWord = chosenWord; // Save the word so it can't be used next time
                messageToSend = chosenWord;
            }

            // 4. Send the Command
            Minecraft client = Minecraft.getInstance();
            if (client.getConnection() != null) {
                client.getConnection().sendCommand("gc " + messageToSend);

                // Update the cooldown timer only after a successful message is sent
                lastTriggerTime = currentTime;
            }
        }
    }
}