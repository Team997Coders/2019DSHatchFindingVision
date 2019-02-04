import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.*;
import org.mockito.Mockito.*;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
public class TestImg8UnitTest {
    /**
     * Tests to see if the distance returned is within an inch of the correct distance between hatches.
     */
    Mat image;
    IHatchTargetPipeline pipeline;
    CameraParameters cameraParameters;
    //HatchTarget hatchTarget;
    ArrayList<HatchTarget> hatchTargets;
    HatchTargetPipelineInterpreter interpreter;

    static {
		System.loadLibrary("opencv_java310");
	}

    @Test
    public void whatAngleDoesItReturn() throws IOException {



        cameraParameters = new Lifecam5000CameraParameters();
        image = Imgcodecs.imread((new File(".\\src\\test\\resource\\test_cases\\1.jpg")).getCanonicalPath());
        //image = Imgcodecs.imread((new File(".\\src\\test\\resource\\test_cases\\2.jpg")).getCanonicalPath());

        pipeline = new Lifecam5000HatchTargetPipeline();
        pipeline.process(image);

        interpreter = new HatchTargetPipelineInterpreter(pipeline, cameraParameters);
        hatchTargets = interpreter.getHatchTargets();

        System.out.println("Found " + hatchTargets.size() + " targets.");

        for(HatchTarget hatchTarget : hatchTargets) {
            System.out.println("\n ---");
            System.out.println("Range of: " + hatchTarget.rangeInInches());
            System.out.println("Angle from target of: " + hatchTarget.aspectAngleInRadians());
            assertEquals(37, hatchTarget.rangeInInches(), 1);
        }

    }
}