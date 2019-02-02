public class CommunicationFailureException extends Exception {
  public CommunicationFailureException () {}
  public CommunicationFailureException (String message) {
    super (message);
  }
  public CommunicationFailureException (Throwable cause) {
    super (cause);
  }
  public CommunicationFailureException (String message, Throwable cause) {
    super (message, cause);
  }
}
