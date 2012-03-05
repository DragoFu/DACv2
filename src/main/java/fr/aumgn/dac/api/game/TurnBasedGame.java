package fr.aumgn.dac.api.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;

import fr.aumgn.dac.api.DAC;
import fr.aumgn.dac.api.area.AreaColumn;
import fr.aumgn.dac.api.config.DACMessage;
import fr.aumgn.dac.api.event.game.DACGameFailEvent;
import fr.aumgn.dac.api.event.game.DACGameLooseEvent;
import fr.aumgn.dac.api.event.game.DACGameNewTurnEvent;
import fr.aumgn.dac.api.event.game.DACGameSuccessEvent;
import fr.aumgn.dac.api.event.game.DACGameTurnEvent;
import fr.aumgn.dac.api.event.stage.DACStageStartEvent;
import fr.aumgn.dac.api.event.stage.DACStageStopEvent;
import fr.aumgn.dac.api.game.event.GameFinish;
import fr.aumgn.dac.api.game.event.GameJumpFail;
import fr.aumgn.dac.api.game.event.GameJumpSuccess;
import fr.aumgn.dac.api.game.event.GameLoose;
import fr.aumgn.dac.api.game.event.GameNewTurn;
import fr.aumgn.dac.api.game.event.GameQuit;
import fr.aumgn.dac.api.game.event.GameStart;
import fr.aumgn.dac.api.game.event.GameTurn;
import fr.aumgn.dac.api.game.event.GameWin;
import fr.aumgn.dac.api.game.event.GameFinish.FinishReason;
import fr.aumgn.dac.api.game.mode.DACGameMode;
import fr.aumgn.dac.api.game.mode.GameHandler;
import fr.aumgn.dac.api.game.mode.GameMode;
import fr.aumgn.dac.api.stage.Stage;
import fr.aumgn.dac.api.stage.StagePlayer;
import fr.aumgn.dac.api.stage.StageQuitReason;
import fr.aumgn.dac.api.util.DACUtil;

public class TurnBasedGame extends SimpleGame {

    private Deque<StagePlayer> ranking;
    private int turn;
    private int turnTimeOutTaskId;
    private boolean finished;

    private Runnable turnTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            turnTimedOut();
        }
    };

    public TurnBasedGame(Stage stage, GameMode gameMode, GameHandler handler, GameOptions options) {
        this(stage, gameMode, handler, shuffle(stage.getPlayers()), options);
    }

    public TurnBasedGame(Stage stage, GameMode gameMode, GameHandler handler, List<? extends StagePlayer> playersList, GameOptions options) {
        super(stage, gameMode, handler, playersList, options);
        ranking = new ArrayDeque<StagePlayer>();
        turn = players.size() - 1;
        turnTimeOutTaskId = -1;
        finished = false;
        GameStart start = new GameStart(this);
        gameHandler.onStart(start);
        if (start.getPoolReset()) {
            arena.getPool().reset();
        }
        DAC.callEvent(new DACStageStartEvent(this));
        start.sendMessages();
        start.handleLosses();
        nextTurn();
    }

    private void increaseTurn() {
        turn++;
        if (turn == players.size()) {
            turn = 0;
            GameNewTurn newTurn = new GameNewTurn(this);
            gameHandler.onNewTurn(newTurn);
            DAC.callEvent(new DACGameNewTurnEvent(newTurn));
            newTurn.sendMessages();
            newTurn.handleLosses();
        }
    }

    private void turnTimedOut() {
        StagePlayer player = players.get(turn);
        send(DACMessage.GameTurnTimedOut.getContent(player.getDisplayName()));
        removePlayer(player, StageQuitReason.TurnTimeOut);
    }

    private void onLoose(StagePlayer player, GameLoose loose) {
        ranking.addFirst(player);
        DAC.getPlayerManager().unregister(player);
        players.remove(player);
        gameHandler.onLoose(loose);
        DAC.callEvent(new DACGameLooseEvent(loose));
        loose.sendMessages();
        loose.handleLosses();
        int min = mode.getClass().getAnnotation(DACGameMode.class).minPlayers();
        if (players.size() == min - 1) {
            if (players.size() == 1) {
                onWin(players.get(0));
            } else {
                stop(new GameFinish(this, FinishReason.NotEnoughPlayer));
            }
        }
    }

    private void stop(GameFinish finish) {
        gameHandler.onFinish(finish);
        DAC.callEvent(new DACStageStopEvent(this));
        finish.sendMessages();
        finished = true;
        Bukkit.getScheduler().cancelTask(turnTimeOutTaskId);
        DAC.getStageManager().unregister(this);
        if (finish.getPoolReset()) {
            arena.getPool().reset();
        }
    }

    @Override
    public List<StagePlayer> getPlayers() {
        return new ArrayList<StagePlayer>(players);
    }

    @Override
    public void removePlayer(StagePlayer player, StageQuitReason reason) {
        onLoose(player, new GameQuit(player, reason));
    }

    @Override
    public void stop() {
        stop(new GameFinish(this, FinishReason.Forced));
    }

    @Override
    public void nextTurn() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        if (turnTimeOutTaskId != -1) {
            scheduler.cancelTask(turnTimeOutTaskId);
        }
        increaseTurn();
        if (!finished) {
            StagePlayer player = players.get(turn);
            turnTimeOutTaskId = scheduler.scheduleAsyncDelayedTask(
                    DAC.getPlugin(), turnTimeOutRunnable, 
                    DAC.getConfig().getTurnTimeOut());
            GameTurn gameTurn = new GameTurn(player);
            gameHandler.onTurn(gameTurn);
            DAC.callEvent(new DACGameTurnEvent(gameTurn));
            gameTurn.sendMessages();
            gameTurn.handleLosses();
            if (gameTurn.getTeleport()) {
                player.tpToDiving();
            }
        }
    }

    @Override
    public boolean isPlayerTurn(StagePlayer player) {
        return !finished && players.get(turn).equals(player);
    }

    @Override
    public void onLoose(StagePlayer player) {
        addSpectator(player.getPlayer());      
        onLoose(player, new GameLoose(player));
    }

    @Override
    public void onFallDamage(EntityDamageEvent event) {
        Player player = (Player) event.getEntity();
        StagePlayer stagePlayer = DAC.getPlayerManager().get(player);
        if (isPlayerTurn(stagePlayer) && arena.getPool().isSafe(player)) {
            int x = player.getLocation().getBlockX();
            int z = player.getLocation().getBlockZ();
            GameJumpFail jumpFail = new GameJumpFail(stagePlayer, x, z);
            gameHandler.onFail(jumpFail);
            DAC.callEvent(new DACGameFailEvent(jumpFail));
            if (!jumpFail.isCancelled()) {
                if (jumpFail.getCancelDeath()) {
                    event.setCancelled(true);
                    DACUtil.fakeDamage(player);
                    if (jumpFail.getMustTeleport()) {
                        stagePlayer.tpAfterFail();
                    }
                }
                if (jumpFail.getSwitchToNextTurn()) {
                    nextTurn();
                }
                jumpFail.sendMessages();
                jumpFail.handleLosses();
            }
        }
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        StagePlayer stagePlayer = DAC.getPlayerManager().get(player);
        if (isPlayerTurn(stagePlayer) && arena.getPool().contains(player)) {
            int x = player.getLocation().getBlockX();
            int z = player.getLocation().getBlockZ();
            GameJumpSuccess jumpSuccess = new GameJumpSuccess(stagePlayer, x, z);
            gameHandler.onSuccess(jumpSuccess);
            DAC.callEvent(new DACGameSuccessEvent(jumpSuccess));
            if (!jumpSuccess.isCancelled()) {
                AreaColumn column = arena.getPool().getColumn(jumpSuccess.getPos());
                if (jumpSuccess.getColumnPattern() != null) {
                    column.set(jumpSuccess.getColumnPattern());
                }
                if (jumpSuccess.getMustTeleport()) {
                    stagePlayer.tpAfterJump();
                } else {
                    if (jumpSuccess.getColumnPattern() != null) {
                        player.teleport(new Location(
                                arena.getWorld(),
                                column.getX(),
                                column.getTop() + 1,
                                column.getZ())
                        );
                    }
                }
                jumpSuccess.sendMessages();
                jumpSuccess.handleLosses();
                if (jumpSuccess.getSwitchToNextTurn()) {
                    nextTurn();
                }
            }
        }
    }

    @Override
    public void onWin(StagePlayer player) {
        ranking.addFirst(player);
        stop(new GameWin(this, ranking));
    }

}