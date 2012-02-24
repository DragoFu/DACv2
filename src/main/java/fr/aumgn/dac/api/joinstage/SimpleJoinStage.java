package fr.aumgn.dac.api.joinstage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.aumgn.dac.api.DAC;
import fr.aumgn.dac.api.arena.Arena;
import fr.aumgn.dac.api.config.DACColor;
import fr.aumgn.dac.api.config.DACColors;
import fr.aumgn.dac.api.config.DACMessage;
import fr.aumgn.dac.api.event.joinstage.DACJoinStageJoinEvent;
import fr.aumgn.dac.api.event.stage.DACStagePlayerQuitEvent;
import fr.aumgn.dac.api.event.stage.DACStageStartEvent;
import fr.aumgn.dac.api.event.stage.DACStageStopEvent;
import fr.aumgn.dac.api.game.mode.DACGameMode;
import fr.aumgn.dac.api.game.mode.GameMode;
import fr.aumgn.dac.api.stage.SimpleStage;
import fr.aumgn.dac.api.stage.StagePlayer;
import fr.aumgn.dac.api.stage.StageQuitReason;

public class SimpleJoinStage extends SimpleStage implements JoinStage {

    private DACColors colors;
    private Set<DACColor> colorsMap;
    private List<SimpleJoinStagePlayer> players;

    public SimpleJoinStage(Arena arena) {
        super(arena);
        colors = DAC.getConfig().getColors();
        colorsMap = new HashSet<DACColor>();
        players = new ArrayList<SimpleJoinStagePlayer>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(DACMessage.JoinNewGame.format(arena.getName()));
            player.sendMessage(DACMessage.JoinNewGame2.getValue());
        }
        DAC.callEvent(new DACStageStartEvent(this));
    }

    private boolean isColorAvailable(String name) {
        DACColor color = colors.get(name);
        if (color == null) {
            return false;
        } else {
            return isColorAvailable(color);
        }
    }

    private boolean isColorAvailable(DACColor color) {
        return !colorsMap.contains(color);
    }

    private DACColor getFirstColorAvailable() {
        for (DACColor color : colors) {
            if (!colorsMap.contains(color)) {
                return color;
            }
        }
        // Should never be reached
        return colors.first();
    }

    private void addPlayer(Player player, DACColor color) {
        DACJoinStageJoinEvent event = new DACJoinStageJoinEvent(this, player, color, player.getLocation());
        DAC.callEvent(event);

        if (!event.isCancelled()) {
            SimpleJoinStagePlayer dacPlayer = new SimpleJoinStagePlayer(this, player, event.getColor(), event.getStartLocation());
            players.add(dacPlayer);
            DAC.getPlayerManager().register(dacPlayer);
            colorsMap.add(event.getColor());
            dacPlayer.send(DACMessage.JoinCurrentPlayers);
            for (StagePlayer currentPlayer : players) {
                dacPlayer.send(DACMessage.JoinPlayerList.format(currentPlayer.getDisplayName()));
            }
            send(DACMessage.JoinPlayerJoin.format(dacPlayer.getDisplayName()));
        }
    }

    @Override
    public void addPlayer(Player player, String[] colorsName) {
        int i = 0;
        DACColor color;
        while (i < colorsName.length && !isColorAvailable(colorsName[i])) {
            i++;
        }
        if (i == colorsName.length) {
            color = getFirstColorAvailable();
        } else {
            color = colors.get(colorsName[i]);
        }
        addPlayer(player, color);
    }

    @Override
    public void removePlayer(StagePlayer player, StageQuitReason reason) {
        DAC.callEvent(new DACStagePlayerQuitEvent(this, player));
        send(DACMessage.JoinPlayerQuit.format(player.getDisplayName()));
        players.remove(player);
        DAC.getPlayerManager().unregister(player);
        colorsMap.remove(player.getColor());
        if (players.size() == 0) {
            stop();
        }
    }

    @Override
    public List<StagePlayer> getPlayers() {
        return new ArrayList<StagePlayer>(players);
    }

    @Override
    public void stop() {
        DAC.callEvent(new DACStageStopEvent(this));
        DAC.getStageManager().unregister(this);
    }

    @Override
    public boolean isMinReached(GameMode mode) {
        int minimum = mode.getClass().getAnnotation(DACGameMode.class).minPlayers();
        return players.size() >= minimum;
    }

    @Override
    public boolean isMaxReached() {
        return (players.size() >= DAC.getConfig().getMaxPlayers());
    }

}
