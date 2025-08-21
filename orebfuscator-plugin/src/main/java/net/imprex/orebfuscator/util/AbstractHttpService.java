package net.imprex.orebfuscator.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.bukkit.plugin.PluginDescriptionFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.imprex.orebfuscator.Orebfuscator;

public abstract class AbstractHttpService {

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
			.registerTypeAdapter(Version.class, new Version.Json())
			.create();

	public static final HttpClient HTTP = HttpClient.newHttpClient();

	protected final String userAgent;

	public AbstractHttpService(Orebfuscator orebfuscator) {
		PluginDescriptionFile plugin = orebfuscator.getDescription();
		this.userAgent = String.format("Imprex-Development/%s/%s", plugin.getName(), plugin.getVersion());
	}

	protected HttpRequest.Builder request(String url) {
		return HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", userAgent)
				.header("Accept", "application/json");
	}

	protected static <T> BodyHandler<Optional<T>> optionalJson(Class<T> target) {
		return (responseInfo) -> responseInfo.statusCode() == 200
				? BodySubscribers.mapping(BodySubscribers.ofInputStream(), inputStream -> {
					try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
						return Optional.ofNullable(GSON.fromJson(reader, target));
					} catch (IOException e) {
						throw new UncheckedIOException("I/O while reading JSON", e);
					}
				})
				: BodySubscribers.replacing(Optional.empty());
	}
}
