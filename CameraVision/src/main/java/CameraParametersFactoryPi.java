import java.net.MalformedURLException;

/**
 * Factory class to get instances of camaera parameters for the Pi
 * platform given a port identifier.
 */
public class CameraParametersFactoryPi {
  /**
   * 
   */
  public static CameraParameters getCameraParameters(String cameraURL, String port) throws CameraParametersException, MalformedURLException {
    CameraModelEvaluatorPi evaluator = new CameraModelEvaluatorPi(String.format("/dev/video%s", port));
    CameraModelEvaluatorPi.Cameras camera = evaluator.getCamera();
    switch (camera) {
      case LifeCamHD3000:
        return new Lifecam3000CameraParametersPi(cameraURL, port);
      case LifeCamHD5000:
        return new Lifecam5000CameraParametersPi(cameraURL, port);
      default:
        throw new CameraNotSupportedException(String.format("Factory does not support creating camera found for device /dev/video%s.", port));
    }
  }
}