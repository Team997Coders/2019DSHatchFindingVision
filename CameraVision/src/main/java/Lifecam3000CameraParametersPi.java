import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * For the Lifecam 5000 camera, the class will set the brightness and exposure
 * on the camera running on a Raspberry Pi running Raspbian. It assumes that 
 * v4l2-ctrl is installed on the Pi and will throw an error otherwise.
 */
public class Lifecam3000CameraParametersPi extends Lifecam3000CameraParameters implements ILuminanceControl {
  private final int port;
  private String binDir = "/usr/bin/";
  private final String host;

  public Lifecam3000CameraParametersPi(String cameraURLString) throws 
      CameraParametersException, MalformedURLException {
    // Default to port 0
    this(cameraURLString, 0);
  }

  public Lifecam3000CameraParametersPi(String cameraURLString, int port) throws 
      CameraParametersException, MalformedURLException {
    URL cameraURL = new URL(cameraURLString);
    host = cameraURL.getHost();
    // Only exposure setting is supported on localhost at the moment
    if (!host.toLowerCase().contains("localhost") && !host.contains("127.0.0.1")) {
      throw new RuntimeException("Camera exposure settings can only be set if camera is on localhost. Implement ssh mechanism for remote setting.");
    }
    this.port = port;
    // Set autoexposure to off.
    setAutoExposure(1);
    // Set exposure to trial value that seems to work reasonably well.
    setExposure(20);
    // Set brightness to trial value that seems to work reasonably well.
    setBrightness(100);
  }

  public void setAutoExposure(int autoExposure) throws CameraParametersException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command("bash", "-c", String.format("%sv4l2-ctl -d /dev/video%d -c exposure_auto=%d", binDir, port, autoExposure));
      StringBuilder output = new StringBuilder();
      output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraParametersException(String.format("Set autoexposure off command returned %d\n%s", rc, output));
      }
    } catch (IOException|InterruptedException e) {
      throw new CameraParametersException(e);
    }
  }

  public void setBrightness(int brightness) throws CameraParametersException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      // -- Linux --
      // Run a shell command
      processBuilder.command("bash", "-c", String.format("%sv4l2-ctl -d /dev/video%d -c brightness=%d", binDir, port, brightness));
      StringBuilder output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraParametersException(String.format("Set brightness command returned %d\n%s", rc, output));
      }
    } catch (IOException|InterruptedException e) {
      throw new CameraParametersException(e);
    }
  }

  public void setExposure(int exposure) throws CameraParametersException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      // -- Linux --
      // Run a shell command
      processBuilder.command("bash", "-c", String.format("%sv4l2-ctl -d /dev/video%d -c exposure_absolute=%d", binDir, port, exposure));
      StringBuilder output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraParametersException(String.format("Set exposure command returned %d\n%s", rc, output));
      }
    } catch (IOException|InterruptedException e) {
      throw new CameraParametersException(e);
    }
  }

  private int runIt(ProcessBuilder processBuilder, StringBuilder output) throws IOException, InterruptedException {
      Process process = processBuilder.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
      int exitVal = process.waitFor();
      return exitVal;
  }
}
