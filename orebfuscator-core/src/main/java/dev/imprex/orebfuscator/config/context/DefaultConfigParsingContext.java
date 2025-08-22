package dev.imprex.orebfuscator.config.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultConfigParsingContext implements ConfigParsingContext {

  private static final String ANSI_RESET = "\u001B[m";
  private static final String ANSI_ERROR = "\u001B[31;1m"; // Bold Red
  private static final String ANSI_WARN = "\u001B[33;1m"; // Bold Yellow

  private final int depth;
  private boolean isolateErrors = false;

  private final Map<String, DefaultConfigParsingContext> section = new LinkedHashMap<>();
  private final List<Message> messages = new ArrayList<>();

  public DefaultConfigParsingContext() {
    this(null);
  }

  private DefaultConfigParsingContext(@Nullable DefaultConfigParsingContext parentContext) {
    this.depth = parentContext != null
        ? parentContext.depth + 1
        : 0;
  }

  @Override
  @NotNull
  public DefaultConfigParsingContext section(@NotNull String path, boolean isolateErrors) {
    DefaultConfigParsingContext context = getContext(path);
    context.isolateErrors = isolateErrors;
    return context;
  }

  @Override
  public void warn(@NotNull ConfigMessage message, @Nullable Object... arguments) {
    Objects.requireNonNull(message, "message can't be null");

    this.messages.add(new Message(false, message.format(arguments)));
  }

  @Override
  public void warn(@NotNull String path, @NotNull ConfigMessage message, @Nullable Object... arguments) {
    getContext(path).warn(message, arguments);
  }

  @Override
  public void error(@NotNull ConfigMessage message, @Nullable Object... arguments) {
    Objects.requireNonNull(message, "message can't be null");

    this.messages.add(new Message(true, message.format(arguments)));
  }

  @Override
  public void error(@NotNull String path, @NotNull ConfigMessage message, @Nullable Object... arguments) {
    getContext(path).error(message, arguments);
  }

  @Override
  @Contract(pure = true)
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

  private DefaultConfigParsingContext getContext(@NotNull String path) {
    Objects.requireNonNull(path, "context path can't be null");

    DefaultConfigParsingContext context = this;
    for (String segment : path.split("\\.")) {
      if (segment.isBlank()) {
        throw new IllegalArgumentException("ConfigParsingContext path doesn't support blank segments: " + path);
      }

      DefaultConfigParsingContext nextContext = context.section.get(segment);
      if (nextContext == null) {
        nextContext = new DefaultConfigParsingContext(context);
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

  private StringBuilder buildReport(final StringBuilder builder) {
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
      entry.getValue().buildReport(builder);
    }

    return builder;
  }

  @Nullable
  public String report() {
    int messageCount = this.getMessageCount();
    if (messageCount == 0) {
      return null;
    }

    StringBuilder builder = new StringBuilder()
        .append(hasErrors() ? ANSI_ERROR : ANSI_WARN)
        .append("Encountered ").append(messageCount).append(" issue(s) while parsing the config:\n")
        .append(ANSI_RESET);

    return buildReport(builder)
        .append(ANSI_RESET)
        .toString();
  }

  private record Message(boolean isError, @NotNull String content) implements Comparable<Message> {

    @Override
    public int compareTo(Message o) {
      int a = this.isError ? 1 : 0;
      int b = o.isError ? 1 : 0;
      return b - a;
    }
  }
}
