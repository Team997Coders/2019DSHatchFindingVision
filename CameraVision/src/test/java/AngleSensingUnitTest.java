import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.*;
import org.mockito.Mockito.*;

import org.opencv.core.*;
public class AngleSensingUnitTest {
    /**
     * Tests to see if the distance returned is within an inch of the correct distance between hatches.
     */
    boolean hatchTargetExists;
    HatchTarget hatchTarget;
    private double pxPerInch = 20;

    @Test
    public void whatAngleDoesItReturn() {
        RotatedRect leftRectangleMock = new RotatedRect(
            //this fancy long decimal is the distance that the center point is offset from point 0 (in),
            //where the rectangle is actually created.
            //Change the other number to move the rectangles around.
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

        double x = 0;
        
        if (hatchTargetExists != false) {
            x = hatchTarget.aspectAngleInRadians();
            
        }

        System.out.println("Found angle " + x);
            assertTrue("" + x, true);
    }

    public double toPx(double inches) {
        return Math.round(inches * pxPerInch);
    }
}