import org.junit.*;
import org.opencv.core.*;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class HatchTargetUnitTest {

    @Test
    public void ItShouldBreakIfFarApart() {
        RotatedRect leftRectangleMock = mock(RotatedRect.class);
        RotatedRect rightRectangleMock = mock(RotatedRect.class);
        CameraParameters parameters = new Lifecam5000CameraParameters();

        when(leftRectangleMock.center.x).thenReturn(40.0); 
        when(rightRectangleMock.center.x).thenReturn(560.0); 
        when(leftRectangleMock.size.width).thenReturn(80.0);
        when(rightRectangleMock.size.width).thenReturn(80.0);
        when(leftRectangleMock.angle).thenReturn(-14.0);
        when(rightRectangleMock.angle).thenReturn(14.0);

        assertThrows(HatchTarget.TargetRectanglesException.class, () -> {HatchTarget hatchTarget = new HatchTarget(leftRectangleMock, rightRectangleMock, parameters);});
    }
}