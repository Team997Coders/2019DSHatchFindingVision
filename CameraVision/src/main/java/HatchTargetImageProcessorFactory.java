import edu.wpi.first.wpilibj.networktables.*;

/**
 * Factory class to wire up a hatch target image processor
 * with all the correct dependencies.
 */
public class HatchTargetImageProcessorFactory {
  /**
   * Static helper to create an image processor instance.
   * @param networkTable  The network table to write all the image processed goodies to
   * @return  A freshly instantiated image processor
   */
  public static ImageProcessor CreateImageProcessor(NetworkTable networkTable, CameraParameters cameraParameters, IHatchTargetPipeline pipeline) {
    HatchTargetPipelineInterpreter interpreter = new HatchTargetPipelineInterpreter(pipeline, cameraParameters);
    return 
      new ImageProcessor(
        pipeline, 
        new NetworkTableWriter(
          interpreter,
          networkTable),
        new ImageAnnotator(interpreter));
  }
}