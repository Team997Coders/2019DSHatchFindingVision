import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.networktables.*;

import java.io.File;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

public class Main {

  public static void main(String ... argv) throws CameraParametersException, MalformedURLException {
    Main main = new Main();
    RuntimeSettings runtimeSettings = new RuntimeSettings(argv);
    if (runtimeSettings.parse()) {
      if (runtimeSettings.getHelp()) {
        // print out the usage to sysout
        runtimeSettings.printUsage();
      } else {
        // run the app
        main.run(runtimeSettings);
        System.exit(0);
      }
    } else {
      // print the parameter error, show the usage, and bail
      System.err.println(runtimeSettings.getParseErrorMessage());
      runtimeSettings.printUsage();
      System.exit(1);
    }
  }

  public void run(RuntimeSettings runtimeSettings) throws CameraParametersException, MalformedURLException {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    NetworkTable publishingTable = null;
    NetworkTable smartDashboardTable = null;

    // Wire up camera parameters for a specific camera...this should be queried via the web api (which does not exist)
    CameraParameters frontCameraParameters = new Lifecam3000CameraParametersPi(runtimeSettings.getFrontCameraURL(), "front");
    CameraParameters backCameraParameters = new Lifecam5000CameraParametersPi(runtimeSettings.getBackCameraURL(), "back");

    // Wire up the pipeline to use for image processing
    IHatchTargetPipeline pipeline = new HatchTargetPipelineLifecam();

    if (!runtimeSettings.getNoNT()) {
      NetworkTable.setClientMode();
      NetworkTable.setTeam(runtimeSettings.getTeam());
      if (runtimeSettings.getNTHost() != "") {
        NetworkTable.setIPAddress(runtimeSettings.getNTHost());

      }
      NetworkTable.initialize();
      publishingTable = NetworkTable.getTable("Vision");
      smartDashboardTable = NetworkTable.getTable("SmartDashboard");
    }

    // Set up the cameras
    HttpCamera frontCamera = new HttpCamera("Front", runtimeSettings.getFrontCameraURL());
    HttpCamera backCamera = new HttpCamera("Back", runtimeSettings.getBackCameraURL());
    
    // This creates a CvSink for us to use. This grabs images from our selected camera, 
    // and will allow us to use those images in opencv
    CvSink frontImageSink = new CvSink("Front Image Grabber");
    frontImageSink.setSource(frontCamera);
    CvSink backImageSink = new CvSink("Back Image Grabber");
    backImageSink.setSource(backCamera);

    // This creates a CvSource to use.
    // This will take in a Mat image that has had OpenCV operations. 
    CvSource imageSource = new CvSource(
      "CV Image Source", 
      VideoMode.PixelFormat.kMJPEG, 
      backCamera.getVideoMode().width, 
      backCamera.getVideoMode().height, 
      backCamera.getVideoMode().fps);
    // This streaming mjpeg server will allow you to see the final image processed image in a browser.
    // TCP Port Usage 
    // By rules, this has to be between 1180 and 1190.
    MjpegServer cvStream = new MjpegServer("HUD", 1186);
    cvStream.setSource(imageSource);

    // Set up the image pump to grab images.
    ImagePump frontImagePump = new ImagePump(frontImageSink);
    ImagePump backImagePump = new ImagePump(backImageSink);

    ScoringDirectionStates scoringDirection = getScoringDirection(smartDashboardTable);

    // Get pipeline interpreter
    HatchTargetPipelineInterpreter interpreter = new HatchTargetPipelineInterpreter(pipeline, 
      scoringDirection == ScoringDirectionStates.Front ? frontCameraParameters : backCameraParameters);

    // Get the image annotator
    ImageAnnotator imageAnnotator = new ImageAnnotator(interpreter);

    // Flag to indicate whether to continue looping
    boolean looping = true;

    HeadsUpDisplay hud = new HeadsUpDisplay(imageAnnotator, interpreter, publishingTable, smartDashboardTable);

    // Get the image processor
    ImageProcessor imageProcessor = new ImageProcessor(
      pipeline, 
      new NetworkTableWriter(
        interpreter,
        publishingTable)
    );

    // Init these vars outside processing loop, as they are expensive to create.
    Mat inputImage = new Mat();
    Mat outputImage = new Mat();

    System.out.println("Processing stream...");

    // Prime the image pump
    inputImage = scoringDirection == ScoringDirectionStates.Front ? frontImagePump.pump() : backImagePump.pump();

    // Working var to save images at end of processing if requested.
    boolean saveImages = false;

    while (!Thread.currentThread().isInterrupted() && looping) {
      if (!inputImage.empty()) {
        // Process the image concurrently...
        imageProcessor.processAsync(inputImage);

        // ... while pumping the frame grabber for the next frame.
        if (scoringDirection == ScoringDirectionStates.Front) {
          frontImagePump.pumpAsync();
          interpreter.setCameraParameters(frontCameraParameters);
        } else {
          backImagePump.pumpAsync();
          interpreter.setCameraParameters(backCameraParameters);
        }

        // Await image processing to finsh
        imageProcessor.awaitProcessCompletion();

        // Update the HUD image with current state info
        outputImage = hud.update(inputImage);

        // Write out the HUD image
        imageSource.putFrame(outputImage);

        // This could/should be put into a future
        // TODO: Read state from smartdashboard in order to trigger 
        if (saveImages) {
          saveImages = false;
          saveImages(inputImage, outputImage);
        }

        // Get the next image
        inputImage = scoringDirection == ScoringDirectionStates.Front ? frontImagePump.awaitPumpCompletion() : backImagePump.awaitPumpCompletion();
      } else {
        // Get the next image, because the prior one was empty
        inputImage = scoringDirection == ScoringDirectionStates.Front ? frontImagePump.pump() : backImagePump.pump();
      }

      // Update the scoring direction
      scoringDirection = getScoringDirection(smartDashboardTable);
    }
  }

  private enum ScoringDirectionStates {
    None,
    Front,
    Back
  }

  private ScoringDirectionStates getScoringDirection(NetworkTable smartDashboardTable) {
    String stateString;

    if (smartDashboardTable == null) {
      return ScoringDirectionStates.Back;
    } else {
      stateString = smartDashboardTable.getString("Scoring Direction", "Back");
      return Enum.valueOf(ScoringDirectionStates.class, stateString);
    }
  }

  private void saveImages(Mat inputImage, Mat outputImage) {
    // Create directory if it does not exist
    String imagesPath = String.format("%s/images", System.getProperty("user.dir"));
    new File(imagesPath).mkdirs();

    if ((new File(imagesPath)).isDirectory()) {
      // Create a unique file prefix
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      Date today = Calendar.getInstance().getTime();        
      String dateTime = dateFormat.format(today);

      // Save input image
      String inputFile = String.format("%s/%s-input.jpg", imagesPath, dateTime);
      Imgcodecs.imwrite(inputFile, inputImage);

      // Save output image
      String outputFile = String.format("%s/%s-output.jpg", imagesPath, dateTime);
      Imgcodecs.imwrite(outputFile, outputImage);
    } else {
      System.out.println(String.format("Could not create %s directory to save images.", imagesPath));
    }
  }
}