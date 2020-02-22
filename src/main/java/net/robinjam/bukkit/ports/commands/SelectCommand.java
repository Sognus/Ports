package net.robinjam.bukkit.ports.commands;

import java.util.List;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import net.robinjam.bukkit.ports.persistence.Port;
import net.robinjam.bukkit.util.Command;
import net.robinjam.bukkit.util.CommandExecutor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;

/**
 * Handles the /port select command.
 *
 * @author robinjam
 * @author Sognus
 */
@Command(name = "select", usage = "[name]", permissions = "ports.select", playerOnly = true, min = 1, max = 1)
public class SelectCommand implements CommandExecutor {

	public void onCommand(CommandSender sender, List<String> args) {
		Player player = (Player) sender;
		Port port = Port.get(args.get(0));

		if (port == null) {
			sender.sendMessage(ChatColor.RED + "There is no such port.");
		} else if (!player.getWorld().getName().equals(port.getWorld())) {
			sender.sendMessage(ChatColor.RED
					+ "That port is in a different world ('" + port.getWorld()
					+ "').");
		} else {
			World worldEditWorld = BukkitAdapter.adapt(player.getWorld());
			CuboidRegionSelector selection = new CuboidRegionSelector(worldEditWorld, port.getActivationRegion().getPos1(), port.getActivationRegion().getPos2());
			WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

			if(worldEdit == null) {
				sender.sendMessage("Unable to load WorldEdit!");
			} else {
				LocalSession worldEditSession = worldEdit.getSession(player);
				worldEditSession.setRegionSelector(worldEditWorld, selection);
				sender.sendMessage(ChatColor.AQUA + "Activation region selected.");
			}
		}
	}

}
