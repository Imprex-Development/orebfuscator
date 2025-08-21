package net.imprex.orebfuscator;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import com.google.gson.annotations.SerializedName;

import net.imprex.orebfuscator.config.GeneralConfig;
import net.imprex.orebfuscator.util.AbstractHttpService;
import net.imprex.orebfuscator.util.ConsoleUtil;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.Version;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class UpdateSystem extends AbstractHttpService {

	private static final Pattern DEV_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-b(?<build>\\d+))?");

	private static boolean isDevVersion(String version) {
		Matcher matcher = DEV_VERSION_PATTERN.matcher(version);
		return matcher.find() && matcher.group("build") != null;
	}

	private static final String API_URI = "https://api.modrinth.com/v2/project/orebfuscator/version?loaders=%s&game_versions=%s";
	private static final String DOWNLOAD_URI = "https://modrinth.com/plugin/orebfuscator/version/%s";

	private static final Duration CACHE_DURATION = Duration.ofMinutes(10L);

	private final Orebfuscator orebfuscator;
	private final GeneralConfig generalConfig;

	private final AtomicReference<Instant> validUntil = new AtomicReference<>();
	private final AtomicReference<CompletableFuture<Optional<ModrinthVersion>>> latestVersion = new AtomicReference<>();

	public UpdateSystem(Orebfuscator orebfuscator) {
		super(orebfuscator);

		this.orebfuscator = orebfuscator;
		this.generalConfig = orebfuscator.getOrebfuscatorConfig().general();
		
		this.checkForUpdates();
	}

	private CompletableFuture<Optional<ModrinthVersion>> requestLatestVersion() {
		String installedVersion = this.orebfuscator.getDescription().getVersion();
		if (!this.generalConfig.checkForUpdates() || isDevVersion(installedVersion)) {
			OFCLogger.debug("UpdateSystem - Update check disabled or dev version detected; skipping");
			return CompletableFuture.completedFuture(Optional.empty());
		}

		var uri = String.format(API_URI, "bukkit", MinecraftVersion.current());
		return HTTP.sendAsync(request(uri).build(), optionalJson(ModrinthVersion[].class)).thenApply(response ->
			response.body().flatMap(body -> {
				var version = Version.parse(installedVersion);
				var latestVersion = Arrays.stream(body)
						.filter(e -> Objects.equals(e.versionType, "release"))
						.filter(e -> Objects.equals(e.status, "listed"))
						.sorted(Comparator.reverseOrder())
						.findFirst();

				latestVersion.ifPresentOrElse(
						v -> OFCLogger.debug("UpdateSystem - Fetched latest version " + v.version),
						() -> OFCLogger.debug("UpdateSystem - Couldn't fetch latest version"));
				
				return latestVersion.map(v -> version.isBelow(v.version) ? v : null);
			})
		).exceptionally(throwable -> {
			OFCLogger.log(Level.WARNING, "UpdateSystem - Unable to fetch latest version", throwable);
			return Optional.empty();
		});
	}

	private CompletableFuture<Optional<ModrinthVersion>> getLatestVersion() {
		Instant validUntil = this.validUntil.get();
		if (validUntil != null && validUntil.compareTo(Instant.now()) < 0 && this.validUntil.compareAndSet(validUntil, null)) {
			OFCLogger.debug("UpdateSystem - Cleared latest cached version");
			this.latestVersion.set(null);
		}

	    CompletableFuture<Optional<ModrinthVersion>> existingFuture = this.latestVersion.get();
	    if (existingFuture != null) {
	        return existingFuture;
	    }

		CompletableFuture<Optional<ModrinthVersion>> newFuture = new CompletableFuture<>();
		if (this.latestVersion.compareAndSet(null, newFuture)) {
			OFCLogger.debug("UpdateSystem - Starting to check for updates");
			this.requestLatestVersion().thenAccept(version -> {
				this.validUntil.set(Instant.now().plus(CACHE_DURATION));
				newFuture.complete(version);
			});
			return newFuture;
		} else {
			return this.latestVersion.get();
		}
	}

	private void ifNewerVersionAvailable(Consumer<ModrinthVersion> consumer) {
		this.getLatestVersion().thenAccept(o -> o.ifPresent(consumer));
	}

	private void checkForUpdates() {
		this.ifNewerVersionAvailable(version -> {
			String downloadUri = String.format(DOWNLOAD_URI, version.version);
			ConsoleUtil.printBox(Level.WARNING, "UPDATE AVAILABLE", "", downloadUri);
		});
	}

	public void checkForUpdates(Player player) {
		this.ifNewerVersionAvailable(version -> {
			String downloadUri = String.format(DOWNLOAD_URI, version.version);
			BaseComponent[] components = new ComponentBuilder("[§bOrebfuscator§f]§7 A new release is available ")
					.append("§f§l[CLICK HERE]")
					.event(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUri))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder("§7Click here to see the latest release").create()))).create();
			OrebfuscatorCompatibility.runForPlayer(player, () -> {
				player.spigot().sendMessage(components);
			});
		});
	}

	public static class ModrinthVersion implements Comparable<ModrinthVersion> {

		private static final Comparator<ModrinthVersion> COMPARATOR = 
				Comparator.comparing(e -> e.version, Comparator.nullsLast(Version::compareTo));

		@SerializedName("version_number")
		public Version version;

		@SerializedName("game_versions")
		public List<Version> gameVersions;

		@SerializedName("version_type")
		public String versionType;

		@SerializedName("loaders")
		public List<String> loaders;
		
		@SerializedName("status")
		public String status;

		@Override
		public int compareTo(ModrinthVersion other) {
			return COMPARATOR.compare(this, other);
		}
	}
}
