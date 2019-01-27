import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Point;
import org.opencv.core.Point3;

/**
 * Encapsulate those characteristics specific to a hatch target, given
 * the rectangles that make it a target.
 */
public class HatchTarget {
//  private final double HATCHTARGETWIDTHININCHES = 14.5; // School mock target
  private final double HATCHTARGETWIDTHININCHES = 12; // Home mock target
  RotatedRect leftRectangle;
  RotatedRect rightRectangle;
  CameraParameters cameraParameters;

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
    // The point of the 0th vertex (which is the lowest point, also the greatest y value)
    // is the pivot point. RotatedRect angle measures, going counterclockwise, the angle formed
    // by horizontal and the right hand size of the vertex connected to the point. And oh yeah,
    // the angle gets more negative until reaching -90 degrees.
    if (leftRectangle.angle == -0 || leftRectangle.angle == -90 || rightRectangle.angle == -0 || rightRectangle.angle == -90) {
      throw new TargetRectanglesException("Rectangles are not tilted.");
    }
    // The left-hand rectangle angle should be MORE negative than the right
    // And add a comfortable offset so that we don't pick up two lefts where
    // the left-left is slightly more tilted than the right-left.
    if ((leftRectangle.angle + 30) > rightRectangle.angle) {
      throw new TargetRectanglesException("Target rectangles are not tilted inward.");
    }
    
    // The distance between centers when the camera is perpendicular should be ~11.4 inches.
    if ((widthInPx() * pxToInchesConversion(center().x)) > 15) {
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
    double width = rect.size.width > rect.size.height ? rect.size.width : rect.size.height;
    // One idea is to compare the widths of the interior rectangles...the degree of difference should tells us
    // something about the angle. We are using the hatch target width below but could use a normalized tape
    // width since we know all the tape width is 2 inches.
    // d = Tin*FOVpixel/(2*Tpixel*tanΘ)

    //old method, only worked for perpendicular cases.
    //return (HATCHTARGETWIDTHININCHES * cameraParameters.getFOVPixelWidth()) / (2 * width * cameraParameters.getTanTheta());

    return ( (((cameraParameters.getFOVPixelWidth() / 2) * pxToInchesConversion(center().x)) * cameraParameters.getRangeCalibrationInInches()) / cameraParameters.getFOVCalibrationInInches() );
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
   * Returns the width between the two target rectangle's centers.
   * @return width between target's centers. (px) 
   */
  public double widthInPx() {
    return (Math.abs(leftRectangle.center.x - rightRectangle.center.x));
  }

  /**
   * Angle formed between plane of target and line segment drawn from robot position
   * to center of target.
   * 
   * @see http://answers.opencv.org/question/56744/converting-pixel-displacement-into-other-unit/?comment=56918#comment-56918
   */
  public double thetaInDegrees() {
    // TODO!
    return 0;
  }

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