public abstract class CameraParameters {
  public abstract double getRangeCalibrationInInches();
  public abstract double getFOVWidthCalibrationInInches();
  public abstract double getFOVHeightCalibrationInInches();

  public double getFOVCalibrationInchArea() {
    return getFOVWidthCalibrationInInches() * getFOVHeightCalibrationInInches();
  }

  public double getFOVPixelWidth() {
    return 640;
  }
  
  public double getFOVPixelHeight() {
    return 480;
  }

  public double getFOVPixelDiagonal() {
    return Math.sqrt(Math.pow(getFOVPixelHeight(), 2) + Math.pow(getFOVPixelWidth(), 2));
  }

  public double getFOVPixelArea() {
    return getFOVPixelHeight() * getFOVPixelWidth();
  }
  
  public double getWidthTanTheta() {
    return (getFOVWidthCalibrationInInches() / 2) / getRangeCalibrationInInches();    
  }

  public double getHeightTanTheta() {
    return (getFOVHeightCalibrationInInches() / 2) / getRangeCalibrationInInches();    
  }

  public double getDiagonalTanTheta() {
    return Math.sqrt(Math.pow(getWidthTanTheta(), 2) + Math.pow(getHeightTanTheta(), 2));
  }

  public double getRadiansPerPixel() {
    return getDiagonalTanTheta() / getFOVPixelDiagonal();
  }
}