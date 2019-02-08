import java.util.Arrays;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Take a raw image from the camera and use the image
 * pipeline interpreter to mark up the processed image
 * for viewing by driver station.
 */
public class ImageAnnotator {
  private final HatchTargetPipelineInterpreter interpreter;
  private Mat outputImage;
  private final Scalar targetingRectangleColor;
  private final Scalar hatchTargetRectangleColor;
  private final Scalar hatchTargetSlewingColor;
  private final Scalar textColor;

  /**
   * Construct an annotator with an instantiated interpreter.
   * 
   * @param interpreter An interpreter with a processed pipeline.
   */
  public ImageAnnotator(HatchTargetPipelineInterpreter interpreter) {
    if (interpreter == null) {
      throw new IllegalArgumentException("Interpreter cannot be null");
    }
    this.interpreter = interpreter;
    this.outputImage = new Mat();
    this.targetingRectangleColor = new Scalar(81, 190, 0);      // green
    this.hatchTargetRectangleColor = new Scalar(255, 51, 0);    // blue
    this.hatchTargetSlewingColor = new Scalar(0, 51, 255);      // red
    this.textColor = new Scalar(2,254,255);                     // yellow
  }

  public void beginAnnotation(Mat inputImage) {
    inputImage.copyTo(outputImage);
  }

  public Mat getCompletedAnnotation() {
    return outputImage;
  }

  /**
   * Draw rectangles for all targeting tape found.
   */
  public void drawTargetingRectangles() {
    // Draw best-fit rectangles around targets
    for (RotatedRect rotatedRect: interpreter.getRectangles()) {
      drawRotatedRect(rotatedRect, targetingRectangleColor, 4);
    }        
  }

  /**
   * Draw rectangles for identified hatch targets.
   */
  public void drawHatchTargetRectangles() {
    for (HatchTarget hatchTarget: interpreter.getHatchTargets()) {
      drawRotatedRect(hatchTarget.targetRectangle(), hatchTargetRectangleColor, 4);
    }
  }

  public void printWidthOfRectangles() {
    for (RotatedRect rotatedRect: interpreter.getRectangles()) {
      Point[] vertices = new Point[4];
      rotatedRect.points(vertices);
      Point textStart = vertices[0];
      textStart.y += 10;
      long width = Math.round((rotatedRect.size.width < rotatedRect.size.height ? rotatedRect.size.width : rotatedRect.size.height) * 100);
      double roundedWidth = ((double)width)/100;
      Imgproc.putText(outputImage, Double.toString(roundedWidth), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    }
  }

  /*public void printDistanceToHatchTargetInInches(double horizontalViewAngleInDegrees) {
    for (HatchTarget hatchTarget: interpreter.getHatchTargets()) {
      Point[] vertices = new Point[4];
      RotatedRect rotatedRect = hatchTarget.targetRectangle();
      rotatedRect.points(vertices);
      Point textStart = vertices[0];
      textStart.y += 10;
      long distance = Math.round(hatchTarget.rangeInInches(horizontalViewAngleInDegrees) * 10);
      double roundedDistance = ((double)distance)/10;
      Imgproc.putText(outputImage, "d: " + Double.toString(roundedDistance), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    }
  }*/

  private Point getBottomLeftPointFromRotatedRect(RotatedRect rotatedRect) {
    Point[] vertices = new Point[4];
    rotatedRect.points(vertices);
    double x = 10000;
    double y = 0;
    // Get the smallest x and the greatest y.
    for (Point point : vertices) {
      x = point.x < x ? point.x : x;
      y = point.y > y ? point.y : y;
    }
    return new Point(x, y);
  }

  public void printTargetInfo(double cameraAngleInDegrees) {
    for (HatchTarget hatchTarget: interpreter.getHatchTargets()) {
      printTargetInfo(hatchTarget, cameraAngleInDegrees);
    }
  }

  public void printTargetInfo(HatchTarget hatchTarget, double cameraAngleInDegrees) {
    RotatedRect rotatedRect = hatchTarget.targetRectangle();
    Point textStart = getBottomLeftPointFromRotatedRect(rotatedRect);

    textStart.y += 10;
    long distance = Math.round(hatchTarget.rangeInInches() * 10);
    double roundedDistance = ((double)distance)/10;
    long angleFromTarget = Math.round(Math.toDegrees(hatchTarget.aspectAngleInRadians()) * 10);
    double roundedAngle = ((double)angleFromTarget / 10);
    long cameraAngleInDegreesTimes10 = Math.round(cameraAngleInDegrees * 10);
    double cameraAngleInDegreesRounded = ((double)cameraAngleInDegreesTimes10 /10);
    Imgproc.putText(outputImage, "distance: " + Double.toString(roundedDistance), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    textStart.y += 15;
    Imgproc.putText(outputImage, "angleFromTgt: " + Double.toString(roundedAngle), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    textStart.y += 15;
    Imgproc.putText(outputImage, "cameraAngle: " + Double.toString(cameraAngleInDegreesRounded), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
  }

  public void drawSlewingRectangle(Point slewPoint) throws TargetNotFoundException {
    HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
    drawRotatedRect(hatchTarget.targetRectangle(), hatchTargetSlewingColor, 4);
    Point textStart = hatchTarget.center();
    textStart.x -= 30;
    Imgproc.putText(outputImage, "A=cancel", textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
  }

  public void drawLockedRectangle(Point slewPoint) throws TargetNotFoundException {
    HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
    drawRotatedRect(hatchTarget.targetRectangle(), hatchTargetSlewingColor, 4);
    Point textStart = hatchTarget.center();
    textStart.x -= 90;
    textStart.y -= 30;
    Imgproc.putText(outputImage, "A=cancel; B=drive to target", textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    textStart = hatchTarget.center();
    textStart.x -= 30;
    textStart.y += 5;
    Imgproc.putText(outputImage, "LOCKED!", textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
  }

  public void drawDrivingRectangle(Point slewPoint) throws TargetNotFoundException {
    HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
    drawRotatedRect(hatchTarget.targetRectangle(), hatchTargetSlewingColor, 4);
    Point textStart = hatchTarget.center();
    textStart.x -= 150;
    textStart.y -= 30;
    Imgproc.putText(outputImage, "A=cancel; B=stop driving, remain locked", textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    textStart = hatchTarget.center();
    textStart.x -= 35;
    textStart.y += 5;
    Imgproc.putText(outputImage, "DRIVING!", textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
  }

  public void drawCalibrationInformation() {
    Size size = outputImage.size();
    ImageUtilities imageUtilities = new ImageUtilities(outputImage);
    float luminosity = imageUtilities.getAverageLuminosity();
    Point textStart = new Point((size.width/2) - 100, size.height/2);
    Imgproc.putText(outputImage, 
      String.format("A=cancel; Luminosity=%.4f", luminosity), 
      textStart, 
      Core.FONT_HERSHEY_COMPLEX_SMALL, 
      .75, 
      textColor);
  }

  public void printTargetIdentifiers(Map<String, Point> identifierToPointMap) {
    for (Map.Entry<String, Point> entry : identifierToPointMap.entrySet()) {
      Imgproc.putText(outputImage, entry.getKey(), entry.getValue(), Core.FONT_HERSHEY_COMPLEX_SMALL, .75, textColor);
    }
  }

  /**
   * Helper routine to draw rotated rectangles.
   * 
   * @param rotatedRect The rectangle to draw.
   * @param color       The color of the rectangle.
   * @param thickness   The thickness of the rectangle.
   */
  private void drawRotatedRect(RotatedRect rotatedRect, Scalar color, int thickness) {
    Point[] vertices = new Point[4];
    rotatedRect.points(vertices);
    MatOfPoint points = new MatOfPoint(vertices);
    Imgproc.drawContours(outputImage, Arrays.asList(points), -1, color, thickness);
  }
}