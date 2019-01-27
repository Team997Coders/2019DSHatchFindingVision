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
    return panTilt != null;
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
  public Mat update(Mat inputImage, HeadsUpDisplayStateMachine.State currentState) throws
      FailedToLock,
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException {
    imageAnnotator.beginAnnotation(inputImage);
    if (currentState == HeadsUpDisplayStateMachine.State.IdentifyingTargets) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      imageAnnotator.printDistanceToHatchTargetInInches();
      mapButtonsToTargetCenterPoints(interpreter.getHatchTargetCenters());
      imageAnnotator.printTargetIdentifiers(identifierToPointMap);
    } else if (currentState == HeadsUpDisplayStateMachine.State.SlewingToTarget) {
      try {
        imageAnnotator.drawSlewingRectangle(slewPoint);
        slewTargetToCenter();
      } catch (TargetNotFoundException e) {
        throw new FailedToLock();
      }
    }
    return imageAnnotator.getCompletedAnnotation();
  }

  public void slewTargetToCenter() throws
      MiniPanTiltTeensy.CommunicationClosedException,
      MiniPanTiltTeensy.TeensyCommunicationErrorException,
      MiniPanTiltTeensy.TeensyCommunicationFailureException,
      TargetNotFoundException {
    if (positioningCamera()) {
      Point normalizedPoint = interpreter.getNormalizedTargetPositionFromCenter(slewPoint);
      int panPct = (int)Math.round(pidX.getOutput(normalizedPoint.x) * 100);
      int tiltPct = (int)Math.round(pidY.getOutput(normalizedPoint.y) * 100);
      panTilt.slew(panPct, tiltPct);
    }
  }

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