public abstract class CameraParameters {
  public abstract double getRangeCalibrationInInches();
  public abstract double getFOVCalibrationInInches();
  public double getFOVPixelWidth() {
    return 640;
  }
  public double getFOVPixelHeight() {
    return 480;
  }
  public double getTanTheta() {
    return getFOVCalibrationInInches() / (2 * getRangeCalibrationInInches());    
  }
}