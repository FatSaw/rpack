package me.bomb.rpack.resourceserver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import me.bomb.rpack.PackSender;

public final class ResourceDispatcher {
	
	private final PackSender packsender;
	protected final ResourceManager resourcemanager;
	private final byte[] host;
	private final int end;
	/**
	 * Dispatch resourcepack byte[] to targets
	 */
	public ResourceDispatcher(PackSender packsender, ResourceManager resourcemanager, String host) throws NullPointerException {
		if(packsender == null || resourcemanager == null || host == null) {
			throw new NullPointerException();
		}
		this.packsender = packsender;
		this.resourcemanager = resourcemanager;
		this.end = host.length();
		int i = this.end + 40;
		this.host = new byte[i];
		this.host[--i] = 'p';
		this.host[--i] = 'i';
		this.host[--i] = 'z';
		this.host[--i] = '.';
		i-=36;
		byte[] hostb = host.getBytes(StandardCharsets.UTF_8);
		while(--i > -1) {
			this.host[i] = hostb[i];
		}
	}
	
	/**
	 * Dispatch resourcepack byte[] to targets as is
	 */
	public final void dispatch(final String id, final UUID[] targets, final byte[] resource, final byte[] sha1) {
		UUID[] tokens = resourcemanager.generateTokens(resource, targets);
		int i = tokens.length;
		byte[] host = new byte[this.host.length];
		System.arraycopy(this.host, 0, host, 0, this.host.length);
		while(--i > -1) {
			final UUID target = targets[i], token = tokens[i];
			final byte[] tokenbytes = token.toString().getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(tokenbytes, 0, host, this.end, 36);
			packsender.send(target, new String(host, StandardCharsets.UTF_8), sha1);
		}
	}
	
	/**
	 * Dispatch resourcepack byte[] to targets and calculate hash
	 */
	public final boolean dispatch(final String id, final UUID[] targets, final byte[] resource) {
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
		byte[] sha1 = sha1hash.digest(resource);
		this.dispatch(id, targets, resource, sha1);
		return true;
	}

}
