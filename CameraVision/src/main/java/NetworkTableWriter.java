import java.util.ArrayList;
import edu.wpi.first.wpilibj.networktables.*;

/**
 * This abstract class implements what we should do when writing to network tables
 * after finding balls.  It is abstract so that the keys can be named within
 * a specific implementation, presumably to identify the ball's color.
 * 
 * @author Chuck Benedict, Mentor, Team 997
 */
public class NetworkTableWriter implements INetworkTableWriter
{
  HatchTargetPipelineInterpreter interpreter;
  NetworkTable publishingTable;
  double[] angleArrayRadians;
  double[] targetRanges;

  /**
   * A network table writer needs an interpreter object to determine what has been found
   * from a process pipeline and then a network table to write to.  We send these external
   * dependencies into this class becuase this class only care about writing results
   * out to network tables, not the pre-steps requied to get there.
   * 
   * @param interperter       The interpreter class that converts blob results to interpreted data
   * @param publishingTable   An instantiated network table that interpreted data will get written to
   */
  public NetworkTableWriter(HatchTargetPipelineInterpreter interpreter, NetworkTable publishingTable) {
    this.interpreter = interpreter;
    this.publishingTable = publishingTable;
  }

  /**
   * Write the values to the network table sent into the class constructor.
   * Returns arrays with all available hatchTarget's ranges and angle, boolean
   * targetFound, number of targets, and camera pitch and yaw.
   * 
   * Returning values from the camera may throw communication exceptions.
   */
  public void write() {
    publishingTable.putBoolean("targetFound", interpreter.targetsFound());
    publishingTable.putNumber("targetCount", interpreter.targetCount());

    ArrayList<HatchTarget> hatchTargets = interpreter.getHatchTargets();
    angleArrayRadians = new double[hatchTargets.size()];
    targetRanges = new double[hatchTargets.size()];
    for (int i = 0; i < hatchTargets.size(); i++) {
      angleArrayRadians[i] = hatchTargets.get(i).aspectAngleInRadians();
    }
    publishingTable.putNumberArray("hatchTargetAngles", angleArrayRadians);
    publishingTable.putNumberArray("hatchTargetRanges", targetRanges);
  }

  public String getTargetFoundKey() {
    return "targetFound";
  }

  public String getTargetCountKey() {
    return "targetCount";
  }
}