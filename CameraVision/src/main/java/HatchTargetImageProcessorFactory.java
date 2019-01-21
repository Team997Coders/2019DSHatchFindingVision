import edu.wpi.first.wpilibj.networktables.*;

/**
 * Factory class to wire up a hatch target image processor
 * with all the correct dependencies.
 */
public class HatchTargetImageProcessorFactory {
  /**
   * Static helper to create an image processor instance.
   * @param networkTable  The network table to write to
   * @return
   */
    public static ImageProcessor CreateImageProcessor(NetworkTable networkTable) {
        HatchTargetPipeline pipeline = new HatchTargetPipeline();
        return 
          new ImageProcessor(
            pipeline, 
            new NetworkTableWriter(
              new HatchTargetPipelineInterpreter(pipeline), 
              networkTable));
    }
}