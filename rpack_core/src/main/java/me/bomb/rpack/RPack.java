package me.bomb.rpack;

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

import me.bomb.rpack.resourceserver.EnumStatus;

public interface RPack {
	
	/**
	 * Starts threads.
	 */
	public void enable();
	
	/**
	 * Stops threads.
	 */
	public void disable();
	
	/**
	 * Handle login.
	 */
	public void login(UUID playeruuid, InetAddress address);
	
	/**
	 * Handle logout.
	 */
	public void logout(UUID playeruuid);
	
	/**
	 * Get the names of packs that were loaded at least once.
	 *
	 * @return true if async used.
	 */
	public boolean getPacks(Consumer<String[]> resultConsumer);

	/**
	 * Loads resource pack to player.
	 * 
	 * @return true if async used.
	 */
	public boolean loadPack(UUID[] playeruuid, String name, boolean update, Consumer<EnumStatus> resultConsumer);
}
