import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageUtilities {
  private final Mat inputImage;

  public ImageUtilities(Mat inputImage) {
    this.inputImage = inputImage;
  }

  /**
   * Find the average luminance of a frame.
   * 
   * @return  The average luminance.
   */
  public float getAverageLuminosity() {
    Mat grayMat = new Mat();
    float avgLum = 0;

    Imgproc.cvtColor(inputImage, grayMat, Imgproc.COLOR_BGR2GRAY);

    long totalIntensity = 0;
    for (int i=0; i < grayMat.rows(); ++i) {
      for (int j=0; j < grayMat.cols(); ++j) {
        totalIntensity += (int) grayMat.get(i, j)[0];
      }
    }

    // Find avg lum of frame
    avgLum = ((float)totalIntensity)/(grayMat.rows() * grayMat.cols());
    return avgLum;
	}
}