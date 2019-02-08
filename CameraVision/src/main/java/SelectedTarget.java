import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;

public class SelectedTarget {
  private final NetworkTable visionNetworkTable;
  private final static String SELECTEDTARGETKEY = "SelectedTarget";
  private final static String RANGEININCHESKEY = "RangeInInches";
  private final static String CAMERAANGLEINDEGREES = "CameraAngleInDegrees";
  private final static String ANGLETOTARGETINDEGREES = "AngleToTargetInDegrees";
  private final static String NORMALIZEDPOINTFROMCENTER = "NormalizedPointFromCenter";
  private final static String ACTIVE = "Active";
  private final static String DRIVETOTARGET = "DriveToTarget";
  private final static String NORMALIZEDPOINTFROMCENTERX = "X";
  private final static String NORMALIZEDPOINTFROMCENTERY = "Y";

  public SelectedTarget(NetworkTable visionNetworkTable) {
    this.visionNetworkTable = visionNetworkTable;
  }

  public void write(
      double rangeInInches, 
      double cameraAngleInDegrees, 
      double angleToTargetInDegrees, 
      double normalizedPointFromCenterX, 
      double normalizedPointFromCenterY,
      boolean driveToTarget) {
    ITable selectedTargetTable = visionNetworkTable.getSubTable(SELECTEDTARGETKEY);
    selectedTargetTable.putNumber(RANGEININCHESKEY, rangeInInches);
    selectedTargetTable.putNumber(CAMERAANGLEINDEGREES, cameraAngleInDegrees);
    selectedTargetTable.putNumber(ANGLETOTARGETINDEGREES, angleToTargetInDegrees);
    selectedTargetTable.putBoolean(ACTIVE, true);
    selectedTargetTable.putBoolean(DRIVETOTARGET, driveToTarget);
    ITable normalizedPointFromCenterTable = selectedTargetTable.getSubTable(NORMALIZEDPOINTFROMCENTER);
    normalizedPointFromCenterTable.putNumber(NORMALIZEDPOINTFROMCENTERX, normalizedPointFromCenterX);
    normalizedPointFromCenterTable.putNumber(NORMALIZEDPOINTFROMCENTERY, normalizedPointFromCenterY);
  }

  public void clear() {
    write(0, 0, 0, 0, 0, false);
    ITable selectedTargetTable = visionNetworkTable.getSubTable(SELECTEDTARGETKEY);
    selectedTargetTable.putBoolean(ACTIVE, false);
  }
}