package fr.aumgn.dac2;

import fr.aumgn.bukkitutils.localization.Localization;
import fr.aumgn.bukkitutils.localization.PluginMessages;
import fr.aumgn.dac2.arena.Arenas;
import fr.aumgn.dac2.config.DACConfig;

public class DAC {

    private final DACPlugin plugin;

    private DACConfig config;
    private PluginMessages cmdMessages;
    private PluginMessages messages;

    private Arenas arenas;

    public DAC(DACPlugin plugin) {
        this.plugin = plugin;
        reloadData();
        this.arenas = new Arenas(this);
    }

    public DACPlugin getPlugin() {
        return plugin;
    }

    public DACConfig getConfig() {
        return config;
    }

    public PluginMessages getCmdMessages() {
        return cmdMessages;
    }

    public PluginMessages getMessages() {
        return messages;
    }

    public void reloadData() {
        config = plugin.reloadDACConfig();

        Localization localization =
                new Localization(plugin, config.getLocale());
        cmdMessages = localization.get("commands");
        messages = localization.get("messages");
    }

    public Arenas getArenas() {
        return arenas;
    }
}
