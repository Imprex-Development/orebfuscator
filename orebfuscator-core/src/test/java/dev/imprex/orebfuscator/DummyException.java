package dev.imprex.orebfuscator;

/**
 * DummyException is a singleton exception used in tests to ensure a consistent stack trace. The stack trace is fixed
 * and shouldn't be changed, even if the class is modified.
 */
public class DummyException extends Exception {

  /**
   * The single instance of DummyException used across all tests.
   */
  public static final DummyException INSTANCE = new DummyException();

  /**
   * Private constructor to prevent additional instances. The stack trace is fixed and the message is set to "dummy
   * exception" to maintain consistency in tests.
   */
  private DummyException() {
    super("dummy exception");

    // Set a fixed stack trace
    this.setStackTrace(new StackTraceElement[]{
        new StackTraceElement("dev.imprex.orebfuscator.DummyException", "<init>", "DummyException.java", 26),
        new StackTraceElement("dev.imprex.orebfuscator.DummyException", "<clinit>", "DummyException.java", 18)
    });
  }
}
