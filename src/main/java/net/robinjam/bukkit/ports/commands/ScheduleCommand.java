package net.robinjam.bukkit.ports.commands;

import java.sql.Time;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.robinjam.bukkit.ports.persistence.Port;
import net.robinjam.bukkit.util.Command;
import net.robinjam.bukkit.util.CommandExecutor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Handles the /port schedule command.
 * 
 * @author robinjam
 * @author Sognus
 */
@Command(name = "schedule", usage = "[name] <schedule>", permissions = "ports.schedule", min = 1, max = 20)
public class ScheduleCommand implements CommandExecutor {

	public void onCommand(CommandSender sender, List<String> args) {
		Port port = Port.get(args.get(0));
		
		if (port == null) {
			sender.sendMessage(ChatColor.RED + "There is no such port.");
		}
		else {
			if (args.size() >= 2) {
				long days = 0;
				long hours = 0;
				long minutes = 0;
				long seconds = 0;
				long milis = 0;

				for(int i = 1; i < args.size(); i++) {
					String arg = args.get(i);
					Pattern pattern = Pattern.compile("(\\d+)?(\\w+)");
					Matcher matcher = pattern.matcher(arg);

					while(matcher.find()) {
						if(matcher.groupCount() == 1){
							try {
								long val = Integer.parseInt(matcher.group(1));
								milis += val;
							} catch(Exception e) {
								// pass
							}
						} else if(matcher.groupCount() == 2) {
							try {
								long val = Integer.parseInt(matcher.group(1));
								String type = matcher.group(2);

								switch (type) {
									case "d":
										days += val;
										break;
									case "h":
										hours += val;
										break;
									case "m":
										minutes += val;
										break;
									case "s":
										seconds += val;
										break;
									case "ms":
										milis += val;
										break;
									default:
										throw new IllegalArgumentException("Time unit not supported");
								}

							} catch (Exception e) {
								// pass
							}
						}
					}

				}

				// Add parsed time to total
				milis += TimeUnit.DAYS.toMillis(days);
				milis += TimeUnit.HOURS.toMillis(hours);
				milis += TimeUnit.MINUTES.toMillis(minutes);
				milis += TimeUnit.SECONDS.toMillis(seconds);

				if(milis < 1) {
					port.setDepartureSchedule(null);
					sender.sendMessage(ChatColor.AQUA + "Departure schedule removed.");
				} else {
					port.setDepartureSchedule((Long.valueOf(milis).intValue()));
					sender.sendMessage(ChatColor.AQUA + "Departure schedule updated.");
				}
			}
			else {
				port.setDepartureSchedule(null);
				sender.sendMessage(ChatColor.AQUA + "Departure schedule removed.");
			}
			Port.save();
		}
	}

}
