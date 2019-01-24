import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

/**
 * Define a generic interface that all pipelines should implement
 * for this project. We are getting filtered countours in order to
 * find rectangles.
 */
public interface IHatchTargetPipeline {
	public void process(Mat source0);
	public ArrayList<MatOfPoint> filterContoursOutput();
}