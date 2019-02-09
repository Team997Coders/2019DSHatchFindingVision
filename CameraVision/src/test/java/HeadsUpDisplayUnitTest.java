import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import org.junit.Test;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class HeadsUpDisplayUnitTest {
	// This must be done in order to call opencv classes
	static {
		System.loadLibrary("opencv_java310");
  }
  
  @Test
  /**
   * This tests a "high pass filter" which waits a bit, after a camera jolt
   * or rapid movement, to redetect a target in the last known good location.
   */
  public void itShouldReturnInputImageAtFirstWhenTargetIsNotFound() {
    // Assemble
    ImageAnnotator imageAnnotatorMock = mock(ImageAnnotator.class);
    HatchTargetPipelineInterpreter hatchTargetPipelineInterpreterMock = mock(HatchTargetPipelineInterpreter.class);
    NetworkTable visionNetworkTableMock = mock(NetworkTable.class);
    NetworkTable smartDashboardMock = mock(NetworkTable.class);
    Mat inputImage = mock(Mat.class);

    when(hatchTargetPipelineInterpreterMock.getHatchTargetFromPoint(any(Point.class))).thenThrow(TargetNotFoundException.class);

    HeadsUpDisplay hud = new HeadsUpDisplay(imageAnnotatorMock, 
      hatchTargetPipelineInterpreterMock, 
      visionNetworkTableMock, 
      smartDashboardMock);

    hud.setState(CameraControlStateMachine.State.SlewingToTarget);

    // Act
    Mat outputImage = hud.update(inputImage);

    //Assert
    assertEquals(inputImage, outputImage);
  }
}