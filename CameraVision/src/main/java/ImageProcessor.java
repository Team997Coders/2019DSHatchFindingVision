import org.opencv.core.*;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.concurrent.*;

/**
 * This class performs the processing and resulting action to a given
 * hatch target pipeline.
 */
public class ImageProcessor {
  private HatchTargetPipeline pipeline;
  private INetworkTableWriter networkTableWriter;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private Mat outputImage = new Mat();
  private Future<?> processAsyncFuture;

  /**
   * ImageProcessor requires a pipeline to process and a network table writer to write
   * results to.
   * @param pipeline              The pipeline to process
   * @param networkTableWriter    A network table writer to send results to
   */
  public ImageProcessor(HatchTargetPipeline pipeline, INetworkTableWriter networkTableWriter) {
    if (pipeline == null) {
      throw new IllegalArgumentException();
    }
    this.pipeline = pipeline;
    this.networkTableWriter = networkTableWriter;
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
      return null;
    });
  }

  /**
   * Await an image process async call to finish.
   */
  public void awaitProcessCompletion() {
    try {
      processAsyncFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      System.out.println(e.getMessage());
    } catch (NullPointerException e) {
      throw new IllegalAccessError("You must call processAsync first before awaiting completion.");
    }
    // Reset our future
    processAsyncFuture = null;
  }

  /**
   * Annotate an image of found targets with green rectangles.
   * @param inputImage    The image to annotate.
   * @return              The annotated image.
   */
  public Mat annotate(Mat inputImage) {
    // Write a processed image that you want to restream
    // This is a marked up image of what the camera sees

    // Draw best-fit green rectangles around targets
    Scalar green = new Scalar(81, 190, 0);
    for (MatOfPoint contour: pipeline.findContoursOutput()) {
      RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
      drawRotatedRect(outputImage, rotatedRect, green, 4);
    }        
    
    return outputImage;
  }

  private void drawRotatedRect(Mat image, RotatedRect rotatedRect, Scalar color, int thickness) {
    Point[] vertices = new Point[4];
    rotatedRect.points(vertices);
    MatOfPoint points = new MatOfPoint(vertices);
    Imgproc.drawContours(image, Arrays.asList(points), -1, color, thickness);
  }
}