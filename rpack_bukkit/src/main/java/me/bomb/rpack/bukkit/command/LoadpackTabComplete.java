package me.bomb.rpack.bukkit.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.bomb.rpack.RPack;

public final class LoadpackTabComplete implements TabCompleter {
	private final Server server;
	private final RPack rpack;

	public LoadpackTabComplete(Server server, RPack rpack) {
		this.server = server;
		this.rpack = rpack;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("rpack.loadpack")) {
			return null;
		}
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("rpack.loadpack.other")) {
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
			return tabcomplete;
		}
		
		//TODO: Suggest with space limit for pre 1.13 clients to avoid wrong values
		if (args.length > 1 && !args[0].equals("@l")) {
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] packs) {
					if (packs != null) {
						int lastspace = -1;
						if(args.length > 2) {
							StringBuilder sb = new StringBuilder(args[1]);
							for(int i = 2;i < args.length;++i) {
								sb.append(' ');
								sb.append(args[i]);
							}
							args[1] = sb.toString();
							lastspace = args[1].lastIndexOf(' ');
						}
						++lastspace;
						if(lastspace == 0) {
							for (String pack : packs) {
								if (pack.startsWith(args[1]) && pack.indexOf(0xA7) == -1) {
									tabcomplete.add(pack);
								}
							}
						} else {
							for (String pack : packs) {
								if (lastspace < pack.length() && pack.startsWith(args[1]) && pack.indexOf(0xA7) == -1) {
									pack = pack.substring(lastspace);
									tabcomplete.add(pack);
								}
							}
						}
					}
					synchronized (tabcomplete) {
						tabcomplete.notify();
					}
				}
			};
			boolean async = rpack.getPacks(consumer);
			if(async) {
				try {
					synchronized (tabcomplete) {
						tabcomplete.wait(200);
					}
				} catch (InterruptedException e) {
				}
			}
		}
		return tabcomplete;
	}

}
