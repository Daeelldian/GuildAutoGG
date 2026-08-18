package guildautogg.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GuildAutoGGClient implements ClientModInitializer {

    /*
     * Keep this URL pointed at the RAW file on GitHub.
     *
     * Edit triggers.json in the GitHub repository to change the triggers.
     * The mod downloads it at startup and refreshes it every 30 minutes.
     */
    private static final String TRIGGERS_URL =
            "https://raw.githubusercontent.com/Daeelldian/GuildAutoGG/main/triggers.json";
    private static final long TRIGGER_REFRESH_MINUTES = 30;

    private long lastTriggerTime = 0;
    private String lastWord = "";

    private final String[] GG_WORDS = {"gg", "ggs", "w", "nice"};
    private final Random random = new Random();

    /*
     * These are only a fallback for cases where GitHub cannot be reached.
     * Once a valid triggers.json has been downloaded, these are replaced by
     * the rules from that file.
     */
    private static final String[][] DEFAULT_TRIGGER_RULES = {
            {"➜"},
            {"!", "(+"},
            {"WOW!", "Dye"},
            {"TROPHY", "You caught"},
            {"OFFER ACCEPTED", ","},
            {"[SkyHanni]"},
            {"You Supercrafted", "!"},
            {"➡"},
            {"EXPORTATION"},
            {"UPGRADE!", "to"},
            {"SHINING!", "caught their first"}
    };

    private volatile List<List<String>> triggerRules = toRuleList(DEFAULT_TRIGGER_RULES);

    private final ScheduledExecutorService triggerRefreshExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "GuildAutoGG-trigger-refresh");
                thread.setDaemon(true);
                return thread;
            });

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                handleMessage(message.getString())
        );

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                handleMessage(message.getString())
        );

        // Do not block Minecraft's client thread while contacting GitHub.
        refreshTriggerRules();
        triggerRefreshExecutor.scheduleAtFixedRate(
                this::refreshTriggerRules,
                TRIGGER_REFRESH_MINUTES,
                TRIGGER_REFRESH_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private void refreshTriggerRules() {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(TRIGGERS_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "GuildAutoGG")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            System.err.println("[GuildAutoGG] Invalid trigger URL: " + e.getMessage());
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("GitHub returned HTTP " + response.statusCode());
                    }
                    return parseTriggerRules(response.body());
                })
                .thenAccept(rules -> {
                    triggerRules = rules;
                    System.out.println("[GuildAutoGG] Loaded " + rules.size()
                            + " trigger rule(s) from GitHub.");
                })
                .exceptionally(error -> {
                    System.err.println("[GuildAutoGG] Could not update trigger rules; "
                            + "keeping the previous rules. " + error.getMessage());
                    return null;
                });
    }

    private List<List<String>> parseTriggerRules(String json) {
        JsonElement root = JsonParser.parseString(json);

        // Accept either:
        //   [["word"], ["word1", "word2"]]
        // or:
        //   {"rules": [["word"], ["word1", "word2"]]}
        JsonElement rulesElement = root;
        if (root.isJsonObject() && root.getAsJsonObject().has("rules")) {
            rulesElement = root.getAsJsonObject().get("rules");
        }

        if (!rulesElement.isJsonArray()) {
            throw new IllegalArgumentException("Trigger repository must contain a JSON array.");
        }

        List<List<String>> rules = new ArrayList<>();
        JsonArray ruleArray = rulesElement.getAsJsonArray();

        for (JsonElement ruleElement : ruleArray) {
            if (!ruleElement.isJsonArray()) {
                throw new IllegalArgumentException("Each trigger rule must be an array of strings.");
            }

            List<String> rule = new ArrayList<>();
            for (JsonElement keywordElement : ruleElement.getAsJsonArray()) {
                if (!keywordElement.isJsonPrimitive()
                        || !keywordElement.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Every trigger keyword must be a string.");
                }

                String keyword = keywordElement.getAsString();
                if (!keyword.isEmpty()) {
                    rule.add(keyword);
                }
            }

            // Empty rules would match every Guild message, so reject them.
            if (!rule.isEmpty()) {
                rules.add(List.copyOf(rule));
            }
        }

        return List.copyOf(rules);
    }

    private static List<List<String>> toRuleList(String[][] rules) {
        List<List<String>> result = new ArrayList<>();
        for (String[] rule : rules) {
            result.add(List.of(rule));
        }
        return List.copyOf(result);
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

        var connection = client.getConnection();
        if (connection != null) {
            lastTriggerTime = currentTime;

            ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

            if (config.delayResponse && config.delayAmount > 0) {
                CompletableFuture.runAsync(() ->
                                client.execute(() -> {
                                    var delayedConnection = client.getConnection();
                                    if (delayedConnection != null) {
                                        delayedConnection.sendCommand("gc " + messageToSend);
                                    }
                                }),
                        CompletableFuture.delayedExecutor(config.delayAmount, TimeUnit.SECONDS));
            } else {
                connection.sendCommand("gc " + messageToSend);
            }
        }
    }

    private boolean isGGTrigger(String text) {
        if (!text.contains("Guild >")) {
            return false;
        }

        for (List<String> ruleSet : triggerRules) {
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
