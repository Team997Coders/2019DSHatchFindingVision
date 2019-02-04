public class CommunicationErrorException extends Exception {
  public CommunicationErrorException () {}
  public CommunicationErrorException (String message) {
    super (message);
  }
  public CommunicationErrorException (Throwable cause) {
    super (cause);
  }
  public CommunicationErrorException (String message, Throwable cause) {
    super (message, cause);
  }
}
