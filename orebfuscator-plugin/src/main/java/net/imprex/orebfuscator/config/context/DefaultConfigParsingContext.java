package net.imprex.orebfuscator.config.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.imprex.orebfuscator.util.OFCLogger;

public class DefaultConfigParsingContext implements ConfigParsingContext {

	private static final String ANSI_RESET = "\u001B[m";
	private static final String ANSI_ERROR = "\u001B[31;1m"; // Bold Red
	private static final String ANSI_WARN = "\u001B[33;1m"; // Bold Yellow

	private final int depth;
	private boolean isolateErrors = false;

	private final Map<String, DefaultConfigParsingContext> section = new LinkedHashMap<>();
	private final List<Message> messages = new ArrayList<>();

	public DefaultConfigParsingContext() {
		this(0);
	}

	private DefaultConfigParsingContext(int depth) {
		this.depth = depth;
	}

	@Override
	public DefaultConfigParsingContext section(String path, boolean isolateErrors) {
		DefaultConfigParsingContext context = getContext(path);
		context.isolateErrors = isolateErrors;
		return context;
	}

	@Override
	public ConfigParsingContext warn(String message) {
		this.messages.add(new Message(false, message));
		return this;
	}

	@Override
	public ConfigParsingContext warn(String path, String message) {
		getContext(path).warn(message);
		return this;
	}

	@Override
	public ConfigParsingContext error(String message) {
		this.messages.add(new Message(true, message));
		return this;
	}

	@Override
	public ConfigParsingContext error(String path, String message) {
		getContext(path).error(message);
		return this;
	}

	@Override
	public boolean hasErrors() {
		for (Message message : this.messages) {
			if (message.isError()) {
				return true;
			}
		}

		for (var section : this.section.values()) {
			if (!section.isolateErrors && section.hasErrors()) {
				return true;
			}
		}

		return false;
	}

	private DefaultConfigParsingContext getContext(String path) {
		DefaultConfigParsingContext context = this;

		for (String segment : path.split("\\.")) {
			DefaultConfigParsingContext nextContext = context.section.get(segment);
			if (nextContext == null) {
				nextContext = new DefaultConfigParsingContext(context.depth + 1);
				context.section.put(segment, nextContext);
			}
			context = nextContext;
		}

		return context;
	}

	private int getMessageCount() {
		int messageCount = this.messages.size();

		for (DefaultConfigParsingContext section : section.values()) {
			messageCount += section.getMessageCount();
		}

		return messageCount;
	}

	private String buildReport() {
		int messageCount = this.getMessageCount();
		if (messageCount == 0) {
			return "";
		}

		final StringBuilder builder = new StringBuilder();
		final String indent = "  ".repeat(this.depth);

		// sort -> errors should come before warnings
		Collections.sort(this.messages);

		for (Message message : this.messages) {
			String color = message.isError() ? ANSI_ERROR : ANSI_WARN;
			builder.append(indent).append(color).append("- ").append(message.content()).append('\n');
		}

		for (var entry : this.section.entrySet()) {
			if (entry.getValue().getMessageCount() == 0) {
				continue;
			}
			builder.append(indent).append(ANSI_WARN).append(entry.getKey()).append(":\n");
			builder.append(entry.getValue().buildReport());
		}

		return builder.toString();
	}

	public String report() {
		int messageCount = this.getMessageCount();
		if (messageCount == 0) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("Encountered ").append(messageCount).append(" issue(s) while parsing the config:\n")
			.append(ANSI_RESET)
			.append(buildReport())
			.append(ANSI_RESET);

		String message = builder.toString();
		OFCLogger.log(hasErrors() ? Level.SEVERE : Level.WARNING, message);
		return message;
	}

	private record Message(boolean isError, String content) implements Comparable<Message> {

		@Override
		public int compareTo(Message o) {
			int a = this.isError ? 1 : 0;
			int b = o.isError ? 1 : 0;
			return b - a;
		}
	}
}
