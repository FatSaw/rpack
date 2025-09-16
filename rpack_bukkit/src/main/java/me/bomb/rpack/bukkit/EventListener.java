package me.bomb.rpack.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.bomb.rpack.RPack;

public final class EventListener implements Listener {
	private final RPack rpack;
	protected EventListener(RPack rpack) {
		this.rpack = rpack;
	}
	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		rpack.login(player.getUniqueId(), player.getAddress().getAddress());
	}
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		rpack.logout(player.getUniqueId());
	}
}
