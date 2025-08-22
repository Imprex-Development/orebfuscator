package dev.imprex.orebfuscator.config.context;

public interface ConfigParsingContext {

	ConfigParsingContext section(String path, boolean isolateErrors);

	default ConfigParsingContext section(String path) {
		return section(path, false);
	}

	ConfigParsingContext warn(String message);

	ConfigParsingContext warn(String path, String message);

	default ConfigParsingContext warnMissingSection() {
		warn("section is missing, adding default one");
		return this;
	}

	default ConfigParsingContext warnMissingOrEmpty() {
		warn("is missing or empty");
		return this;
	}

	default ConfigParsingContext warnUnknownBlock(String name) {
		warn(String.format("contains unknown block '%s', skipping", name));
		return this;
	}

	default ConfigParsingContext warnAirBlock(String name) {
		warn(String.format("contains air block '%s', skipping", name));
		return this;
	}

	default boolean disableIfError(boolean enabled) {
		if (enabled && hasErrors()) {
			warn("section got disabled due to errors");
			return false;
		}
		return enabled;
	}

	ConfigParsingContext error(String message);

	ConfigParsingContext error(String path, String message);

	default ConfigParsingContext errorMinValue(String path, long min, long value) {
		if (value < min) {
			error(path, String.format("value too low {value(%d) < min(%d)}", value, min));
		}
		return this;
	}

	default ConfigParsingContext errorMinMaxValue(String path, long min, long max, long value) {
		if (value < min || value > max) {
			error(path, String.format("value out of range {value(%d) not in range[%d, %d]}", value, min, max));
		}
		return this;
	}

	default ConfigParsingContext errorMissingOrEmpty() {
		error("is missing or empty");
		return this;
	}

	boolean hasErrors();
}
