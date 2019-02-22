import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CameraModelEvaluatorPi {
  public final String device;
  private String binDir = "/usr/bin/";

  /**
   * This class will, given a linux device name, evaluate the device
   * and if it is a supported camera, will return that camera as an
   * enumerated type.
   * 
   * @param device  Linux device to evaluate. For example "/dev/video0".
   */
  public CameraModelEvaluatorPi(String device) {
    this.device = device;
  }

  public Cameras getCamera() {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      // -- Linux --
      // Run a shell command
      processBuilder.command("bash", "-c", buildGetCameraInfoCommand(device, binDir));
      StringBuilder output = new StringBuilder();
      int rc = runIt(processBuilder, output);
      if (rc != 0) {
        throw new CameraModelEvaluatorException(String.format("Determine camera type command returned %d\n%s", rc, output.toString()));
      } else {
        if (output.toString().contains("HD-3000")) {
          return Cameras.LifeCamHD3000;
        } else if (output.toString().contains("HD-5000")) {
          return Cameras.LifeCamHD5000;
        } else {
          throw new CameraNotSupportedException(String.format("Device %s has a camera that is not supported.", device));
        }
      }
    } catch (IOException|InterruptedException e) {
      throw new CameraModelEvaluatorException(e);
    }
  }

  private String buildGetCameraInfoCommand(String device, String binDir) {
    return String.format("%sv4l2-ctl -d %s -D", binDir, device);
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

  public enum Cameras {
    LifeCamHD3000, LifeCamHD5000
  }
}