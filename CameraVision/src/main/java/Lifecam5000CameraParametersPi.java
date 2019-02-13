/**
 * For the Lifecam 5000 camera, the class will set the brightness and exposure
 * on the camera running on a Raspberry Pi running Raspbian. It assumes that 
 * v4l2-ctrl is installed on the Pi and it assumes that the camera is plugged
 * into port 0.
 * TODO: Port restriction will need to be relaxed.
 */
public class Lifecam5000CameraParametersPi extends Lifecam5000CameraParameters implements ILuminanceControl {
  private final int port;

  public Lifecam5000CameraParametersPi(int port) {
    this.port = port;
    ProcessBuilder processBuilder = new ProcessBuilder();
    // Set autofocus to off! POS Lifecam just goes autofocus bonkers.
    processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c focus_auto=0", port));
    // Set autoexposure to off.
    processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c exposure_auto=1", port));
  }

  public void setBrightness(int brightness) {
    ProcessBuilder processBuilder = new ProcessBuilder();
    // -- Linux --
    // Run a shell command
    processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c brightness=%d", port, brightness));
  }

  public void setExposure(int exposure) {
    ProcessBuilder processBuilder = new ProcessBuilder();
    // -- Linux --
    // Run a shell command
    processBuilder.command("bash", "-c", String.format("v4l2-ctrl -d /dev/video%d -c exposure_absolute==%d", port, exposure));
  }
/*  

#  v4l2-ctl -d /dev/video0 -c focus_auto=0
  # Turn off auto exposure
  v4l2-ctl -d /dev/video0 -c exposure_auto=1
  # Turn down exposure
  v4l2-ctl -d /dev/video0 -c exposure_absolute=5
  # Turn up brightness
  v4l2-ctl -d /dev/video0 -c brightness=100
*/
}