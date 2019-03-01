import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.junit.Test;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;

public class HeadsUpDisplayUnitTest {
	// This must be done in order to call opencv classes
	static {
		System.loadLibrary("opencv_java310");
  }
  
  @Test
  /**
   * This tests a "high pass filter" which waits a bit, after a camera jolt
   * or rapid movement, to redetect a target in the last known good location. It
   * does it for the SlewingToTarget state.
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
  /**
   * This tests the retry limit for the SlewingToTarget state to see if 
   * we signal to fire the FailedToLock trigger via network tables. I have since
   * realized that there is an RPC mechanism to network tables where this could
   * be done directly...maybe we could refactor.
   */
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

  @Test
  public void itShouldWritePanAngleToNetworkTables() {
    // Assemble
    ImageAnnotator imageAnnotatorMock = mock(ImageAnnotator.class);
    HatchTargetPipelineInterpreter hatchTargetPipelineInterpreterMock = mock(HatchTargetPipelineInterpreter.class);
    NetworkTable visionNetworkTableMock = mock(NetworkTable.class);
    NetworkTable smartDashboardMock = mock(NetworkTable.class);
    Mat inputImageMock = mock(Mat.class);
    HatchTarget hatchTargetMock = mock(HatchTarget.class);
    ITable selectedTargetSubTableMock = mock(ITable.class);
    ITable normalizedPointSubtable = mock(ITable.class);
    when(hatchTargetMock.targetRectangle()).thenReturn(new RotatedRect());
    when(hatchTargetMock.aspectAngleInRadians()).thenReturn(0D);
    when(smartDashboardMock.getString("Scoring Direction", "Back")).thenReturn("Front");
    when(smartDashboardMock.getNumber("Front Camera Pan Angle", 90)).thenReturn(30D);
    when(hatchTargetPipelineInterpreterMock.getHatchTargetFromPoint(null)).thenReturn(hatchTargetMock);
    when(hatchTargetPipelineInterpreterMock.getNormalizedTargetPositionFromCenter(isA(Point.class))).thenReturn(new Point());
    when(visionNetworkTableMock.getSubTable("SelectedTarget")).thenReturn(selectedTargetSubTableMock);
    when(selectedTargetSubTableMock.getSubTable("NormalizedPointFromCenter")).thenReturn(normalizedPointSubtable);

    HeadsUpDisplay hud = new HeadsUpDisplay(imageAnnotatorMock, 
      hatchTargetPipelineInterpreterMock, 
      visionNetworkTableMock, 
      smartDashboardMock);

    hud.setState(CameraControlStateMachine.State.AutoLocked);

    // Act
    hud.update(inputImageMock);

    // Assert
    verify(selectedTargetSubTableMock, times(1)).putNumber("CameraAngleInDegrees", -60D);
  }
}