package me.bomb.rpack.velocity.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.bomb.rpack.RPack;
import me.bomb.rpack.resourceserver.EnumStatus;
import me.bomb.rpack.util.LangOptions;
import me.bomb.rpack.util.LangOptions.Placeholder;

public final class LoadpackCommand implements SimpleCommand {
	
	private final ProxyServer server;
	private final RPack rpack;
	
	public LoadpackCommand(ProxyServer server, RPack rpack) {
		this.server = server;
		this.rpack = rpack;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("rpack.loadpack")) {
			LangOptions.loadpack_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !sender.hasPermission("rpack.loadpack.update")) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getUsername();
					} else {
						LangOptions.loadpack_noconsoleselector.sendMsg(sender);
						return;
					}
				} else if (!sender.hasPermission("rpack.loadpack.other")) {

					LangOptions.loadpack_nopermissionother.sendMsg(sender);
					return;
				}
				
				Optional<Player> target = server.getPlayer(args[0]);
				if (target.isEmpty()) {
					LangOptions.loadpack_targetoffline.sendMsg(sender);
					return;
				}
				targetuuid = target.get().getUniqueId();
			}
			if(args.length>2) {
				StringBuilder sb = new StringBuilder(args[1]);
				for(int i = 2;i < args.length;++i) {
					sb.append(' ');
					sb.append(args[i]);
				}
				args[1] = sb.toString();
			}
			String name = args[1];
			
			Placeholder placeholder = new Placeholder("%packid%", name);
			LangOptions.loadpack_processing.sendMsg(sender, placeholder);
			rpack.loadPack(targetuuid == null ? null : new UUID[] {targetuuid}, name, targetuuid == null, new Consumer<EnumStatus>() {
				@Override
				public void accept(EnumStatus status) {
					if(status == null) {
						return;
					}
					(status == EnumStatus.NOTEXSIST ? LangOptions.loadpack_nopack : status == EnumStatus.DISPATCHED ? LangOptions.loadpack_success_dispatched : status == EnumStatus.PACKED ? LangOptions.loadpack_success_updated : status == EnumStatus.REMOVED ? LangOptions.loadpack_success_deleted : LangOptions.loadpack_loaderunavilable).sendMsg(sender, placeholder);
				}
			});
		} else if(args.length == 1 && args[0].equals("@l") && sender instanceof ConsoleCommandSource) {
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] packs) {
					StringBuilder sb = new StringBuilder("Packs: ");
					for(String packid : packs) {
						sb.append('\n');
						sb.append(packid);
					}
					sender.sendPlainMessage(sb.toString());
				}
				
			};
			rpack.getPacks(consumer);
		} else {
			LangOptions.loadpack_usage.sendMsg(sender);
		}
		return;
		
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("rpack.loadpack")) {
			return null;
		}
		String[] args = invocation.arguments();
		List<String> tabcomplete = new ArrayList<String>();
		if (args.length <= 1) {
			if (sender instanceof Player) {
				tabcomplete.add("@s");
			}
			if (sender.hasPermission("rpack.loadpack.other")) {
				for (Player player : server.getAllPlayers()) {
					if (player.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getUsername());
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
