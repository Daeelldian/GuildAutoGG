package guildautogg.modid.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "guildautogg")
public class ModConfig implements ConfigData {

    public boolean delayResponse = true;
    public int delayAmount = 3;

}