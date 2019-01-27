import static org.junit.Assert.assertEquals;

import org.junit.*;
import org.mockito.Mockito.*;

import org.opencv.core.*;
public class DistanceEstimatorUnitTest {
    /**
     * Tests to see if the distance returned is within an inch of the correct distance between hatches.
     */
    boolean hatchTargetExists;
    HatchTarget hatchTarget;
    private double pxPerInch = 20;

    @Test
    public void doesItReturnTheCorrectDistances() {
        RotatedRect leftRectangleMock = new RotatedRect(
            //this fancy long decimal is the distance that the center point is offset from point 0,
            //where the rectangle is actually created.
            new Point((toPx(2) + toPx(0.2796026292)), 0), 
            new Size(toPx(2), toPx(5.5)), 
            -75.5
        );
        RotatedRect rightRectangleMock = new RotatedRect(
            new Point((toPx(14) - toPx(0.2796026292)), 0), 
            new Size(toPx(2), toPx(5.5)), 
            -14.5
        );
        CameraParameters parameters = new Lifecam5000CameraParameters();
        
        try {
            hatchTarget = new HatchTarget(leftRectangleMock, rightRectangleMock, parameters);
            hatchTargetExists = true;
        } catch(Exception e) {
            System.out.println("sadness");
        }

        
        if (hatchTargetExists != false) {
            double x = (hatchTarget.widthInPx()*hatchTarget.pxToInchesConversion(hatchTarget.center().x));
            System.out.println("Found" + x);
            assertEquals(12, x, 1); 
        }
    }

    public double toPx(double inches) {
        return Math.round(inches * pxPerInch);
    }
}