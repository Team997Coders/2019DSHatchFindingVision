import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * For the Lifecam 5000 camera, the class will set the brightness and exposure
 * on the camera running on a Raspberry Pi running Raspbian. It assumes that 
 * v4l2-ctrl is installed on the Pi and will throw an error otherwise.
 */
public class Lifecam5000CameraParametersPi extends Lifecam5000CameraParameters implements ILuminanceControl {
  private final int port;

  public Lifecam5000CameraParametersPi() throws CameraParametersException {
    // Default to port 0
    this(0);
  }

  public Lifecam5000CameraParametersPi(int port) throws CameraParametersException {
    try {
      this.port = port;
      ProcessBuilder processBuilder = new ProcessBuilder();
      // Set autofocus to off! POS Lifecam just goes autofocus bonkers.
      processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c focus_auto=0", port));
      StringBuilder output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraParametersException(String.format("Set autofocus off command returned %d\n%s", rc, output));
      }
      // Set autoexposure to off.
      processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c exposure_auto=1", port));
      output = new StringBuilder();
      rc = runIt(processBuilder, output);
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
      processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c brightness=%d", port, brightness));
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
      processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c exposure_absolute=%d", port, exposure));
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