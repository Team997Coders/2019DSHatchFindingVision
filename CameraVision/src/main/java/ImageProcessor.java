import org.opencv.core.*;

import java.util.concurrent.*;

/**
 * This class performs the processing and resulting action to a given
 * hatch target pipeline.
 */
public class ImageProcessor {
  private HatchTargetPipeline pipeline;
  private INetworkTableWriter networkTableWriter;
  private ImageAnnotator imageAnnotator;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private Future<Mat> processAsyncFuture;

  /**
   * ImageProcessor requires a pipeline to process and a network table writer to write
   * results to.
   * @param pipeline              The pipeline to process
   * @param networkTableWriter    A network table writer to send results to
   */
  public ImageProcessor(HatchTargetPipeline pipeline, 
      INetworkTableWriter networkTableWriter, 
      ImageAnnotator imageAnnotator) {
    if (pipeline == null) {
      throw new IllegalArgumentException();
    }
    this.pipeline = pipeline;
    this.networkTableWriter = networkTableWriter;
    this.imageAnnotator = imageAnnotator;
    this.processAsyncFuture = null;
  }

  /**
   * Process an image asynchronously.  Call awaitProcessCompletion to wait for completion.
   * You can only process one image at a time.
   * @param inputImage    The image to process
   */
  public void processAsync(Mat inputImage)
  {
    if (processAsyncFuture != null) {
      throw new IllegalAccessError("Only one process can be awaited at a time.");            
    }
    // Hold on the the future...use the awaiter to wait for completion
    processAsyncFuture = executor.submit(() -> {
      // Apply the pipeline to the image.
      pipeline.process(inputImage);

      // Update network table
      networkTableWriter.write();
      return imageAnnotator.annotate(inputImage);
    });
  }

  /**
   * Await an image process async call to finish.
   */
  public Mat awaitProcessCompletion() {
    Mat outputImage = null;
    try {
      outputImage = processAsyncFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      System.out.println(e.getMessage());
    } catch (NullPointerException e) {
      throw new IllegalAccessError("You must call processAsync first before awaiting completion.");
    }
    // Reset our future
    processAsyncFuture = null;
    return outputImage;
  }
}