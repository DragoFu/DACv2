package fr.aumgn.dac.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.aumgn.dac.DAC;
import fr.aumgn.dac.config.DACMessage;
import fr.aumgn.dac.player.DACPlayer;
import fr.aumgn.dac.stage.StageManager;
import fr.aumgn.utils.command.PlayerCommandExecutor;

public class KickCommand extends PlayerCommandExecutor {

	@Override
	public boolean checkUsage(String[] args) {
		return args.length == 1;
	}

	@Override
	public void onPlayerCommand(Context context, String[] args) {
		StageManager stageManager = DAC.getStageManager();
		for (Player player : matchPlayer(context, args[0])) {
			DACPlayer dacPlayer = stageManager.getPlayer(player);
			dacPlayer.getStage().removePlayer(dacPlayer);
		}

		error(DACMessage.CmdKickNotInGame);
	}

	public List<Player> matchPlayer(Context context, String arg) {
		List<Player> list = Bukkit.matchPlayer(arg);
		if (list.isEmpty()) {
			error(DACMessage.CmdKickNoPlayerFound);
		}
		return list;
	}

}
