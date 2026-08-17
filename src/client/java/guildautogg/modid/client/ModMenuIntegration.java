package guildautogg.modid.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
// You might need to let IntelliJ auto-import the exact path for AutoConfigClient
import me.shedaniel.autoconfig.AutoConfigClient;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // We now call AutoConfigClient instead of AutoConfig
        return parent -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get();
    }
}