import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

/**
 * Encapsulate logic to produce a targeting display that responds to
 * user input.
 */
public class HeadsUpDisplay {
  private final ImageAnnotator imageAnnotator;
  private final HatchTargetPipelineInterpreter interpreter;
  private Map<CameraControlStateMachine.Trigger, Point> buttonToPointMap = new HashMap<CameraControlStateMachine.Trigger, Point>();
  private Map<String, Point> identifierToPointMap = new HashMap<String, Point>();
  private final Map<CameraControlStateMachine.Trigger, String> buttonToIdentifierMap = new HashMap<>();
  private Point slewPoint;
  private CameraControlStateMachine.State lastState;
  
  /**
   * Constructor for the HUD taking a reference to an annotator and interpreter and camera control.
   * 
   * @param imageAnnotator  The image annotator to draw artifacts on HUD.
   * @param interpreter     The image pipeline interpreter to figure out what is on the image.
   * @param pid             PID settings to control camera positioning.
   * @param panTilt         The panTilt servos controlling the camera position.
   */
  public HeadsUpDisplay(ImageAnnotator imageAnnotator, 
      HatchTargetPipelineInterpreter interpreter) {
    if (imageAnnotator == null) {
      throw new IllegalArgumentException("Image annotator cannot be null.");
    }
    if (interpreter == null) {
      throw new IllegalArgumentException("Image interpreter cannot be null.");
    }
    this.imageAnnotator = imageAnnotator;
    this.interpreter = interpreter;
    mapButtonsToIdentifiers();
  }

  /**
   * Update the image processing display and camera mount based on current state of state machine.
   * 
   * @param inputImage          The inputImage to annotate.
   * @param visionNetworkTable  The vision network table.
   * @param smartDashboard      The smartdashboard network table.
   * @return                    Return the annotated image.
   * @throws FailedToLock       Thrown if target failed to lock on.
   */
  public Mat update(Mat inputImage, NetworkTable visionNetworkTable, NetworkTable smartDashboard) { 
    imageAnnotator.beginAnnotation(inputImage);

    CameraControlStateMachine.State thisState = Enum.valueOf(CameraControlStateMachine.State.class, visionNetworkTable.getString("State", "IdentifyingTargets"));
    int panAngle = (int) Math.round(Math.abs(smartDashboard.getNumber("Camera Pan Angle", 90) - 90));

    // Reset the slewpoint if state change occurred and we have a trigger value
    String triggerString = visionNetworkTable.getString("Trigger", "");
    if (lastState != thisState && triggerString != "") {
      // Get the selected target
      CameraControlStateMachine.Trigger trigger = Enum.valueOf(CameraControlStateMachine.Trigger.class, visionNetworkTable.getString("Trigger", "AButton"));
      // Translate to slew point
      slewPoint = buttonToPointMap.get(trigger);
    }

    // Look at current state machine state and act
    if (thisState == CameraControlStateMachine.State.IdentifyingTargets) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      imageAnnotator.printTargetInfo(panAngle);
      // This is how we associate hatch targets to buttons
      mapButtonsToTargetCenterPoints(interpreter.getHatchTargetCenters());
      // Print the button identifiers on the hatch targets
      imageAnnotator.printTargetIdentifiers(identifierToPointMap);
      ArrayList<String> triggers = new ArrayList<String>();
      buttonToPointMap.keySet().forEach((key) -> triggers.add(key.toString()));
      visionNetworkTable.putStringArray("SelectableTargetTriggers", (String[])triggers.toArray());
    } else if (thisState == CameraControlStateMachine.State.SlewingToTarget) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle being slewed
        imageAnnotator.drawSlewingRectangle(slewPoint);
      } catch (TargetNotFoundException e) {
        // We can no longer find a target containing our selected target point.
        visionNetworkTable.putString("RequestedState", CameraControlStateMachine.State.LockFailed.toString());
      }
    }
    /*
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.TargetLocked) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle showing we are locked
        imageAnnotator.drawLockedRectangle(slewPoint);
        // Continuing slewing camera to get selected target in center of FOV
        slewTargetToCenter(0);
      } catch (TargetNotFoundException e) {
        // We can no longer find a target containing our selected target point.
        stateMachine.loseLock();
      }
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.DrivingToTarget) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle indicating that driving is in progress
        imageAnnotator.drawDrivingRectangle(slewPoint);
        // Continuing slewing camera to get selected target in center of FOV
        slewTargetToCenter(0);
        //TODO: FEED NETWORK TABLES TRACKING INFORMATION
        //TODO: ALSO, ONCE THIS STATE EXITS, THEN NT SHOULD BE CLEARED.
      } catch (TargetNotFoundException e) {
        // We can no longer find a target containing our selected target point.
        stateMachine.loseLock();
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
      // TODO: Something is up with centering. Pressing button always causes a fire of the state machine
      // but the pan/tilt does not always respond. Probably some issue between this app and
      // the pan/tilt and/or the firmware.
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      center();
      stateMachine.identifyTargets();
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.Calibrating) {
      imageAnnotator.drawCalibrationInformation();
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.LockFailed) {
      // TODO: Give visual indication to user that lock failed
      // Print something to the screen for 1-2 seconds
      stateMachine.identifyTargets();
    } else if (stateMachine.getState() == HeadsUpDisplayStateMachine.State.LockLost) {
      // TODO: Give visual indication to user that lock was lost
      // Print something to the screen for 1-2 seconds
      stateMachine.identifyTargets();
    }
*/
    lastState = thisState;
    return imageAnnotator.getCompletedAnnotation();
  }

  private void mapButtonsToTargetCenterPoints(ArrayList<Point> centerPoints) {
    buttonToPointMap.clear();
    identifierToPointMap.clear();
    int buttonIndex = 0;
    for(Point point : centerPoints) {
      switch (buttonIndex) {
        case 0:
          buttonToPointMap.put(CameraControlStateMachine.Trigger.AButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(CameraControlStateMachine.Trigger.AButton), point);
          break;
        case 1:
          buttonToPointMap.put(CameraControlStateMachine.Trigger.BButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(CameraControlStateMachine.Trigger.BButton), point);
          break;
        case 2:
          buttonToPointMap.put(CameraControlStateMachine.Trigger.XButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(CameraControlStateMachine.Trigger.XButton), point);
          break;
        case 3:
          buttonToPointMap.put(CameraControlStateMachine.Trigger.YButton, point);
          identifierToPointMap.put(buttonToIdentifierMap.get(CameraControlStateMachine.Trigger.YButton), point);
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
    buttonToIdentifierMap.put(CameraControlStateMachine.Trigger.AButton, "A");
    buttonToIdentifierMap.put(CameraControlStateMachine.Trigger.BButton, "B");
    buttonToIdentifierMap.put(CameraControlStateMachine.Trigger.XButton, "X");
    buttonToIdentifierMap.put(CameraControlStateMachine.Trigger.YButton, "Y");
  }

  public void setSlewPoint(CameraControlStateMachine.Trigger trigger) {
    // Set the point to slew to
    slewPoint = buttonToPointMap.get(trigger);
    // Reset the PID control in order to begin slewing
  }

  public boolean isSlewPointDefined(CameraControlStateMachine.Trigger trigger) {
    return buttonToPointMap.get(trigger) != null;
  }
}