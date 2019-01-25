import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * Encapsulate logic to produce a targeting display that responds to
 * user input.
 */
public class HeadsUpDisplay {
  private final ImageAnnotator imageAnnotator;
  private final HatchTargetPipelineInterpreter interpreter;
  private Map<HeadsUpDisplayStateMachine.Trigger, Point> buttonToPointMap = new HashMap<HeadsUpDisplayStateMachine.Trigger, Point>();
  private Map<String, Point> identifierToPointMap = new HashMap<String, Point>();
  private final Map<HeadsUpDisplayStateMachine.Trigger, String> buttonToIdentifierMap = new HashMap<>();
  private Point slewPoint;
  
  /**
   * Constructor for the HUD taking a reference to an annotator and interpreter.
   * 
   * @param imageAnnotator  The image annotator to draw artifacts on HUD.
   * @param interpreter     The image pipeline interpreter to figure out what is on the image.
   */
  public HeadsUpDisplay(ImageAnnotator imageAnnotator, HatchTargetPipelineInterpreter interpreter) {
    this.imageAnnotator = imageAnnotator;
    this.interpreter = interpreter;
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

  public Mat update(Mat inputImage, HeadsUpDisplayStateMachine.State currentState) throws FailedToLock {
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
      } catch (ImageAnnotator.TargetNotFoundException e) {
        throw new FailedToLock();
      }
    }
    return imageAnnotator.getCompletedAnnotation();
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
    slewPoint = buttonToPointMap.get(trigger);
  }

  public boolean isSlewPointDefined(HeadsUpDisplayStateMachine.Trigger trigger) {
    return buttonToPointMap.get(trigger) != null;
  }
}