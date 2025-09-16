package me.bomb.rpack.velocity;

import java.util.UUID;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent.Status;
import com.velocitypowered.api.proxy.Player;

import me.bomb.rpack.RPack;
import me.bomb.rpack.resourceserver.ResourceManager;

public final class EventListener {
	private final RPack rpack;
	private final ResourceManager resourcemanager;
	protected EventListener(RPack rpack,ResourceManager resourcemanager) {
		this.rpack = rpack;
		this.resourcemanager = resourcemanager;
	}
	@Subscribe
	public void onLoginEvent(LoginEvent event) {
		Player player = event.getPlayer();
		rpack.login(player.getUniqueId(), player.getRemoteAddress().getAddress());
	}
	@Subscribe
	public void onDisconnectEvent(DisconnectEvent event) {
		rpack.logout(event.getPlayer().getUniqueId());
	}
	@Subscribe
	public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Status status = event.getStatus();
		if(status==Status.ACCEPTED) {
			resourcemanager.setAccepted(uuid);
			return;
		}
		if(status==Status.DECLINED||status==Status.FAILED_DOWNLOAD||status==Status.SUCCESSFUL) {
			resourcemanager.remove(uuid);
		}
	}
	
}
