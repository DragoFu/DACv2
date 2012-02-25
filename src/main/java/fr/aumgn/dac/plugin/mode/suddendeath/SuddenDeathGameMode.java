package fr.aumgn.dac.plugin.mode.suddendeath;

import fr.aumgn.dac.api.game.Game;
import fr.aumgn.dac.api.game.GameOptions;
import fr.aumgn.dac.api.game.TurnBasedGame;
import fr.aumgn.dac.api.game.mode.DACGameMode;
import fr.aumgn.dac.api.game.mode.GameMode;
import fr.aumgn.dac.api.game.mode.GameHandler;
import fr.aumgn.dac.api.stage.Stage;
import fr.aumgn.dac.api.stage.StagePlayer;

@DACGameMode(name = "sudden-death", isDefault = false, aliases = {"sd"})
public class SuddenDeathGameMode implements GameMode {

    @Override
    public Game createGame(Stage stage, GameOptions options) {
        return new TurnBasedGame(this, stage, options);
    }

    @Override
    public GameHandler createHandler() {
        return new SuddenDeathGameHandler();
    }

    @Override
    public SuddenDeathGamePlayer createPlayer(Game game, StagePlayer player, int index) {
        return new SuddenDeathGamePlayer(game, player, index);
    }

}
