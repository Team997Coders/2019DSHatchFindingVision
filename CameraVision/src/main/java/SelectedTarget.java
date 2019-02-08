import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;

public class SelectedTarget {
  private final NetworkTable visionNetworkTable;
  private final static String SELECTEDTARGETKEY = "SelectedTarget";
  private final static String RANGEININCHESKEY = "RangeInInches";
  private final static String CAMERAANGLEINDEGREES = "CameraAngleInDegrees";
  private final static String ANGLETOTARGETINDEGREES = "AngleToTargetInDegrees";
  private final static String NORMALIZEDPOINTFROMCENTER = "NormalizedPointFromCenter";
  private final static String ACTIVE = "Enabled";
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
      double normalizedPointFromCenterY) {
    ITable selectedTargetTable = visionNetworkTable.getSubTable(SELECTEDTARGETKEY);
    selectedTargetTable.putNumber(RANGEININCHESKEY, rangeInInches);
    selectedTargetTable.putNumber(CAMERAANGLEINDEGREES, cameraAngleInDegrees);
    selectedTargetTable.putNumber(ANGLETOTARGETINDEGREES, angleToTargetInDegrees);
    ITable normalizedPointFromCenterTable = selectedTargetTable.getSubTable(NORMALIZEDPOINTFROMCENTER);
    normalizedPointFromCenterTable.putNumber(NORMALIZEDPOINTFROMCENTERX, normalizedPointFromCenterX);
    normalizedPointFromCenterTable.putNumber(NORMALIZEDPOINTFROMCENTERY, normalizedPointFromCenterY);
    selectedTargetTable.putBoolean(ACTIVE, true);
  }

  public void clear() {
    ITable selectedTargetTable = visionNetworkTable.getSubTable(SELECTEDTARGETKEY);
    selectedTargetTable.putBoolean(ACTIVE, false);
    selectedTargetTable.putNumber(RANGEININCHESKEY, 0.0);
    selectedTargetTable.putNumber(CAMERAANGLEINDEGREES, 0.0);
    selectedTargetTable.putNumber(ANGLETOTARGETINDEGREES, 0.0);
    ITable normalizedPointFromCenterTable = selectedTargetTable.getSubTable(NORMALIZEDPOINTFROMCENTER);
    normalizedPointFromCenterTable.putNumber(NORMALIZEDPOINTFROMCENTERX, 0.0);
    normalizedPointFromCenterTable.putNumber(NORMALIZEDPOINTFROMCENTERY, 0.0);
  }
}