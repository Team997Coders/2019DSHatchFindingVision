import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.core.Point;

/**
 * Encapsulate those characteristics specific to a hatch target, given
 * the rectangles that make it a target.
 */
public class HatchTarget {
//  private final double HATCHTARGETWIDTHININCHES = 14.5; // School mock target
  private final double HATCHTARGETWIDTHININCHES = 12; // Home mock target
  private final double TAPEHEIGHTININCHES = 5.5; // Per rule book
  private final double TAPEWIDTHININCHES = 2.0; // Per rule book
  private final double TAPEAREAININCHES = TAPEHEIGHTININCHES * TAPEWIDTHININCHES;
  private RotatedRect leftRectangle;
  private RotatedRect rightRectangle;
  private CameraParameters cameraParameters;

  /**
   * Custom exception to indicate an invalid set of rotation
   * angles for the containing rectangles within a hatch target.
   */
  public class TargetRectanglesException extends Exception {
    public TargetRectanglesException () {}
    public TargetRectanglesException (String message) {
      super (message);
    }
    public TargetRectanglesException (Throwable cause) {
      super (cause);
    }
    public TargetRectanglesException (String message, Throwable cause) {
      super (message, cause);
    }
  }


  /**
   * Construct a hatch target given the interior targeting rectangles.
   * 
   * @param leftRectangle                     The left hand side RotatedRect.
   * @param rightRectangle                    The right hand side RotatedRect.
   * @throws TargetRectanglesAngleException   Exception thrown if rectangle angles are not valid.
   */
  public HatchTarget(RotatedRect leftRectangle, 
      RotatedRect rightRectangle,
      CameraParameters cameraParameters) throws TargetRectanglesException {
      
    this.leftRectangle = leftRectangle;
    this.rightRectangle = rightRectangle;
    this.cameraParameters = cameraParameters;
    validateTargetRectangles();
  }

  /**
   * Check that the rectangles are pointing at each other on top
   * of the target, and if they are close enough to be a valid target. Throw an exception otherwise.
   * To learn how opencv determines this magic:
   * 
   * @see https://namkeenman.wordpress.com/2015/12/18/open-cv-determine-angle-of-rotatedrect-minarearect/

   * @throws TargetRectanglesException   Exception thrown if rectangle angles or width is not valid.
   */
  private void validateTargetRectangles() throws TargetRectanglesException {
    //System.out.println("  leftWidth: " + leftRectangle.size.width + ", leftHeight: " + leftRectangle.size.height);
    //System.out.println("  rightWidth: " + rightRectangle.size.width + ", rightHeight: " + rightRectangle.size.height);
    //System.out.println("  leftAngle: " + leftRectangle.angle + ", rightAngle: " + rightRectangle.angle);
    
    // The rectangles shouldn't be tilted more than around 20deg.
    if ((pxToInchesConversion(center().x) * Math.abs(leftRectangle.center.y - rightRectangle.center.y)) > 2) {
      System.out.println("Target is tilted by too much.");
      throw new TargetRectanglesException("Target rectangles are tilted by too much.");
    }

    // The point of the 0th vertex (which is the lowest point, also the greatest y value)
    // is the pivot point. RotatedRect angle measures, going counterclockwise, the angle formed
    // by horizontal and the right hand size of the vertex connected to the point. And oh yeah,
    // the angle gets more negative until reaching -90 degrees.
    if ((leftRectangle.angle > -55 || leftRectangle.angle == -90) || rightRectangle.angle < -25 || rightRectangle.angle == -0) {
      System.out.println("Rectangles are not tilted.");
      throw new TargetRectanglesException("Rectangles are not tilted.");
    }

    // The rectangles should have the shorter sides on top and bottom.
    if ((normalize(leftRectangle).width > normalize(leftRectangle).height) || (normalize(rightRectangle).width > normalize(rightRectangle).height)) {
      System.out.println("Rectangles are horizontal.");
      throw new TargetRectanglesException("Rectangles are horizontal.");
    }

    // The left-hand rectangle angle should be MORE negative than the right
    // And add a comfortable offset so that we don't pick up two lefts where
    // the left-left is slightly more tilted than the right-left.
    if ((leftRectangle.angle + 30) > rightRectangle.angle) {
      System.out.println("Target rectangles are not tilted inward.");
      throw new TargetRectanglesException("Target rectangles are not tilted inward.");
    }
    
    // The distance between centers when the camera is perpendicular should be ~11.4 inches.
    if ((widthInPx() * pxToInchesConversion(center().x)) > 15) {
      System.out.println("Target rectangles are too far apart.");
      throw new TargetRectanglesException("Target rectangles are too far apart.");
    }

  }

  public Point center() {
    return new Point((leftRectangle.center.x + rightRectangle.center.x) * 0.5, (leftRectangle.center.y + rightRectangle.center.y) * 0.5);
  }

  /**
   * Range to target in inches.
   * 
   * @see https://wpilib.screenstepslive.com/s/3120/m/8731/l/90361-identifying-and-processing-the-targets
   */
  public double rangeInInches() {
    RotatedRect rect = targetRectangle();
    // Get length and width for both targeting rectangles
    double size = leftRectangle.size.height + leftRectangle.size.width + rightRectangle.size.height + rightRectangle.size.width;

    // Adjust to compensate for image not in center of frame horizontally
    double percentageOffCenterHorizontal = (Math.abs((0.5 * cameraParameters.getFOVPixelWidth()) - rect.center.x)) / (0.5 * cameraParameters.getFOVPixelWidth());
    double deltaSize = (size * percentageOffCenterHorizontal * (Math.atan(cameraParameters.getWidthTanTheta()) / (2*3.14159)));

    // Adjust to compensate for image not in center of frame vertically
    double percentageOffCenterVertical = (Math.abs((0.5 * cameraParameters.getFOVPixelHeight()) - rect.center.y)) / (0.5 * cameraParameters.getFOVPixelHeight());
    deltaSize = deltaSize + (size * percentageOffCenterVertical * (Math.atan(cameraParameters.getHeightTanTheta()) / (2*3.14159)));

    // Adjust for aspect angle
    deltaSize = deltaSize + (size * (aspectAngleInRadians() / (2*3.14159)));
    
    // d = Tin*FOVpixel/(2*Tpixel*tanΘ)

    return ((TAPEWIDTHININCHES + TAPEHEIGHTININCHES) * 2 * cameraParameters.getFOVPixelWidth()) / (2 * (size - deltaSize) * cameraParameters.getWidthTanTheta());

    //return ( (((cameraParameters.getFOVPixelWidth() / 2) * pxToInchesConversion(center().x)) * cameraParameters.getRangeCalibrationInInches()) / cameraParameters.getFOVCalibrationInInches() );
  }

  public double aspectAngleInRadians() {
    double pixelDifference = Math.abs(leftRectangle.size.area() - rightRectangle.size.area());
    return pixelDifference * (Math.atan(cameraParameters.getDiagonalTanTheta()) / cameraParameters.getFOVPixelDiagonal());
  }

  public double getLeftAndRightRectangleAreaInPixels() {
    return leftRectangle.size.area() + rightRectangle.size.area();
  }

  public double getTapeToFOVPixelAreaRatio() {
    return getLeftAndRightRectangleAreaInPixels() / cameraParameters.getFOVPixelArea();
  }

  public double getTapeToCalibratedFOVInchAreaRatio() {
    return (TAPEAREAININCHES * 2) / cameraParameters.getFOVCalibrationInchArea();
  }

  public double getRangeInchesPerSquarePixel() {
    return cameraParameters.getRangeCalibrationInInches() / 1836.526788;
//    return cameraParameters.getRangeCalibrationInInches() / (getTapeToCalibratedFOVInchAreaRatio() * cameraParameters.getFOVPixelArea());
  }

  /**
   * Returns the width of an area in inches, based on the width in pixels and a center point.
   * Should work for both perpendicular and angled vison.
   * @param centerPos Position of target area. (px)
   * @return Conversion for px to inches at target area. Multiply by a pixel distance. 
   */
  public double pxToInchesConversion(double centerPos) {
    //return (2 / leftRectangle.size.width); This method only works for a perpendicular view

    //centerPos should be in the center of your thing you want to measure, so that the difference in width
    //due to view averages out.
    double pxIn2Inches = ( (((rightRectangle.size.width - leftRectangle.size.width) / (widthInPx())) * (centerPos - leftRectangle.center.x)) + leftRectangle.size.width);
    return (2 / pxIn2Inches);
  }

  /**
   * Returns the width of an area in inches, based on the width in pixels and a center point.
   * Should work for both perpendicular and angled vison.
   * Use this constructor version if you have two endpoints instead of a center.
   * These parameters don't actually care which bound you put in first, since it's just an average.
   * @param leftX Left bound.
   * @param rightX Right bound.
   */
  public double pxToInchesConversion(double leftX, double rightX) {
    return pxToInchesConversion((leftX + rightX) / 2);
  }

  /**
   * Returns the width between the two target rectangle's centers.
   * @return width between target's centers. (px) 
   */
  public double widthInPx() {
    return (Math.abs(leftRectangle.center.x - rightRectangle.center.x));
  }

  /**
   * Angle formed between plane of target and line segment drawn from robot position
   * to center of target. When theta = 0, the robot is perpendicular to the wall.
   * 
   * Finds the point at which the range between the camera and the 'wall' made by the targets is
   * effectively 0, and uses the angle between that and the range to the center of the seen wall.=
   * 
   * @see http://answers.opencv.org/question/56744/converting-pixel-displacement-into-other-unit/?comment=56918#comment-56918
   */
  /*
  public double thetaInDegrees() {
    if ((rightRectangle.size.width - leftRectangle.size.width) == 0) {
      return 0;
    } else if ((rightRectangle.center.x - leftRectangle.center.x) == 0) {
      return 90;
    } else {
      double c = ( ( -1 * ((leftRectangle.size.width) / ((rightRectangle.size.width - leftRectangle.size.width) / (rightRectangle.center.x - leftRectangle.center.x)))) + leftRectangle.size.width);
      //c is the point at which the 'wall' we're looking at meets the line perpendicular to the range. 
      //It's in pixels though, so we need to convert it still.
      return Math.atan( ((center().x - c) * pxToInchesConversion(c, center().x)) / (rangeInInches()) );
    }
  }
*/

  /**
   * Get an array of points representing the verticies
   * of the left rectangle of the hatch target.
   * 
   * @return  An array of points.
   */
  private Point[] getLeftRectangleVerticies() {
    Point[] vertices = new Point[4];
    leftRectangle.points(vertices);
    return vertices;
  }

  /**
   * Get an array of points representing the verticies
   * of the right rectangle of the hatch target.
   * 
   * @return  An array of points.
   */
  private Point[] getRightRectangleVerticies() {
    Point[] vertices = new Point[4];
    rightRectangle.points(vertices);
    return vertices;
  }

  private double getAverageTapePixelHeight() {
    double rightRectangleHeight = rightRectangle.size.width > rightRectangle.size.height ? rightRectangle.size.width : rightRectangle.size.height;
    double leftRectangleHeight = leftRectangle.size.width > leftRectangle.size.height ? leftRectangle.size.width : leftRectangle.size.height;
    return (rightRectangleHeight + leftRectangleHeight) / 2;
  }

  private double getAverageTapePixelWidth() {
    double rightRectangleWidth = rightRectangle.size.width < rightRectangle.size.height ? rightRectangle.size.width : rightRectangle.size.height;
    double leftRectangleWidth = leftRectangle.size.width < leftRectangle.size.height ? leftRectangle.size.width : leftRectangle.size.height;
    return (rightRectangleWidth + leftRectangleWidth) / 2;
  }

  private RotatedRect getRectangleClosestToFOVCenter() {
    return Math.abs(cameraParameters.getFOVPixelWidth() - leftRectangle.center.x) < Math.abs(cameraParameters.getFOVPixelWidth() - rightRectangle.center.x) 
      ? leftRectangle : rightRectangle;
  }

  private double getTotalTapePixelWidth() {
    double rightRectangleWidth = rightRectangle.size.width < rightRectangle.size.height ? rightRectangle.size.width : rightRectangle.size.height;
    double leftRectangleWidth = leftRectangle.size.width < leftRectangle.size.height ? leftRectangle.size.width : leftRectangle.size.height;
    return leftRectangleWidth + rightRectangleWidth;
  }

  /**
   * Height is always measured from the lowest point, point 0, to the first point in a clockwise direction.
   * If the rectangle's angle is smaller than -45, the height will be along the bottom.
   * @param rect Rectangle to normalize.
   * @return A size object with the correct heights.
   */
  public Size normalize(RotatedRect rect) {
    if(rect.angle < -45) {
      return new Size(rect.size.height, rect.size.width);
    } else {
      return rect.size;
    }
  }

  /**
   * Concatenate the contents of like-typed arrays.
   * 
   * @param first   The first array.
   * @param second  The second array.
   * @return        The concatenated array.
   */
  private static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  /**
   * Get the rotated rectangle formed by the outer verticies of each
   * containing rectangle.
   * 
   * @return  The best fit rotated rectangle.
   */
  public RotatedRect targetRectangle() {
    return Imgproc.minAreaRect(new MatOfPoint2f(concat(getLeftRectangleVerticies(), getRightRectangleVerticies())));
  }
}