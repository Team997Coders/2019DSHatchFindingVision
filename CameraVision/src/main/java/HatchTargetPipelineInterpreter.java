import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
* Interpret the result of the hatch target pipeline.  This abstracts out the logic
* from the pipeline class.
*
* @author Chuck Benedict, Mentor, Team 997
*/
public class HatchTargetPipelineInterpreter {
	// Processed pipeline that we will do the interpretation against
	private IHatchTargetPipeline pipeline;
	private CameraParameters cameraParameters;

	/**
	 * A comparator class for sorting rotated rectangles on the
	 * x axis left to right.
	 */
	private class SortByXAscending implements Comparator<RotatedRect> {

    public int compare(RotatedRect a, RotatedRect b) {
			return (int)Math.round(a.center.x - b.center.x);
		}
	}

	/**
	* Constructor taking a processed pipeline
	*
	* @param pipeline	A processed pipeline that returns filtered contour results
	*/
	public HatchTargetPipelineInterpreter(IHatchTargetPipeline pipeline, CameraParameters cameraParameters) {
		if (pipeline == null)
		{
			throw new IllegalArgumentException("Pipline cannot be null.");
		}
		if (cameraParameters == null)
		{
			throw new IllegalArgumentException("Camera parameters cannot be null.");
		}
		this.pipeline = pipeline;
		this.cameraParameters = cameraParameters;
	}

	/**
	 * Set the camera parameters that will be used to compute distances
	 * and angles to targets.
	 * 
	 * @param cameraParameters Calibrated camera parameters
	 */
	public void setCameraParameters(CameraParameters cameraParameters) {
		if (cameraParameters == null) {
			throw new IllegalArgumentException("cameraParameters cannot be null.");
		}
		this.cameraParameters = cameraParameters;
	}

	/**
	 * Process filtered contours and return an array of best fit rectangles
	 * for each contour found.
	 * 
	 * @return	An array list of RotatedRect
	 */
	protected ArrayList<RotatedRect> getRectangles() {
		ArrayList<RotatedRect> listOfRectangles = new ArrayList<RotatedRect>();
		for (MatOfPoint contour: pipeline.filterContoursOutput()) {
			RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
			listOfRectangles.add(rotatedRect);
		}
		return listOfRectangles;
	}

	/**
	 * Process filtered contours and return an array of best fit rectangles
	 * sorted in left to right sort order as seen on the x axis.
	 * 
	 * @return	An array list of RotatedRect
	 */
	protected ArrayList<RotatedRect> getRectanglesByXAscending() {
		ArrayList<RotatedRect> sortedRectangles = getRectangles();
		Collections.sort(sortedRectangles, new SortByXAscending());
		return sortedRectangles;
	}

	/**
	 * Process ordered rectangles looking for rotated rectangles
	 * tilted in the appropriate direction denoting a target.
	 * 
	 * @return	An array list of hatch targets.
	 */
	public ArrayList<HatchTarget> getHatchTargets() {
		ArrayList<HatchTarget> hatchTargets = new ArrayList<HatchTarget>();
		ArrayList<RotatedRect> sortedRectangles = getRectanglesByXAscending();

		// Must start with at least 2 rectangles
		if (sortedRectangles.size() >= 2) {
			// Get an iterator to move through the array with
			Iterator<RotatedRect> iterator = sortedRectangles.iterator();
			// Pump iterator for a left-hand rectangle
			RotatedRect leftRectangle = iterator.next();
			// Declare a right-hand rectangle
			RotatedRect rightRectangle = null;
			// Process while we have another rectangle left in the array
			while(iterator.hasNext()) {
				// Pump iterator for right-hand rectangle
				rightRectangle = iterator.next();
				try {
					// Try to create a hatch target
					HatchTarget hatchTarget = new HatchTarget(leftRectangle, rightRectangle, cameraParameters);
					// If we have an instantiated hatch target, add it to the list
					hatchTargets.add(hatchTarget);
					// Move to the next target to examine
					if (iterator.hasNext()) {
						leftRectangle = iterator.next();
					}
				} catch (HatchTarget.TargetRectanglesException e) {
					// It did not work, so move over one rectangle so we can try again
					leftRectangle = rightRectangle;
					// Start again from the top of loop
					continue;
				}
			}
		}
		return hatchTargets;
	}

  /**
   * Returns a number from -1 to 1 corresponding to the position, from left to right, of the target's
   * horizontal position relative to the center of the FOV.
   * 
   * @param targetIdentifyingPoint  A point inside an identified target.
   * @return                        The relative position from center represented as -1 to 1, x and y, 
   *                                with (-1, -1) representing top left.
   */
  public Point getNormalizedTargetPositionFromCenter(Point targetIdentifyingPoint) throws TargetNotFoundException {
    // Find the hatch target containing the identifying point
    HatchTarget hatchTarget = getHatchTargetFromPoint(targetIdentifyingPoint);
    Point center = hatchTarget.center();
    Point point = new Point();
    // Find center of FOV
    double oneHalfFOVPixelWidth = cameraParameters.getFOVPixelWidth() / 2;
    double oneHalfFOVPixelHeight = cameraParameters.getFOVPixelHeight() / 2;
    // Normalize position
    if (center.x > oneHalfFOVPixelWidth) {
      point.x = (center.x - (cameraParameters.getFOVPixelWidth() / 2)) / oneHalfFOVPixelWidth;
      point.y = (center.y - (cameraParameters.getFOVPixelHeight() / 2)) / oneHalfFOVPixelHeight;
    } else {
      point.x = (((cameraParameters.getFOVPixelWidth() / 2) - center.x) / oneHalfFOVPixelWidth) * -1;
      point.y = (((cameraParameters.getFOVPixelHeight() / 2) - center.y ) / oneHalfFOVPixelHeight) * -1;
    }
    return point;
  }

	public HatchTarget getHatchTargetFromPoint(Point point) throws TargetNotFoundException {
    // Iterate over hatch targets
    for (HatchTarget hatchTarget: getHatchTargets()) {
      // Upsize the rotated rectangle by 30% to add some slop for slewing
      double upsizeFactor = 1.3;
      RotatedRect originalRectangle = hatchTarget.targetRectangle();

      // Upsize the rectangle
      RotatedRect upsizedRectangle = new RotatedRect(
        new Point(originalRectangle.center.x, originalRectangle.center.y),
        new Size(originalRectangle.size.height * upsizeFactor, originalRectangle.size.width * upsizeFactor), 
        originalRectangle.angle);

      // Return it if the point being sent in is contained within
      if (upsizedRectangle.boundingRect().contains(point)) {
        return hatchTarget;
      }
    }
    // Otherwise bail and let consumer know
    throw new TargetNotFoundException("No hatch target found containing point.");
	}

	/**
	 * Return the center points for each found hatch target.
	 * 
	 * @return	As array list of points.
	 */
	public ArrayList<Point> getHatchTargetCenters() {
		ArrayList<Point> hatchTargetCenters = new ArrayList<Point>();

		for(HatchTarget hatchTarget : getHatchTargets()) {
			hatchTargetCenters.add(hatchTarget.center());
		}
		return hatchTargetCenters;
	}

	/**
	 * Return up to the two closest targets to the center of the
	 * field of view.
	 * 
	 * @return	As array list of points ordered left to right.
	 */
	public ArrayList<Point> getHatchTargetCentersClosestToFOVCenter() {
		ArrayList<Point> hatchTargetCenters = new ArrayList<Point>();
		ArrayList<HatchTarget> hatchTargets = getHatchTargets();
		long targetCount = targetCount();
		double centerX = cameraParameters.getFOVPixelWidth() / 2;
		HatchTarget closestLeft;
		HatchTarget closestRight;
		HatchTarget currentTarget;
		HatchTarget priorTarget;

		if (targetCount == 0) {
			// do nothing
		} else if (targetCount == 1) {
			// Add the same one twice so that either left or right picks it up
			hatchTargetCenters.add(hatchTargets.get(0).center());
			hatchTargetCenters.add(hatchTargets.get(0).center());
		} else {
			// Processing left to right...
			closestLeft = hatchTargets.get(0);
			closestRight = hatchTargets.get(1);
			double leastDistanceFromCenter = Math.abs(closestLeft.center().x - centerX) + Math.abs(closestRight.center().x - centerX);
			for(int index = 2; index < targetCount; index++) {
				currentTarget = hatchTargets.get(index);
				priorTarget = hatchTargets.get(index - 1);
				double currentDistanceFromCenter = Math.abs(currentTarget.center().x - centerX) + Math.abs(priorTarget.center().x - centerX);
				if (currentDistanceFromCenter < leastDistanceFromCenter) {
					closestLeft = priorTarget;
					closestRight = currentTarget;
					leastDistanceFromCenter = currentDistanceFromCenter;
				}
			}
			hatchTargetCenters.add(closestLeft.center());
			hatchTargetCenters.add(closestRight.center());
		}

		return hatchTargetCenters;
	}

	/**
	 * Did we find at least one target on a processed frame?
	 * 
	 * @return True if at least one target was found
	 */
	public boolean targetsFound() {
		return !getHatchTargets().isEmpty();
	}

	/**
	 * Get the count of the number of targets found on a processed frame.
	 *  
	 * @return The count of the number of targets found
	 */
	public long targetCount() {
		return getHatchTargets().size();
	}
}