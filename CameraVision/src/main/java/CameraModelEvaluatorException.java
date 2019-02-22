/**
 * Custom exception to indicate that an error occurred
 * calling camera evaluator commands for the platform.
 */
public class CameraModelEvaluatorException extends RuntimeException {
  public CameraModelEvaluatorException () {}
  public CameraModelEvaluatorException (String message) {
    super (message);
  }
  public CameraModelEvaluatorException (Throwable cause) {
    super (cause);
  }
  public CameraModelEvaluatorException (String message, Throwable cause) {
    super (message, cause);
  }
}
