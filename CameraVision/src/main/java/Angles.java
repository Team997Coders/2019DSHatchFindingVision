/**
 * Value class to represent pan/tilt angles.
 */
public class Angles {
  private final int tilt;
  private final int pan;

  public Angles(int tilt, int pan) {
    this.tilt = tilt;
    this.pan = pan;
  }
  public int getPanAngle() {
    return pan;
  }
  public int getTiltAngle() {
    return tilt;
  }
}
