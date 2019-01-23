import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Point;

/**
 * Encapsulate those characteristics specific to a hatch target, given
 * the rectangles that make it a target.
 */
public class HatchTarget {
  double TARGETTAPEWIDTHININCHES = 2.0;
  RotatedRect leftRectangle;
  RotatedRect rightRectangle;

  /**
   * Custom exception to indicate an invalid set of rotation
   * angles for the containing rectangles within a hatch target.
   */
  public class TargetRectanglesAngleException extends Exception {
    public TargetRectanglesAngleException () {}
    public TargetRectanglesAngleException (String message) {
      super (message);
    }
    public TargetRectanglesAngleException (Throwable cause) {
      super (cause);
    }
    public TargetRectanglesAngleException (String message, Throwable cause) {
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
      RotatedRect rightRectangle) throws TargetRectanglesAngleException {
    this.leftRectangle = leftRectangle;
    this.rightRectangle = rightRectangle;
    validateTargetRectanglesRotationAngles();
  }

  /**
   * Check that the rectangles are pointing at each other on top
   * of the target. Throw an exception otherwise.
   * To learn how opencv determines this magic:
   * 
   * @see https://namkeenman.wordpress.com/2015/12/18/open-cv-determine-angle-of-rotatedrect-minarearect/

   * @throws TargetRectanglesAngleException   Exception thrown if rectangle angles are not valid.
   */
  private void validateTargetRectanglesRotationAngles() throws TargetRectanglesAngleException {
    // The point of the 0th vertex (which is the lowest point, also the greatest y value)
    // is the pivot point. RotatedRect angle measures, going counterclockwise, the angle formed
    // by horizontal and the right hand size of the vertex connected to the point. And oh yeah,
    // the angle gets more negative until reaching -90 degrees.
    if (leftRectangle.angle == -0 || leftRectangle.angle == -90 || rightRectangle.angle == -0 || rightRectangle.angle == -90) {
      throw new TargetRectanglesAngleException("Rectangles are not tilted.");
    }
    // The left-hand rectangle angle should be MORE negative than the right
    // And add a comfortable offset so that we don't pick up two lefts where
    // the left-left is slightly more tilted than the right-left.
    if ((leftRectangle.angle + 30) > rightRectangle.angle) {
      throw new TargetRectanglesAngleException("Target rectangles are not tilted inward.");
    }

		//TODO: The edge case where two targets each have their inner rectangles
		// obscured will still cause the outer rectangles to be mistakenly
		// identified as a target. Some distance threshold should be checked.
    // TODO: throw another error here
  }

  public Point center() {
    return new Point((leftRectangle.center.x + rightRectangle.center.x) * 0.5, (leftRectangle.center.y + rightRectangle.center.y) * 0.5);
  }

  /**
   * Range to target in inches.
   */
  public double rangeInInches() {
    return 0;
  }

  /**
   * Angle formed between wall of target and line segment drawn from robot position
   * to target.
   */
  public double thetaInDegrees() {
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