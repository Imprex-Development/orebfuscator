package net.imprex.orebfuscator.config.context;

public interface ConfigParsingContext {

	ConfigParsingContext section(String path, boolean isolateErrors);

	default ConfigParsingContext section(String path) {
		return section(path, false);
	}

	ConfigParsingContext warn(String message);

	ConfigParsingContext warn(String path, String message);

	default ConfigParsingContext warnMissingSection() {
		warn("section is missing, using default one");
		return this;
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
		if (value < min) {
			error(path, String.format("value out of range {value(%d) not in range[%d, %d]}", value, min, max));
		}
		return this;
	}

	default ConfigParsingContext errorMissingOrEmpty(String path) {
		error(path, "is missing or empty");
		return this;
	}

	boolean hasErrors();
}
