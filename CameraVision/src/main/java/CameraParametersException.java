/**
 * Custom exception to indicate that setting camera parameters
 * was unsuccessful.
 */
public class CameraParametersException extends Exception {
  public CameraParametersException () {}
  public CameraParametersException (String message) {
    super (message);
  }
  public CameraParametersException (Throwable cause) {
    super (cause);
  }
  public CameraParametersException (String message, Throwable cause) {
    super (message, cause);
  }
}
