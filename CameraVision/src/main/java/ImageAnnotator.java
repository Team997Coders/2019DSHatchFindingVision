import java.util.Arrays;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ImageAnnotator {
  private final HatchTargetPipelineInterpreter interpreter;
  private Mat outputImage;
  private final Scalar targetingRectangleColor;
  private final Scalar hatchTargetRectangleColor;

  public ImageAnnotator(HatchTargetPipelineInterpreter interpreter) {
    this.interpreter = interpreter;
    this.outputImage = new Mat();
    this.targetingRectangleColor = new Scalar(81, 190, 0);      // green
    this.hatchTargetRectangleColor = new Scalar(255, 51, 0);    // blue
  }

  public Mat annotate(Mat inputImage) {
    inputImage.copyTo(outputImage);
    drawTargetingRectangles();
    drawHatchTargetRectangles();
    return outputImage;
  }

  private void drawTargetingRectangles() {
    // Draw best-fit rectangles around targets
    for (RotatedRect rotatedRect: interpreter.getRectangles()) {
      drawRotatedRect(rotatedRect, targetingRectangleColor, 4);
    }        
  }

  private void drawHatchTargetRectangles() {
    for (HatchTarget hatchTarget: interpreter.getHatchTargets()) {
      drawRotatedRect(hatchTarget.targetRectangle(), hatchTargetRectangleColor, 4);
    }
  }

  private void drawRotatedRect(RotatedRect rotatedRect, Scalar color, int thickness) {
    Point[] vertices = new Point[4];
    rotatedRect.points(vertices);
    MatOfPoint points = new MatOfPoint(vertices);
    Imgproc.drawContours(outputImage, Arrays.asList(points), -1, color, thickness);
  }
}