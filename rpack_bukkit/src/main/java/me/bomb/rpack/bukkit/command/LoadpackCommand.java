package me.bomb.rpack.bukkit.command;

import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import me.bomb.rpack.RPack;
import me.bomb.rpack.resourceserver.EnumStatus;
import me.bomb.rpack.util.LangOptions;
import me.bomb.rpack.util.LangOptions.Placeholder;

public final class LoadpackCommand implements CommandExecutor {
	private final Server server;
	private final RPack rpack;
	private final SelectorProcessor selectorprocessor;

	public LoadpackCommand(Server server, RPack rpack, SelectorProcessor selectorprocessor) {
		this.server = server;
		this.rpack = rpack;
		this.selectorprocessor = selectorprocessor;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("rpack.loadpack")) {
			LangOptions.loadpack_nopermission.sendMsg(sender);
			return true;
		}
		if (args.length > 1) {
			UUID targetuuid = null;
			if (!args[0].equals("@n") || !sender.hasPermission("rpack.loadpack.update")) {
				if (args[0].equals("@s")) {
					if (sender instanceof Player) {
						args[0] = ((Player) sender).getName();
					} else {
						LangOptions.loadpack_noconsoleselector.sendMsg(sender);
						return true;
					}
				} else if (!sender.hasPermission("rpack.loadpack.other")) {
					LangOptions.loadpack_nopermissionother.sendMsg(sender);
					return true;
				} else {
					if (args[0].startsWith("@p")) {
						String closestplayername = selectorprocessor.getNearest(sender, args[0].substring(2));
						
						if(closestplayername == null) {
							LangOptions.loadpack_unavilableselector_near.sendMsg(sender);
							return true;
						}
						args[0] = closestplayername;
					}
					
					if (args[0].startsWith("@r")) {
						String randomplayername = selectorprocessor.getRandom(sender, args[0].substring(2));
						if(randomplayername == null) {
							LangOptions.loadpack_unavilableselector_random.sendMsg(sender);
							return true;
						}
						args[0] = randomplayername;
					}
					if(args[0].startsWith("@a")) {
						UUID[] targetarray = args[0].length() == 2 ? selectorprocessor.getAllGlobal() : selectorprocessor.getSameWorld(sender, args[0].substring(2)); 
						if(targetarray == null) {
							LangOptions.loadpack_unavilableselector_all.sendMsg(sender);
							return true;
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
						this.executeCommand(sender, name, targetarray);
						return true;
					}
				}
				
				Player target = server.getPlayerExact(args[0]);
				if (target == null) {
					LangOptions.loadpack_targetoffline.sendMsg(sender);
					return true;
				}
				targetuuid = target.getUniqueId();
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
			if(targetuuid == null) {
				executeCommand(sender, name, null);
				return true;
			}
			executeCommand(sender, name, new UUID[]{targetuuid});
		} else if(args.length == 1 && args[0].equals("@l") && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
			
			Consumer<String[]> consumer = new Consumer<String[]>() {
				@Override
				public void accept(String[] packs) {
					StringBuilder sb = new StringBuilder("Packs: ");
					for(String packid : packs) {
						sb.append('\n');
						sb.append(packid);
					}
					sender.sendMessage(sb.toString());
				}
				
			};
			rpack.getPacks(consumer);
			
		} else {
			LangOptions.loadpack_usage.sendMsg(sender);
		}
		return true;
	}
	
	private void executeCommand(CommandSender sender, String packname, UUID[] targetuuids) {
		Placeholder placeholder = new Placeholder("%packid%", packname);
		LangOptions.loadpack_processing.sendMsg(sender, placeholder);
		rpack.loadPack(targetuuids, packname, targetuuids == null, new Consumer<EnumStatus>() {
			@Override
			public void accept(EnumStatus status) {
				if(status == null) {
					return;
				}
				(status == EnumStatus.NOTEXSIST ? LangOptions.loadpack_nopack : status == EnumStatus.DISPATCHED ? LangOptions.loadpack_success_dispatched : status == EnumStatus.PACKED ? LangOptions.loadpack_success_updated : status == EnumStatus.REMOVED ? LangOptions.loadpack_success_deleted : LangOptions.loadpack_loaderunavilable).sendMsg(sender, placeholder);
			}
		});
	}
	
}
