package net.imprex.orebfuscator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.JavaVersion;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.PermissionUtil;

public class OrebfuscatorCommand implements CommandExecutor, TabCompleter {

	private static final List<String> TAB_COMPLETE = Arrays.asList("dump");

	private final DateTimeFormatter fileFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH.mm.ss");
	private final DateTimeFormatter timeFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final Orebfuscator orebfuscator;

	public OrebfuscatorCommand(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("orebfuscator")) {
			sender.sendMessage("Incorrect command registered!");
			return false;
		}

		if (!PermissionUtil.canAccessAdminTools(sender)) {
			sender.sendMessage("You don't have the 'orebfuscator.admin' permission.");
			return false;
		}

		if (args.length == 0) {
			sender.sendMessage("You are using " + this.orebfuscator.toString());
			sender.sendMessage(this.orebfuscator.getStatistics().toString());
		} else if (args[0].equalsIgnoreCase("dump")) {
			TemporalAccessor now = OffsetDateTime.now(ZoneOffset.UTC);

			JsonObject root = new JsonObject();
			root.addProperty("timestamp", timeFormat.format(now));
			
			JsonObject versions = new JsonObject();
			versions.addProperty("java", Integer.toString(JavaVersion.get()));
			versions.addProperty("nms", MinecraftVersion.nmsVersion());
			versions.addProperty("server", Bukkit.getVersion());
			versions.addProperty("bukkit", Bukkit.getBukkitVersion());
			versions.addProperty("protocolLib", ProtocolLibrary.getPlugin().toString());
			versions.addProperty("orebfuscator", orebfuscator.toString());
			root.add("versions", versions);

			root.add("statistics", orebfuscator.getStatistics().toJson());

			JsonObject plugins = new JsonObject();
			for (Plugin bukkitPlugin : Bukkit.getPluginManager().getPlugins()) {
				PluginDescriptionFile description = bukkitPlugin.getDescription();
				JsonObject plugin = new JsonObject();
				plugin.addProperty("version", description.getVersion());
				plugin.addProperty("author", description.getAuthors().toString());
				plugins.add(bukkitPlugin.getName(), plugin);
			}
			root.add("plugins", plugins);

			JsonObject worlds = new JsonObject();
			for (World bukkitWorld : Bukkit.getWorlds()) {
				JsonObject world = new JsonObject();
				world.addProperty("uuid", bukkitWorld.getUID().toString());
				world.addProperty("heightAccessor", HeightAccessor.get(bukkitWorld).toString());
				worlds.add(bukkitWorld.getName(), world);
			}
			root.add("worlds", worlds);

			JsonObject listeners = new JsonObject();
			for (PacketListener packetListener : ProtocolLibrary.getProtocolManager().getPacketListeners()) {
				JsonObject listener = new JsonObject();
				listener.addProperty("plugin", packetListener.getPlugin().toString());
				listener.addProperty("receivingWhitelist", packetListener.getSendingWhitelist().toString());
				listener.addProperty("sendingWhitelist", packetListener.getSendingWhitelist().toString());
				String key = packetListener.getClass().toGenericString() + "@" + System.identityHashCode(packetListener);
				listeners.add(key, listener);
			}
			root.add("listeners", listeners);

			Base64.Encoder encoder = Base64.getUrlEncoder();

			String latestLog = OFCLogger.getLatestVerboseLog();
			root.addProperty("verboseLog", encoder.encodeToString(latestLog.getBytes(StandardCharsets.UTF_8)));

			try {
				Path configPath = orebfuscator.getDataFolder().toPath().resolve("config.yml");
				String config = Files.readAllLines(configPath).stream().collect(Collectors.joining("\n"));
				root.addProperty("config", encoder.encodeToString(config.getBytes(StandardCharsets.UTF_8)));
			} catch (IOException e) {
				e.printStackTrace();
			}

			String configReport = orebfuscator.getOrebfuscatorConfig().report();
			configReport = configReport != null ? configReport : "";
			root.addProperty("configReport", encoder.encodeToString(configReport.getBytes(StandardCharsets.UTF_8)));

			Path path = orebfuscator.getDataFolder().toPath().resolve("dump-" + fileFormat.format(now) + ".json");
			try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(path))) {
				writer.setIndent("  ");
				Streams.write(root, writer);
			} catch (IOException e) {
				e.printStackTrace();
			}

			sender.sendMessage("Dump file created at: " + path);
		} else {
			return false;
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return args.length == 1 ? TAB_COMPLETE : Collections.emptyList();
	}
}
