package me.bomb.rpack.bukkit;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.rpack.RPack;
import me.bomb.rpack.ClientRPack;
import me.bomb.rpack.Configuration;
import me.bomb.rpack.LocalRPack;
import me.bomb.rpack.MessageSender;
import me.bomb.rpack.PackSender;
import me.bomb.rpack.bukkit.command.LoadpackCommand;
import me.bomb.rpack.bukkit.command.LoadpackTabComplete;
import me.bomb.rpack.bukkit.command.SelectorProcessor;
import me.bomb.rpack.bukkit.legacy.LegacyMessageSender_1_10_R1;
import me.bomb.rpack.bukkit.legacy.LegacyMessageSender_1_11_R1;
import me.bomb.rpack.bukkit.legacy.LegacyMessageSender_1_7_R4;
import me.bomb.rpack.bukkit.legacy.LegacyMessageSender_1_8_R3;
import me.bomb.rpack.bukkit.legacy.LegacyMessageSender_1_9_R2;
import me.bomb.rpack.bukkit.legacy.LegacyPackSender_1_10_R1;
import me.bomb.rpack.bukkit.legacy.LegacyPackSender_1_7_R4;
import me.bomb.rpack.bukkit.legacy.LegacyPackSender_1_8_R3;
import me.bomb.rpack.bukkit.legacy.LegacyPackSender_1_9_R2;
import me.bomb.rpack.resourceserver.ResourceManager;
import me.bomb.rpack.source.DefaultPackSource;
import me.bomb.rpack.source.PackSource;
import me.bomb.rpack.util.LoggerUtil;
import me.bomb.rpack.util.LangOptions;


public final class RPackBukkit extends JavaPlugin {
	
	private static RPack instance = null;
	
	private final RPack rpack;
	private final ConcurrentHashMap<UUID,InetAddress> playerips;
	private final boolean waitacception, usecmd;
	private final String configerrors;
	private final ResourceManager resourcemanager;

	public RPackBukkit() {
		LoggerUtil.setLogger(new me.bomb.rpack.util.Logger() {
			java.util.logging.Logger logger = RPackBukkit.this.getLogger();
			@Override
			public void warn(String msg) {
				logger.warning(msg);
			}
			
			@Override
			public void info(String msg) {
				logger.info(msg);
			}
			
			@Override
			public void error(String msg) {
				logger.severe(msg);
			}
		});
		final Server server = this.getServer();
		byte ver = 127;
		try {
			String nmsversion = this.getServer().getClass().getPackage().getName().substring(23);
			ver = Byte.valueOf(nmsversion.split("_", 3)[1]);
		} catch (StringIndexOutOfBoundsException | NumberFormatException e) {
		}
		
		Path plugindir = this.getDataFolder().toPath(), configfile = plugindir.resolve("config.yml"), langfile = plugindir.resolve("lang.yml"), packdir = plugindir.resolve("Packs");
		FileSystem fs = plugindir.getFileSystem();
		FileSystemProvider fsp = fs.provider();
		try {
			fsp.createDirectory(plugindir);
		} catch (IOException e) {
		}
		boolean waitacception = ver == 7 ? false : true;
		Configuration config = new Configuration(plugindir.getFileSystem(), configfile, packdir, waitacception, true, "config_client.yml");
		this.configerrors = config.errors;
		if(config.use) {
			try {
				fsp.createDirectory(packdir);
			} catch (IOException e) {
			}
			this.usecmd = config.usecmd;
			if(config.connectuse) {
				this.waitacception = false;
				this.playerips = null;
				ClientRPack rpack = new ClientRPack(config);
				this.resourcemanager = null;
				this.rpack = rpack;
			} else {
				PackSender packsender;
				switch (ver) {
				case 7:
					packsender = new LegacyPackSender_1_7_R4(server);
				break;
				case 8:
					packsender = new LegacyPackSender_1_8_R3(server);
				break;
				case 9:
					packsender = new LegacyPackSender_1_9_R2(server);
				break;
				case 10:
					packsender = new LegacyPackSender_1_10_R1(server);
				break;
				case 11: case 12:
					packsender = new BukkitPackSender(server);
				break;
				default:
					packsender = new BukkitPackSender(server);
				break;
				}
				this.waitacception = config.waitacception;
				playerips = config.sendpackstrictaccess ? new ConcurrentHashMap<UUID,InetAddress>(16,0.75f,1) : null;
				
				PackSource packsource = new DefaultPackSource(packdir);
				LocalRPack rpack = new LocalRPack(config, packsource, packsender, playerips);
				this.resourcemanager = rpack.resourcemanager;
				this.rpack = rpack;
			}
			MessageSender messagesender;
			switch (ver) {
			case 7:
				messagesender = new LegacyMessageSender_1_7_R4();
			break;
			case 8:
				messagesender = new LegacyMessageSender_1_8_R3();
			break;
			case 9:
				messagesender = new LegacyMessageSender_1_9_R2();
			break;
			case 10:
				messagesender = new LegacyMessageSender_1_10_R1();
			break;
			case 11:
				messagesender = new LegacyMessageSender_1_11_R1();
			break;
			default:
				messagesender = new SpigotMessageSender();
			break;
			}
			LangOptions.loadLang(messagesender, langfile, ver > 15 ? "lang_rgb.yml" : "lang_old.yml");
			if(RPackBukkit.instance == null) {
				RPackBukkit.instance = this.rpack;
			}
		} else {
			this.waitacception = false;
			this.usecmd = false;
			this.playerips = null;
			this.resourcemanager = null;
			this.rpack = null;
		}
	}
	
	public final static RPack API() {
		return instance;
	}

	//PLUGIN INIT START
	public void onEnable() {
		final Server server = this.getServer();
		if(!this.configerrors.isEmpty()) {
			this.getLogger().severe("RPack config initialization errors: \n".concat(configerrors));
			return;
		}
		if(this.rpack == null) {
			return;
		}
		if(this.usecmd) {
			SelectorProcessor selectorprocessor = new SelectorProcessor(server, new Random());
			PluginCommand loadmusiccommand = getCommand("loadpack");
			loadmusiccommand.setExecutor(new LoadpackCommand(server, rpack, selectorprocessor));
			loadmusiccommand.setTabCompleter(new LoadpackTabComplete(server, rpack));
		}
		if(playerips != null) {
			playerips.clear();
			for(Player player : server.getOnlinePlayers()) {
				playerips.put(player.getUniqueId(), player.getAddress().getAddress());
			}
		}
		PluginManager pluginmanager = server.getPluginManager();
		if(this.resourcemanager != null) {
			pluginmanager.registerEvents(new EventListener(this.rpack), this);
			if(waitacception) {
				pluginmanager.registerEvents(new PackStatusEventListener(resourcemanager), this);
			}
		}
		
		this.rpack.enable();
	}

	public void onDisable() {
		if(this.rpack == null) {
			return;
		}
		this.rpack.disable();
	}
	//PLUGIN INIT END

}
