package com.playmonumenta.scriptedquests.adapters;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.adventure.PaperAdventure;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.minecraft.server.v1_16_R3.ChatComponentUtils;
import net.minecraft.server.v1_16_R3.TileEntityCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VersionAdapter_v1_16_R3 implements VersionAdapter {

	public void setAutoState(CommandBlock state, boolean auto) {
		((CraftCommandBlock) state).getTileEntity().b(auto);
	}

	public void removeAllMetadata(Plugin plugin) {
		CraftServer server = (CraftServer) plugin.getServer();
		server.getEntityMetadata().removeAll(plugin);
		server.getPlayerMetadata().removeAll(plugin);
		server.getWorldMetadata().removeAll(plugin);
		for (World world : Bukkit.getWorlds()) {
			((CraftWorld) world).getBlockMetadata().removeAll(plugin);
		}
	}

	public @Nullable ParseResults<?> parseCommand(String command) {
		try {
			String testCommandStr = command.replaceAll("@S", "testuser").replaceAll("@N", "testnpc").replaceAll("@U", UUID.randomUUID().toString().toLowerCase());
			return ((CraftServer) Bukkit.getServer()).getServer().getCommandDispatcher().dispatcher().parse(testCommandStr, ((CraftServer) Bukkit.getServer()).getServer().getServerCommandListener());
		} catch (Exception e) {
			// Failed to test the command - ignore it and print a log message
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Component resolveComponents(Component component, Player player) {
		try {
			return PaperAdventure.asAdventure(ChatComponentUtils.filterForDisplay(((CraftPlayer) player).getHandle().getCommandListener(),
				new AdventureComponent(component).deepConverted(), ((CraftPlayer) player).getHandle(), 0));
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return component;
		}
	}

	public void executeCommandAsBlock(Block block, String command) {
		TileEntityCommand tileEntity = new TileEntityCommand();
		tileEntity.setLocation(((CraftBlock) block).getCraftWorld().getHandle(), ((CraftBlock) block).getPosition());
		Bukkit.dispatchCommand(tileEntity.getCommandBlock().getBukkitSender(tileEntity.getCommandBlock().getWrapper()), command);
	}

}
