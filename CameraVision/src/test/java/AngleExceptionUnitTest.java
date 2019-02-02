import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.*;
import org.mockito.Mockito.*;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
public class AngleExceptionUnitTest {
    /**
     * Tests to see if the distance returned is within an inch of the correct distance between hatches.
     */
    Mat image;
    IHatchTargetPipeline pipeline;
    CameraParameters cameraParameters;
    HatchTarget hatchTarget;
    ArrayList<HatchTarget> hatchTargets;
    HatchTargetPipelineInterpreter interpreter;

    static {
		System.loadLibrary("opencv_java310");
	}

    @Test
    public void whatAngleDoesItReturn() throws IOException {



        cameraParameters = new Lifecam5000CameraParameters();
        image = Imgcodecs.imread((new File(".\\src\\test\\resource\\2019-02-01-23-59-57-input.jpg")).getCanonicalPath());

        pipeline = new Lifecam5000HatchTargetPipeline();
        pipeline.process(image);

        interpreter = new HatchTargetPipelineInterpreter(pipeline, cameraParameters);
        hatchTargets = interpreter.getHatchTargets();

        assertEquals(0, hatchTargets.size());

    }
}