package me.bomb.rpack.velocity;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.rpack.RPack;
import me.bomb.rpack.Configuration;
import me.bomb.rpack.LocalRPack;
import me.bomb.rpack.PackSender;
import me.bomb.rpack.ServerRPack;
import me.bomb.rpack.resourceserver.ResourceManager;
import me.bomb.rpack.source.DefaultPackSource;
import me.bomb.rpack.source.PackSource;
import me.bomb.rpack.util.LoggerUtil;
import me.bomb.rpack.util.LangOptions;
import me.bomb.rpack.velocity.command.LoadpackCommand;

public final class RPackVelocity {
	
	private static RPack instance = null;
	
	private final ProxyServer server;
    private final Logger logger;
	private final RPack rpack;
	private final ResourceManager resourcemanager;
	private final ConcurrentHashMap<UUID,InetAddress> playerips;
	private final boolean usecmd;
	private final String configerrors;
    
	@Inject
	public RPackVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		LoggerUtil.setLogger(new me.bomb.rpack.util.Logger() {
			@Override
			public void warn(String msg) {
				logger.warn(msg);
			}
			
			@Override
			public void info(String msg) {
				logger.info(msg);
			}
			
			@Override
			public void error(String msg) {
				logger.error(msg);
			}
		});
		Path plugindir = dataDirectory, configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), packdir = plugindir.resolve("Packs");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = true;
		Configuration config = new Configuration(plugindir.getFileSystem(), configfile, packdir, waitacception, false, "config_server.yml");
		this.configerrors = config.errors;
		if(config.use) {
			try {
				fsp.createDirectory(packdir);
			} catch (IOException e) {
			}
			this.usecmd = config.usecmd;
			playerips = config.sendpackstrictaccess ? new ConcurrentHashMap<UUID,InetAddress>(16,0.75f,1) : null;

			PackSender packsender = new VelocityPackSender(server);
	        
			PackSource packsource = new DefaultPackSource(packdir);
			if(config.connectuse) {
				ServerRPack rpack = new ServerRPack(config, packsource, packsender, playerips);
				this.resourcemanager = rpack.resourcemanager;
				this.rpack = rpack;
			} else {
				LocalRPack rpack = new LocalRPack(config, packsource, packsender, playerips);
				this.resourcemanager = rpack.resourcemanager;
				this.rpack = rpack;
			}
			this.server = server;
	        this.logger = logger;
			LangOptions.loadLang(new VelocityMessageSender(), langfile, false ? "lang_rgb.yml" : "lang_old.yml");
			if(RPackVelocity.instance == null) {
				RPackVelocity.instance = this.rpack;
			}
		} else {
			this.usecmd = false;
			this.playerips = null;
			this.resourcemanager = null;
			this.server = null;
	        this.logger = null;
			this.rpack = null;
		}
    }
	
	public final static RPack API() {
		return instance;
	}
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		if(!this.configerrors.isEmpty()) {
			logger.error("RPack config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.rpack == null) {
			return;
		}
		
		if(this.usecmd) {
			LoadpackCommand loadmusic = new LoadpackCommand(server, rpack);
			CommandManager cmdmanager = this.server.getCommandManager();
			CommandMeta loadmusicmeta = cmdmanager.metaBuilder("loadpack").plugin(this).build();
			cmdmanager.register(loadmusicmeta, loadmusic);
		}
		
		if(this.resourcemanager != null) {
			this.server.getEventManager().register(this, new EventListener(this.rpack, this.resourcemanager));
		}
		this.rpack.enable();
	}
	
	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		if(this.rpack == null) {
			return;
		}
		this.rpack.disable();
	}
	
}
