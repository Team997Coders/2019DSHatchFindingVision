  /**
   * Custom exception to indicate that a target is not expected
   * where it should be.
   */
  public class TargetNotFoundException extends Exception {
    public TargetNotFoundException () {}
    public TargetNotFoundException (String message) {
      super (message);
    }
    public TargetNotFoundException (Throwable cause) {
      super (cause);
    }
    public TargetNotFoundException (String message, Throwable cause) {
      super (message, cause);
    }
  }
