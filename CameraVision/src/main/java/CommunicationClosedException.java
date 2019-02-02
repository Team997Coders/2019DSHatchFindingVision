public class CommunicationClosedException extends Exception {
  public CommunicationClosedException () {}
  public CommunicationClosedException (String message) {
    super (message);
  }
  public CommunicationClosedException (Throwable cause) {
    super (cause);
  }
  public CommunicationClosedException (String message, Throwable cause) {
    super (message, cause);
  }
}
