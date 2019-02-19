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
  private String binDir = "/usr/bin/";

  public Lifecam5000CameraParametersPi() throws CameraParametersException {
    // Default to port 0
    this(0);
  }

  public Lifecam5000CameraParametersPi(int port) throws CameraParametersException {
    this.port = port;
    // Set autofocus to off! POS Lifecam just goes autofocus bonkers.
    setAutofocus(0);
    // Set focus to infinity
    setFocus(0);
    // Set autoexposure to off.
    setAutoExposure(1);
    // Set exposure to trial value that seems to work reasonably well.
    setExposure(9);
    // Set brightness to trial value that seems to work reasonably well.
    setBrightness(100);
  }

  public void setAutofocus(int autofocus) throws CameraParametersException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command("bash", "-c", String.format("%sv4l2-ctl -d /dev/video%d -c focus_auto=%d", binDir, port, autofocus));
      StringBuilder output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraParametersException(String.format("Set autofocus off command returned %d\n%s", rc, output));
      }
    } catch (IOException|InterruptedException e) {
      throw new CameraParametersException(e);
    }
  }

  public void setFocus(int focus) throws CameraParametersException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command("bash", "-c", String.format("%sv4l2-ctl -d /dev/video%d -c focus_absolute=%d", binDir, port, focus));
      StringBuilder output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraParametersException(String.format("Set focus command returned %d\n%s", rc, output));
      }
    } catch (IOException|InterruptedException e) {
      throw new CameraParametersException(e);
    }
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