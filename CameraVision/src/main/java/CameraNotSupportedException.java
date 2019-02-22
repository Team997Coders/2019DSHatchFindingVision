/**
 * Custom exception to indicate that a camera was found
 * that it not supported.
 */
public class CameraNotSupportedException extends RuntimeException {
  public CameraNotSupportedException () {}
  public CameraNotSupportedException (String message) {
    super (message);
  }
  public CameraNotSupportedException (Throwable cause) {
    super (cause);
  }
  public CameraNotSupportedException (String message, Throwable cause) {
    super (message, cause);
  }
}
