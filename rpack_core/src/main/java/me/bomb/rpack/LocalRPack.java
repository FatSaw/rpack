package me.bomb.rpack;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import me.bomb.rpack.resourceserver.EnumStatus;
import me.bomb.rpack.resourceserver.ResourceDispatcher;
import me.bomb.rpack.resourceserver.ResourceManager;
import me.bomb.rpack.source.PackSource;

import static me.bomb.rpack.util.NameFilter.filterName;

public class LocalRPack implements RPack {
	public final PackSource packsource;
	public final ResourceManager resourcemanager;
	public final ResourceDispatcher dispatcher;
	private final ConcurrentHashMap<UUID,InetAddress> playerips;
	
	public LocalRPack(Configuration config, PackSource packsource, PackSender packsender, ConcurrentHashMap<UUID,InetAddress> playerips) {
		this.packsource = packsource;
		this.resourcemanager = new ResourceManager(config.servercache, config.clientcache ? config.tokensalt : null, config.waitacception, config.sendpackstrictaccess ? playerips.values() : null, config.sendpackifip, config.sendpackport, config.sendpackbacklog, config.sendpacktimeout, config.sendpackserverfactory, (short) 2);
		this.dispatcher = new ResourceDispatcher(packsender, resourcemanager, config.sendpackhost);
		this.playerips = playerips;
	}
	
	public void enable() {
		resourcemanager.start();
	}
	
	public void disable() {
		resourcemanager.end();
		resourcemanager.clearCache();
	}
	
	public void login(UUID playeruuid, InetAddress address) {
		if(playerips != null) {
			playerips.put(playeruuid, address);
		}
	}
	
	public void logout(UUID playeruuid) {
		resourcemanager.remove(playeruuid);
		if(playerips != null) {
			playerips.remove(playeruuid);
		}
	}
	
	private AtomicBoolean cachePacksUpdated = new AtomicBoolean(false);
	private String[] cachePacks = null;
	
	public final boolean getPacks(Consumer<String[]> resultConsumer) {
		if(cachePacksUpdated.get()) {
			resultConsumer.accept(cachePacks);
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				String[] names = packsource.ids();
				resultConsumer.accept(names);
				String[] namescache = new String[names.length];
				System.arraycopy(names, 0, namescache, 0, names.length);
				cachePacksUpdated.set(true);
				cachePacks = namescache;
			}
		};
		r.run();
		return false;
	}
	
	public final boolean loadPack(UUID[] playeruuid, String id, boolean update, Consumer<EnumStatus> resultConsumer) {
		String filteredId = filterName(id);
		Runnable r = new Runnable() {
			public void run() {
				byte[] resource;
				if(update) {
					cachePacksUpdated.set(false);
					resource = packsource.get(filteredId);
				} else {
					resource = resourcemanager.getCached(filteredId);
					if(resource == null) {
						resource = packsource.get(filteredId);
					}
				}
				if(resource == null) {
					resultConsumer.accept(update && resourcemanager.removeCache(filteredId) ? EnumStatus.REMOVED : EnumStatus.NOTEXSIST);
				} else {
					resourcemanager.putCache(filteredId, resource);
					if(playeruuid != null) {
						dispatcher.dispatch(filteredId, playeruuid, resource);
						resultConsumer.accept(EnumStatus.DISPATCHED);
					} else {
						resultConsumer.accept(update ? EnumStatus.PACKED : EnumStatus.UNAVILABLE);
					}
				}
			}
		};
		r.run();
		return false;
	}
	
}
