package net.robinjam.bukkit.ports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.milkbowl.vault.economy.Economy;
import net.robinjam.bukkit.ports.persistence.Port;
import net.robinjam.bukkit.util.EconomyUtils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author robinjam
 * @author Sognus
 */
public class PortTickTask implements Runnable, Listener {
	
	private Map<Player, Port> playerLocations = new HashMap<Player, Port>();
	private Map<Player, Integer> playerCooldowns = new HashMap<Player, Integer>();
	private Set<Player> authorizedPlayers = new HashSet<Player>();
	private long tickNumber = 1;

	@Override
	public void run() {
		Ports plugin = Ports.getInstance();
		long portTickPeriod = plugin.getConfig().getLong("port-tick-period");
		long notifyTickPeriod = plugin.getConfig().getLong("notify-tick-period");
		
		// Iterate over every player on the server
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			// Check if the player was standing in a port's activation zone on the last port tick
			if (playerLocations.containsKey(player)) {
				// Check if the player is still standing in the port's activation zone
				Port port = playerLocations.get(player);
				if (port.contains(player.getLocation())) {
					if (authorizedPlayers.contains(player)) {
						// If it's time for the port to depart, teleport the player
						if (portShouldDepart(port)) {
							teleportPlayer(player, port.getDestination());
							
							// If the port has a cooldown, add the player to the cooldown list
							if (port.getCooldown() != null)
								playerCooldowns.put(player, port.getCooldown());
							
							playerLocations.remove(player);
							authorizedPlayers.remove(player);
						}
						// Otherwise, notify the player when the next departure will be if the notification period has elapsed
						else if (tickNumber == notifyTickPeriod) {
							player.sendMessage(plugin.translate("port-tick-task.notify", port.getDescription(), formatNextDeparture(port)));
						}
					}
				}
				// If the player has left the activation zone, bid them farewell and remove them from the player location tracker
				else {
					playerLocations.remove(player);
					authorizedPlayers.remove(player);
					player.sendMessage(plugin.translate("port-tick-task.leave"));
				}
			}
			// The player was not standing in a port's activation zone on the last port tick
			else {
				// Check if the player is now standing in a port's activation zone
				for (Port port : Port.getAll()) {
					if (port.contains(player.getLocation())) {
						// If the player is currently in a vehicle, eject them (otherwise the teleport will be unsuccessful)
						if (player.getVehicle() != null)
							player.getVehicle().eject();
						
						if (playerCanUsePort(player, port)) {
							// If the port is an insta-port, teleport the player immediately
							if (port.getDepartureSchedule() == null) {
								teleportPlayer(player, port.getDestination());
								
								// If the port has a cooldown, add the player to the cooldown list
								if (port.getCooldown() != null)
									playerCooldowns.put(player, port.getCooldown());
								
								break;
							}
							// Otherwise, add them to the player location tracker
							else {
								playerLocations.put(player, port);
								authorizedPlayers.add(player);
								player.sendMessage(plugin.translate("port-tick-task.enter", port.getDescription(), formatNextDeparture(port)));
								break;
							}
						} else {
							playerLocations.put(player, port);
							authorizedPlayers.remove(player);
						}
					}
				}
			}
		}

		// Update all ports
		for (Port port : Port.getAll()) {
			Integer departureSchedule = port.getDepartureSchedule() == null ? 0 : port.getDepartureSchedule();

			if(System.currentTimeMillis() > port.getLastPortTime() + departureSchedule) {
				port.refreshLastPortTime();
			}
		}
		
		// Increment the tick counter
		if (tickNumber == notifyTickPeriod)
			tickNumber = 1;
		else
			++tickNumber;
		
		// Update player cooldowns
		Iterator<Map.Entry<Player, Integer>> it = playerCooldowns.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Player, Integer> entry = it.next();
			int cooldown = entry.getValue();
			cooldown -= portTickPeriod;
			if (cooldown > 0)
				entry.setValue(cooldown);
			else
				it.remove();
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onVehicleEnter(VehicleEnterEvent event) {
		if (event.isCancelled() || !(event.getEntered() instanceof Player))
			return;
		
		playerLocations.remove((Player) event.getEntered());
	}
	
	private String formatNextDeparture(Port port) {
		// TODO: replace with realtime
		long nextPortMilis = port.getLastPortTime() + port.getDepartureSchedule();
		long difference = nextPortMilis - System.currentTimeMillis();
		return formatTime(difference);
	}
	
	private String formatTime(long millis) {
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		millis -= TimeUnit.SECONDS.toMillis(seconds);

		// Start a new String
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");

		// Days
		if(days > 0){
			sb.append(String.format("%dd"));
			// Spacer
			sb.append(" ");
		}


		// Hours
		if(days > 0 || hours > 0){
			sb.append(String.format("%dh", hours));
			// Spacer
			sb.append(" ");
		}


		// Minutes - always shown
		sb.append(String.format("%dm", minutes));

		// Spacer
		sb.append(" ");

		// Seconds - always shown
		sb.append(String.format("%ds", seconds));

		// Spacer
		sb.append(" ");

		// Miliseconds - always shown
		sb.append(String.format("%dms", millis));

		// End of formatting
		sb.append(" ]");

		// Return built string
		return sb.toString();

	}
	
	private boolean portShouldDepart(Port port) {
		// True if enough time ellapsed since last port - refresh in PortTickTask because of multiple players teleport
		return System.currentTimeMillis() > port.getLastPortTime() + port.getDepartureSchedule();
	}
	
	private boolean playerCanUsePort(Player player, Port port) {
		Ports plugin = Ports.getInstance();
		
		// Ensure the player is not on cooldown
		Integer cooldown = playerCooldowns.get(player);
		if (cooldown != null) {
			player.sendMessage(plugin.translate("port-tick-task.cooldown", formatTime(cooldown)));
			return false;
		}
		
		// Ensure the port has a destination
		if (port.getDestination() == null) {
			player.sendMessage(plugin.translate("port-tick-task.no-destination", port.getDescription()));
			return false;
		}
		
		// Ensure the player has permission to use the port
		if (port.getPermission() != null && !player.hasPermission(port.getPermission())) {
			player.sendMessage(plugin.translate("port-tick-task.no-permission", port.getDescription()));
			return false;
		}
		
		// Ensure the player is carrying the correct ticket (if required)
		Integer ticketItemId = port.getTicketItemId();
		Integer ticketDataValue = port.getTicketDataValue();
		ItemStack heldItem = player.getInventory().getItemInMainHand();
		byte heldData = heldItem.getData().getData();
		if (ticketItemId != null && heldItem.getType().getId() != ticketItemId || (ticketDataValue != null && heldData != ticketDataValue)) {
			player.sendMessage(plugin.translate("port-tick-task.no-ticket", port.getDescription()));
			return false;
		}
		
		// Ensure the player has enough money to use the port (if required)
		Economy economy;
		if (port.getPrice() != null && (economy = EconomyUtils.getEconomy()) != null) {
			if (economy.withdrawPlayer(player, port.getPrice()).transactionSuccess()) {
				player.sendMessage(plugin.translate("port-tick-task.payment-taken", economy.format(port.getPrice())));
			} else {
				player.sendMessage(plugin.translate("port-tick-task.not-enough-money", port.getDescription(), economy.format(port.getPrice())));
				return false;
			}
		}

		// Remove the ticket from the player's hand
		if (ticketItemId != null) {
			int heldItemAmount = player.getInventory().getItemInMainHand().getAmount();
			if (heldItemAmount == 1)
				player.getInventory().setItemInMainHand(null);
			else
				player.getInventory().getItemInMainHand().setAmount(heldItemAmount - 1);
			player.sendMessage(plugin.translate("port-tick-task.ticket-taken"));
		}
		
		return true;
	}
	
	public static void teleportPlayer(Player player, Port port) {
		player.teleport(port.getArrivalLocation());

		// Reset the player's fall distance so they don't take fall damage
		player.setFallDistance(0.0f);

		// Refresh the chunk to prevent chunk errors
		World world = player.getWorld();
		Chunk chunk = world.getChunkAt(player.getLocation());
		chunk.unload();
		chunk.load();
		player.sendMessage(Ports.getInstance().translate("port-tick-task.depart"));
	}

}
