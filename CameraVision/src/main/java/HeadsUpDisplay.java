import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

/**
 * Encapsulate logic to produce a targeting display that responds to
 * user input.
 */
public class HeadsUpDisplay {
  private final ImageAnnotator imageAnnotator;
  private final HatchTargetPipelineInterpreter interpreter;
  private Map<CameraControlStateMachine.Trigger, Point> buttonToPointMap = new HashMap<CameraControlStateMachine.Trigger, Point>();
  private Map<CameraControlStateMachine.Trigger, Point> autoLockChoicesToPointMap = new HashMap<CameraControlStateMachine.Trigger, Point>();
  private Map<String, Point> identifierToPointMap = new HashMap<String, Point>();
  private final Map<CameraControlStateMachine.Trigger, String> buttonToIdentifierMap = new HashMap<>();
  private Point slewPoint;
  private CameraControlStateMachine.State state;
  private final NetworkTable smartDashboard;
  private final NetworkTable visionNetworkTable;
  private static int tnfeRetryLimit = 4;
  private int tnfeRetries = 0;
  
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
      NetworkTable visionNetworkTable, 
      NetworkTable smartDashboard) {
    if (imageAnnotator == null) {
      throw new IllegalArgumentException("Image annotator cannot be null.");
    }
    if (interpreter == null) {
      throw new IllegalArgumentException("Image interpreter cannot be null.");
    }
    this.imageAnnotator = imageAnnotator;
    this.interpreter = interpreter;
    this.smartDashboard = smartDashboard;
    this.visionNetworkTable = visionNetworkTable;
    this.state = CameraControlStateMachine.State.IdentifyingTargets;
    mapButtonsToIdentifiers();
    wireUpNetworkTableListeners();
  }

  protected class StateChangeListener implements ITableListener {
    private HeadsUpDisplay hud;

    public StateChangeListener(HeadsUpDisplay hud) {
      this.hud = hud;
    }

    @Override
    public void valueChanged(ITable table, String key, Object value, boolean isNew) {
      if (key == "State") {
        String stateString = (String)value;
        if (stateString == "") {
          stateString = "IdentifyingTargets";
        }
        CameraControlStateMachine.State state = Enum.valueOf(CameraControlStateMachine.State.class, stateString);
        hud.setState(state);
      } else if (key == "Trigger") {
        String triggerString = (String)value;
        if (triggerString.trim().length() == 0) {
          hud.selectTarget(null);
        } else {
          CameraControlStateMachine.Trigger trigger = Enum.valueOf(CameraControlStateMachine.Trigger.class, triggerString);
          hud.selectTarget(trigger);       
        }
      }
    }
  }

  protected void setState(CameraControlStateMachine.State state) {
    this.state = state;
  }

  protected void selectTarget(CameraControlStateMachine.Trigger trigger) {
    // Reset the slewpoint if we have a trigger value
    if (trigger != null) {
      // Try to translate to slew point for button presses
      slewPoint = buttonToPointMap.get(trigger);
      if (slewPoint == null) {
        // Translate to slew point for auto lock
        slewPoint = autoLockChoicesToPointMap.get(trigger);
      }
    }
  }

  public void wireUpNetworkTableListeners() {
    StateChangeListener listener = new StateChangeListener(this);
    visionNetworkTable.addTableListener("State", listener, true);
    visionNetworkTable.addTableListener("Trigger", listener, true);
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
  public Mat update(Mat inputImage) { 
    imageAnnotator.beginAnnotation(inputImage);

    String scoringDirection = smartDashboard.getString("Scoring Direction", "Back");
    int panAngle = (int) Math.round(smartDashboard.getNumber(String.format("%s Camera Pan Angle", scoringDirection), 90) - 90);

    // Look at current state machine state and act
    if (state == CameraControlStateMachine.State.IdentifyingTargets) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      imageAnnotator.printTargetInfo(panAngle);
      // This is how we associate hatch targets to buttons
      mapButtonsToTargetCenterPoints(interpreter.getHatchTargetCenters());
      // Associate closest hatch targets to FOV center to autolock left and right choices
      mapAutoLockChoicesToTargetCenterPoints(interpreter.getHatchTargetCentersClosestToFOVCenter());
      // Print the button identifiers on the hatch targets
      imageAnnotator.printTargetIdentifiers(identifierToPointMap);
      // Write the selectable target triggers to network tables so the state machine
      // knows which targets (and thus which buttons) are selectable
      ArrayList<String> triggers = new ArrayList<String>();
      buttonToPointMap.keySet().forEach((key) -> triggers.add(key.toString()));
      String[] triggersArray = new String[triggers.size()];
      triggersArray = triggers.toArray(triggersArray);
      visionNetworkTable.putStringArray("SelectableTargetTriggers", triggersArray);
      // Clear the selected target
      SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
      selectedTarget.clear();
    } else if (state == CameraControlStateMachine.State.SlewingToTarget) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        tnfeRetries = 0;
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle being slewed
        imageAnnotator.drawSlewingRectangle(slewPoint);
        // Write the selected target information to network tables
        SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
        Point normalizedPointFromCenter = interpreter.getNormalizedTargetPositionFromCenter(slewPoint);
        selectedTarget.write(hatchTarget.rangeInInches(), 
            panAngle, 
            Math.toDegrees(hatchTarget.aspectAngleInRadians()), 
            normalizedPointFromCenter.x, 
            normalizedPointFromCenter.y);
      } catch (TargetNotFoundException e) {
        if (tnfeRetries > tnfeRetryLimit) {
          tnfeRetries = 0;
          // We can no longer find a target containing our selected target point.
          visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.FailedToLock.toString());
        } else {
          tnfeRetries++;
        }
      } catch (NullPointerException e) {
        // If the slewpoint is null, just flip back to identifying targets
        visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.IdentifyTargets.toString());
      }
    } else if (state == CameraControlStateMachine.State.TargetLocked) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        tnfeRetries = 0;
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle showing we are locked
        imageAnnotator.drawLockedRectangle(slewPoint);
        // Print information about target
        imageAnnotator.printTargetInfo(hatchTarget, panAngle);
        // Continue writing the selected target information to network tables
        SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
        Point normalizedPointFromCenter = interpreter.getNormalizedTargetPositionFromCenter(slewPoint);
        selectedTarget.write(hatchTarget.rangeInInches(), 
            panAngle, 
            Math.toDegrees(hatchTarget.aspectAngleInRadians()), 
            normalizedPointFromCenter.x, 
            normalizedPointFromCenter.y);
      } catch (TargetNotFoundException e) {
        if (tnfeRetries > tnfeRetryLimit) {
          tnfeRetries = 0;
          // We can no longer find a target containing our selected target point.
          visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.LoseLock.toString());
        } else {
          tnfeRetries++;
        }
      }
    } else if (state == CameraControlStateMachine.State.LockFailed) {
      // TODO: Give visual indication to user that lock failed
      // Print something to the screen for 1-2 seconds
      // Clear the selected target
      SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
      selectedTarget.clear();
      visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.IdentifyTargets.toString());
    } else if (state == CameraControlStateMachine.State.LockLost) {
      // TODO: Give visual indication to user that lock was lost
      // Print something to the screen for 1-2 seconds
      // Clear the selected target
      SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
      selectedTarget.clear();
      visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.IdentifyTargets.toString());
    } else if (state == CameraControlStateMachine.State.Calibrating) {
      // Clear the selected target
      SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
      selectedTarget.clear();
      // Update luminance values
      // Draw the luminance value on the frame
      imageAnnotator.drawCalibrationInformation();
    } else if (state == CameraControlStateMachine.State.DrivingToTarget) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        tnfeRetries = 0;
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle indicating that driving is in progress
        imageAnnotator.drawDrivingRectangle(slewPoint);
        // Print information about target
        imageAnnotator.printTargetInfo(hatchTarget, panAngle);
        // Continue writing the selected target information to network tables
        SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
        Point normalizedPointFromCenter = interpreter.getNormalizedTargetPositionFromCenter(slewPoint);
        selectedTarget.write(hatchTarget.rangeInInches(), 
            panAngle, 
            Math.toDegrees(hatchTarget.aspectAngleInRadians()), 
            normalizedPointFromCenter.x, 
            normalizedPointFromCenter.y);
      } catch (TargetNotFoundException e) {
        if (tnfeRetries > tnfeRetryLimit) {
          tnfeRetries = 0;
          // We can no longer find a target containing our selected target point.
          visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.LoseLock.toString());
        } else {
          tnfeRetries++;
        }
      }
    } else if (state == CameraControlStateMachine.State.AutoLocked) {
      try {
        // Update the known center of the selected target from the last known point
        HatchTarget hatchTarget = interpreter.getHatchTargetFromPoint(slewPoint);
        tnfeRetries = 0;
        slewPoint = hatchTarget.targetRectangle().center;
        // Draw the targeting rectangle showing we are locked
        imageAnnotator.drawAutoLockedRectangle(slewPoint);
        // Print information about target
        imageAnnotator.printTargetInfo(hatchTarget, panAngle);
        // Continue writing the selected target information to network tables
        SelectedTarget selectedTarget = new SelectedTarget(visionNetworkTable);
        Point normalizedPointFromCenter = interpreter.getNormalizedTargetPositionFromCenter(slewPoint);
        selectedTarget.write(hatchTarget.rangeInInches(), 
            panAngle, 
            Math.toDegrees(hatchTarget.aspectAngleInRadians()), 
            normalizedPointFromCenter.x, 
            normalizedPointFromCenter.y);
      } catch (TargetNotFoundException e) {
        if (tnfeRetries > tnfeRetryLimit) {
          tnfeRetries = 0;
          // We can no longer find a target containing our selected target point.
          visionNetworkTable.putString("Fire", CameraControlStateMachine.Trigger.LoseLock.toString());
        } else {
          tnfeRetries++;
        }
      }
    }

    return imageAnnotator.getCompletedAnnotation();
  }

  private void mapAutoLockChoicesToTargetCenterPoints(ArrayList<Point> centerPoints) {
    autoLockChoicesToPointMap.clear();
    int index = 0;
    for(Point point : centerPoints) {
      switch (index) {
        case 0:
          autoLockChoicesToPointMap.put(CameraControlStateMachine.Trigger.AutoLockLeft, point);
          break;
        case 1:
          autoLockChoicesToPointMap.put(CameraControlStateMachine.Trigger.AutoLockRight, point);
          break;
        default:
          // Do nothing
      }
      index +=1;
    }    
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
          // Or is 4 targets enough? Plenty I am sure....
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