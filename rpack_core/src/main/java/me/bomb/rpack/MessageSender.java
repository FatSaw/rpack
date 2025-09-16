package me.bomb.rpack;

public interface MessageSender {

	/**
	 * Sends message to player.
	 */
	public void send(Object target, String message);
	
	/**
	 * Get player language.
	 */
	public String getLocale(Object target);
	
}
