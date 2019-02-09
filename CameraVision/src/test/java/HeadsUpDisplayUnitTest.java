import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.opencv.core.Mat;

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
  public void itShouldReturnInputImageAtFirstWhenTargetIsNotFoundWhenSlewingToTarget() {
    // Assemble
    ImageAnnotator imageAnnotatorMock = mock(ImageAnnotator.class);
    HatchTargetPipelineInterpreter hatchTargetPipelineInterpreterMock = mock(HatchTargetPipelineInterpreter.class);
    NetworkTable visionNetworkTableMock = mock(NetworkTable.class);
    NetworkTable smartDashboardMock = mock(NetworkTable.class);
    Mat inputImage = mock(Mat.class);

    when(hatchTargetPipelineInterpreterMock.getHatchTargetFromPoint(isNull())).thenThrow(TargetNotFoundException.class);
    when(imageAnnotatorMock.getCompletedAnnotation()).thenReturn(inputImage);

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

  @Test
  public void itShouldThrowTargetNotFoundExcpetionAfterRetriesWhenSlewingToTarget() {
    // Assemble
    ImageAnnotator imageAnnotatorMock = mock(ImageAnnotator.class);
    HatchTargetPipelineInterpreter hatchTargetPipelineInterpreterMock = mock(HatchTargetPipelineInterpreter.class);
    NetworkTable visionNetworkTableMock = mock(NetworkTable.class);
    NetworkTable smartDashboardMock = mock(NetworkTable.class);
    Mat inputImage = mock(Mat.class);

    when(hatchTargetPipelineInterpreterMock.getHatchTargetFromPoint(isNull())).thenThrow(TargetNotFoundException.class);
    when(imageAnnotatorMock.getCompletedAnnotation()).thenReturn(inputImage);

    HeadsUpDisplay hud = new HeadsUpDisplay(imageAnnotatorMock, 
      hatchTargetPipelineInterpreterMock, 
      visionNetworkTableMock, 
      smartDashboardMock);

    hud.setState(CameraControlStateMachine.State.SlewingToTarget);

    // Act
    // Call more than 5 times (retry setting is 0 based)
    hud.update(inputImage);
    hud.update(inputImage);
    hud.update(inputImage);
    hud.update(inputImage);
    hud.update(inputImage);
    hud.update(inputImage);

    //Assert
    // Network table should have a Fire value of FailedToLock
    verify(visionNetworkTableMock, times(1)).putString("Fire", "FailedToLock");
  }
}