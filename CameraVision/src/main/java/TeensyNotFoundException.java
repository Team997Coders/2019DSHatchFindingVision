  /**
   * Custom exception to indicate teensy not found
   */
  public class TeensyNotFoundException extends Exception {
    public TeensyNotFoundException () {}
    public TeensyNotFoundException (String message) {
      super (message);
    }
    public TeensyNotFoundException (Throwable cause) {
      super (cause);
    }
    public TeensyNotFoundException (String message, Throwable cause) {
      super (message, cause);
    }
  }
