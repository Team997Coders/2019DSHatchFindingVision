public interface ILuminanceControl {
  void setExposure(int exposure) throws CameraParametersException;
  void setBrightness(int brightness) throws CameraParametersException;
}