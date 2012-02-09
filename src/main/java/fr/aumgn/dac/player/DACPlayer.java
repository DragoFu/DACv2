package fr.aumgn.dac.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.aumgn.dac.config.DACColor;
import fr.aumgn.dac.stage.Stage;

public interface DACPlayer {
	
	boolean equals(DACPlayer obj);
	
	Stage getStage();

	Player getPlayer();
	
	String getDisplayName();
	
	DACColor getColor();
	
	Location getStartLocation();
	
}