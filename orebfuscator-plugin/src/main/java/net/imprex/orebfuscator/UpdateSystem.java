package net.imprex.orebfuscator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.imprex.orebfuscator.config.GeneralConfig;
import net.imprex.orebfuscator.util.ConsoleUtil;
import net.imprex.orebfuscator.util.OFCLogger;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

public class UpdateSystem {

	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-b(\\d+))?");

	private static final String API_LATEST = "https://api.github.com/repos/Imprex-Development/Orebfuscator/releases/latest";
	private static final long UPDATE_COOLDOWN = 1_800_000L; // 30min

	private final Lock lock = new ReentrantLock();

	private final Orebfuscator orebfuscator;
	private final GeneralConfig generalConfig;

	private JsonObject releaseData;
	private long updateCooldown = -1;
	private int failedAttempts = 0;

	public UpdateSystem(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.generalConfig = orebfuscator.getOrebfuscatorConfig().general();
		
		this.checkForUpdates();
	}

	private JsonObject getReleaseData() {
		this.lock.lock();
		try {
			long systemTime = System.currentTimeMillis();

			if (this.failedAttempts < 5) {

				if (this.releaseData != null || systemTime - this.updateCooldown > UPDATE_COOLDOWN) {
					try {
						URL url = new URL(API_LATEST);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						try (InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream())) {
							this.releaseData = new JsonParser().parse(inputStreamReader).getAsJsonObject();
							this.updateCooldown = systemTime;
						}
					} catch (IOException e) {
						OFCLogger.warn("Unable to fetch latest update from: " + API_LATEST);
						OFCLogger.warn(e.toString());

						if (++this.failedAttempts == 5) {
							this.updateCooldown = systemTime;
						}
					}
				}

			} else if (systemTime - this.updateCooldown > UPDATE_COOLDOWN) {
				this.failedAttempts = 0;
				this.updateCooldown = -1;
				return this.getReleaseData();
			}

			return this.releaseData;
		} finally {
			this.lock.unlock();
		}
	}

	private String getTagName() {
		JsonObject releaseData = this.getReleaseData();
		if (releaseData != null && releaseData.has("tag_name")) {
			return releaseData.getAsJsonPrimitive("tag_name").getAsString();
		}
		return null;
	}

	private String getHtmlUrl() {
		JsonObject releaseData = this.getReleaseData();
		if (releaseData != null && releaseData.has("html_url")) {
			return releaseData.getAsJsonPrimitive("html_url").getAsString();
		}
		return null;
	}

	private boolean isDevVersion(String version) {
		Matcher matcher = VERSION_PATTERN.matcher(version);
		return matcher.find() && matcher.groupCount() == 4;
	}

	private boolean isUpdateAvailable() {
		String version = this.orebfuscator.getDescription().getVersion();
		if (this.generalConfig.checkForUpdates() && !this.isDevVersion(version)) {
			String tagName = this.getTagName();
			return tagName != null && !version.equals(tagName);
		}
		return false;
	}

	private void checkForUpdates() {
		Bukkit.getScheduler().runTaskAsynchronously(this.orebfuscator, () -> {
			if (this.isUpdateAvailable()) {
				ConsoleUtil.printBox(Level.WARNING,
						"UPDATE AVAILABLE", "", this.getHtmlUrl());
			}
		});
	}

	public void checkForUpdates(Player player) {
		Bukkit.getScheduler().runTaskAsynchronously(this.orebfuscator, () -> {
			if (this.isUpdateAvailable()) {
				BaseComponent[] components = new ComponentBuilder("[§bOrebfuscator§f]§7 A new release is available ")
						.append("§f§l[CLICK HERE]")
						.event(new ClickEvent(ClickEvent.Action.OPEN_URL, this.getHtmlUrl()))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click here to see the latest release").create())).create();
				Bukkit.getScheduler().runTask(this.orebfuscator, () -> {
					player.spigot().sendMessage(components);
				});
			}
		});
	}
}
