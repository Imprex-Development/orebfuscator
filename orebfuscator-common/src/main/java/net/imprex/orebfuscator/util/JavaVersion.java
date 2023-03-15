package net.imprex.orebfuscator.util;

public final class JavaVersion {

	private static final int JAVA_VERSION = javaMajorVersion();

	public static int get() {
		return JAVA_VERSION;
	}

	private static int javaMajorVersion() {
		return majorVersion(System.getProperty("java.specification.version", "1.6"));
	}

	/**
	 * taken from:
	 * https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/PlatformDependent0.java#L1011
	 */
	private static int majorVersion(final String javaSpecVersion) {
        final String[] components = javaSpecVersion.split("\\.");
        final int[] version = new int[components.length];
        for (int i = 0; i < components.length; i++) {
            version[i] = Integer.parseInt(components[i]);
        }

        if (version[0] == 1) {
            assert version[1] >= 6;
            return version[1];
        } else {
            return version[0];
        }
    }

	private JavaVersion() {
	}
}
