package me.bomb.rpack;

import java.util.UUID;

public interface PackSender {
	
	/**
	 * Sends resourcepack to player.
	 */
	public void send(UUID uuid, String url, byte[] sha1);
	
}
