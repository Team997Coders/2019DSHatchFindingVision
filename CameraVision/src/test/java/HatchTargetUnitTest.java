import org.junit.*;
import org.mockito.internal.stubbing.answers.Returns;
import org.opencv.core.*;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class HatchTargetUnitTest {

    private double pxPerInch;
    private double inchesBetweenRectangles;
    /**
     * Makes sure the target validator won't validate targets that are too far apart.
     */
    @Test
    public void ItShouldBreakIfFarApart() {
        //set these variables to stuff, and the code will do the rest of the math.
        //despite the fact that pxPerInch is a double, it should always be an integer.
        //making pxPerInch too big could result in problems, don't go above 46.0 as 
        //that fills the whole FOV.
        pxPerInch = 10.0; 
        inchesBetweenRectangles = 18.0;

        RotatedRect leftRectangleMock = new RotatedRect(
            new Point(toPx(1), 0), 
            new Size(toPx(2), 
            toPx(5)), -75.5
        );
        RotatedRect rightRectangleMock = new RotatedRect(
            new Point(toPx(1 + inchesBetweenRectangles), 0), 
            new Size(toPx(2), 
            toPx(5)), -14.5
        );
        CameraParameters parameters = new Lifecam5000CameraParameters();

        assertThrows(HatchTarget.TargetRectanglesException.class, () -> {HatchTarget hatchTarget = new HatchTarget(leftRectangleMock, rightRectangleMock, parameters);});
    }

    public double toPx(double inches) {
        return (inches * pxPerInch);
    }
}