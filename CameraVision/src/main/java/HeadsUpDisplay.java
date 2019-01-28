import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * Encapsulate logic to produce a targeting display that responds to
 * user input.
 */
public class HeadsUpDisplay implements Closeable {
  private final ImageAnnotator imageAnnotator;
  private final HatchTargetPipelineInterpreter interpreter;
  private Map<HeadsUpDisplayStateMachine.Trigger, Point> buttonToPointMap = new HashMap<HeadsUpDisplayStateMachine.Trigger, Point>();
  private Map<String, Point> identifierToPointMap = new HashMap<String, Point>();
  private final Map<HeadsUpDisplayStateMachine.Trigger, String> buttonToIdentifierMap = new HashMap<>();
  private Point slewPoint;
  private final MiniPID pidX;
  private final MiniPID pidY;
  private final MiniPanTiltTeensy panTilt;
  
  /**
   * Constructor for the HUD taking a reference to an annotator and interpreter and camera control.
   * 
   * @param imageAnnotator  The image annotator to draw artifacts on HUD.
   * @param interpreter     The image pipeline interpreter to figure out what is on the image.
   * @param pid             PID settings to control camera positioning.
   * @param panTilt         The panTilt servos controlling the camera position.
   */
  public HeadsUpDisplay(ImageAnnotator imageAnnotator, 
      HatchTargetPipelineInterpreter interpreter,
      MiniPID pidX,
      MiniPID pidY,
      MiniPanTiltTeensy panTilt) {
    if (imageAnnotator == null) {
      throw new IllegalArgumentException("Image annotator cannot be null.");
    }
    if (interpreter == null) {
      throw new IllegalArgumentException("Image interpreter cannot be null.");
    }
    if (panTilt != null && (pidX == null || pidY == null)) {
      throw new IllegalArgumentException("PIDs cannot be null");
    }
    this.imageAnnotator = imageAnnotator;
    this.interpreter = interpreter;
    this.pidX = pidX;
    this.pidY = pidY;
    this.panTilt = panTilt;
    mapButtonsToIdentifiers();
  }

  /**
   * Custom exception to indicate that a target is not expected
   * where it should be.
   */
  public class FailedToLock extends Exception {
    public FailedToLock () {}
    public FailedToLock (String message) {
      super (message);
    }
    public FailedToLock (Throwable cause) {
      super (cause);
    }
    public FailedToLock (String message, Throwable cause) {
      super (message, cause);
    }
  }

  /**
   * Determine whether positioning components are available.
   * 
   * @return  True if positioning available.
   */
  private boolean positioningCamera() {
    return (panTilt != null);
  }

  /**
   * Close open resources.
   */
  public void close() {
    if (positioningCamera()) {
      panTilt.close();
    }
  }

  /**
   * Update the image processing display and camera mount based on current state of state machine.
   * 
   * @param inputImage    The inputImage to annotate.
   * @param currentState  The current state of the state machine.
   * @return              Return the annotated image.
   * @throws FailedToLock Thrown if target failed to lock on.
   */
  public Mat update(Mat inputImage, HeadsUpDisplayStateMachine stateMachine) throws
      FailedToLock,
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    imageAnnotator.beginAnnotation(inputImage);

    if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.IdentifyingTargets) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      imageAnnotator.printDistanceToHatchTargetInInches();
      // This is how we associate hatch targets to buttons
      mapButtonsToTargetCenterPoints(interpreter.getHatchTargetCenters());
      // Print the button identifiers on the hatch targets
      imageAnnotator.printTargetIdentifiers(identifierToPointMap);
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.SlewingToTarget) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle being slewed
        imageAnnotator.drawSlewingRectangle(slewPoint);
        // Continuing slewing camera to get selected target in center of FOV
        slewTargetToCenter();
      } catch (TargetNotFoundException e) {
        // We can no longer find a target containing our selected target point.
        throw new FailedToLock();
      }
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.Panning) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      pan(stateMachine.getPanPct());
      if (stateMachine.getPanPct() == 0) {
        stateMachine.identifyTargets();
      }
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.Tilting) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      tilt(stateMachine.getTiltPct());
      if (stateMachine.getTiltPct() == 0) {
        stateMachine.identifyTargets();
      }
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.Centering) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      center();
      stateMachine.identifyTargets();
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.LockFailed) {
      // TODO: Give visual indication to user that lock failed
      // Print something to the screen for 1-2 seconds
      stateMachine.identifyTargets();
    }

    return imageAnnotator.getCompletedAnnotation();
  }

  /**
   * Slew HUD camera to center selected target within the FOV.
   * 
   * @throws MiniPanTiltTeensy.CommunicationClosedException
   * @throws MiniPanTiltTeensy.TeensyCommunicationErrorException
   * @throws MiniPanTiltTeensy.TeensyCommunicationFailureException
   * @throws TargetNotFoundException
   */
  public void slewTargetToCenter() throws
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException,
      TargetNotFoundException {
    if (positioningCamera()) {
      Point normalizedPoint = interpreter.getNormalizedTargetPositionFromCenter(slewPoint);
      int panPct = (int)Math.round(pidX.getOutput(normalizedPoint.x) * 100);
      int tiltPct = (int)Math.round(pidY.getOutput(normalizedPoint.y) * 100) * -1;
      System.out.println(String.format("normalizedPoint.x=%.2f; normalizedPoint.y=%.2f panPct=%d; tiltPct=%d", normalizedPoint.x, normalizedPoint.y, panPct, tiltPct));
      panTilt.slew(panPct, tiltPct);
    }
  }

  /**
   * Slew the HUD camera based on percentage of maximum slew rate.
   * 
   * @param panPct    Integer from -100 to 100 indicating pan slew rate as a percentage.
   * @param tiltPct   Integer from -100 to 100 indicating tilt slew rate as a percentage.
   * 
   * @throws MiniPanTiltTeensy.CommunicationClosedException
   * @throws MiniPanTiltTeensy.TeensyCommunicationErrorException
   * @throws MiniPanTiltTeensy.TeensyCommunicationFailureException
   */
  public void slew(int panPct, int tiltPct) throws
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    if (positioningCamera()) {
      panTilt.slew(panPct, tiltPct);
    }
  }

  /**
   * Pan the HUD camera based on percentage of maximum pan rate.
   * 
   * @param panPct    Integer from -100 to 100 indicating pan slew rate as a percentage.
   * 
   * @throws MiniPanTiltTeensy.CommunicationClosedException
   * @throws MiniPanTiltTeensy.TeensyCommunicationErrorException
   * @throws MiniPanTiltTeensy.TeensyCommunicationFailureException
   */
  public void pan(int panPct) throws
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    if (positioningCamera()) {
      panTilt.pan(panPct);
    }
  }

  /**
   * Tilt the HUD camera based on percentage of maximum tilt rate.
   * 
   * @param tiltPct    Integer from -100 to 100 indicating tilt slew rate as a percentage.
   * 
   * @throws MiniPanTiltTeensy.CommunicationClosedException
   * @throws MiniPanTiltTeensy.TeensyCommunicationErrorException
   * @throws MiniPanTiltTeensy.TeensyCommunicationFailureException
   */
  public void tilt(int tiltPct) throws
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    if (positioningCamera()) {
      panTilt.tilt(tiltPct);
    }
  }

  /**
   * Slew the HUD camera to the mount's center position.
   * 
   * @throws MiniPanTiltTeensy.CommunicationClosedException
   * @throws MiniPanTiltTeensy.TeensyCommunicationErrorException
   * @throws MiniPanTiltTeensy.TeensyCommunicationFailureException
   */
  public void center() throws
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    if (positioningCamera()) {
      panTilt.center();
    }
  }

  /**
   * Convenience method to stop slewing the HUD camera.
   * 
   * @throws MiniPanTiltTeensy.CommunicationClosedException
   * @throws MiniPanTiltTeensy.TeensyCommunicationErrorException
   * @throws MiniPanTiltTeensy.TeensyCommunicationFailureException
   */
  public void stopSlewing() throws 
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    if(positioningCamera()) {
      panTilt.slew(0, 0);
    }
  }

  private void mapButtonsToTargetCenterPoints(ArrayList<Point> centerPoints) {
    buttonToPointMap.clear();
    identifierToPointMap.clear();
    int buttonIndex = 0;
    for(Point point : centerPoints) {
      switch (buttonIndex) {
        case 0:
          buttonToPointMap.put(HeadsUpDisplayStateMachine.Trigger.AButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(HeadsUpDisplayStateMachine.Trigger.AButton), point);
          break;
        case 1:
          buttonToPointMap.put(HeadsUpDisplayStateMachine.Trigger.BButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(HeadsUpDisplayStateMachine.Trigger.BButton), point);
          break;
        case 2:
          buttonToPointMap.put(HeadsUpDisplayStateMachine.Trigger.XButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(HeadsUpDisplayStateMachine.Trigger.XButton), point);
          break;
        case 3:
          buttonToPointMap.put(HeadsUpDisplayStateMachine.Trigger.YButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(HeadsUpDisplayStateMachine.Trigger.YButton), point);
          break;
        default:
          // TODO: Do nothing for now...should the HUD say "more targets" and provide a way to get to them?
          // Or is 4 targets enough?
      }
      buttonIndex +=1;
    }
  }

  private void mapButtonsToIdentifiers() {
    buttonToIdentifierMap.clear();
    buttonToIdentifierMap.put(HeadsUpDisplayStateMachine.Trigger.AButton, "A");
    buttonToIdentifierMap.put(HeadsUpDisplayStateMachine.Trigger.BButton, "B");
    buttonToIdentifierMap.put(HeadsUpDisplayStateMachine.Trigger.XButton, "X");
    buttonToIdentifierMap.put(HeadsUpDisplayStateMachine.Trigger.YButton, "Y");
  }

  public void setSlewPoint(HeadsUpDisplayStateMachine.Trigger trigger) {
    // Set the point to slew to
    slewPoint = buttonToPointMap.get(trigger);
    // Reset the PID control in order to begin slewing
    if (positioningCamera()) {
      pidX.reset();
      pidY.reset();
    }
  }

  public boolean isSlewPointDefined(HeadsUpDisplayStateMachine.Trigger trigger) {
    return buttonToPointMap.get(trigger) != null;
  }
}