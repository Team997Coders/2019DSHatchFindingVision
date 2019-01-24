import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
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
		ArrayList<HatchTarget> hatchTargets= new ArrayList<HatchTarget>();
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
				} catch (HatchTarget.TargetRectanglesAngleException e) {
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