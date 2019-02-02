import edu.wpi.cscore.*;
import edu.wpi.cscore.HttpCamera.HttpCameraKind;
import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;

import java.io.File;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

public class Main {

  public static void main(String ... argv) {
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

  public void run(RuntimeSettings runtimeSettings) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    NetworkTable publishingTable = null;

    // Wire up camera parameters for a specific camera...this could be parameterized from command line
    CameraParameters cameraParameters = new Lifecam5000CameraParameters();

    // Wire up the pipeline to use for image processing
    IHatchTargetPipeline pipeline = new Lifecam5000HatchTargetPipeline();

    if (!runtimeSettings.getNoNT()) {
      NetworkTable.setClientMode();
      NetworkTable.setTeam(runtimeSettings.getTeam());
      if (runtimeSettings.getNTHost() != "") {
        NetworkTable.setIPAddress(runtimeSettings.getNTHost());
      }
      NetworkTable.initialize();
      publishingTable = NetworkTable.getTable("SmartDashboard");
    }

    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;

    // This streaming mjpeg server will allow you to see the source image in a browser.
    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);

    // HTTP Camera
    // This is our camera name from the robot.
    // This can be set in your robot code with the following command
    // CameraServer.getInstance().startAutomaticCapture("YourCameraNameHere");
    // "USB Camera 0" is the default if no string is specified
    // In NetworkTables, you can create a key CameraPublisher/<YourCameraNameHere>/streams
    // of an array of strings to store the urls of the stream(s) the camera publishes.
    // These urls point to an mjpeg stream over http, with each jpeg image separated
    // into multiparts with the mixed data sub-type.
    // See https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html for more info.
    // Jpeg part delimiters are separated by a boundary string specified in the Content-Type header.
    //String cameraName = "USB Camera 0";
    String cameraName = "VisionCoProc";
    HttpCamera camera = setHttpCamera(cameraName, inputStream, runtimeSettings.getCameraURL(), runtimeSettings.getNoNT());
    
    /***********************************************/

    // This creates a CvSink for us to use. This grabs images from our selected camera, 
    // and will allow us to use those images in opencv
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    // This creates a CvSource to use.
    // This will take in a Mat image that has had OpenCV operations. 
    CvSource imageSource = new CvSource(
      "CV Image Source", 
      VideoMode.PixelFormat.kMJPEG, 
      camera.getVideoMode().width, 
      camera.getVideoMode().height, 
      camera.getVideoMode().fps);
    // This streaming mjpeg server will allow you to see the final image processed image in a browser.
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);

    // Set up the image pump to grab images.
    ImagePump imagePump = new ImagePump(imageSink);

    // Get pipeline interpreter
    HatchTargetPipelineInterpreter interpreter = new HatchTargetPipelineInterpreter(pipeline, cameraParameters);

    // Get the image annotator
    ImageAnnotator imageAnnotator = new ImageAnnotator(interpreter);

    // Get the HUD
    MiniPanTiltTeensy panTilt = null;
    // Just P works pretty well
    try {panTilt = new MiniPanTiltTeensy();} catch(Exception e){}
    MiniPID pidX = new MiniPID(.32, 0, 0);
    MiniPID pidY = new MiniPID(.32, 0, 0);
    HeadsUpDisplay hud = new HeadsUpDisplay(imageAnnotator, interpreter, pidX, pidY, panTilt);

    // Get the image processor
    ImageProcessor imageProcessor = new ImageProcessor(
      pipeline, 
      new NetworkTableWriter(
        interpreter,
        publishingTable,
        panTilt)
    );
    
    // Get the state machine
    HeadsUpDisplayStateMachine stateMachine = new HeadsUpDisplayStateMachine(hud);

    System.out.println("Opening command port on 2222...");

    Thread commandProcessorThread = null;

    // Open up a server socket to listen for connections for command input
    try(ServerSocket serverSocket = new ServerSocket(2222)) {

      // Set up command processor on its own thread
      CommandProcessor commandProcessor = new CommandProcessor(serverSocket, new CommandProcessorValueBuilder());
      commandProcessorThread = new Thread(commandProcessor);
      commandProcessorThread.start();

      // Init these vars outside processing loop, as they are expensive to create.
      Mat inputImage = new Mat();
      Mat outputImage = new Mat();

      System.out.println("Processing stream...");

      // Prime the image pump
      inputImage = imagePump.pump();

      // Working var to save images at end of processing if requested.
      boolean saveImages = false;

      while (!Thread.currentThread().isInterrupted()) {
        if (!inputImage.empty()) {
          // Process the image concurrently
          // with pumping the frame grabber for the next frame.
          imageProcessor.processAsync(inputImage);
          imagePump.pumpAsync();

          // Await image processing to finsh
          imageProcessor.awaitProcessCompletion();

          // Fetch a user command and update HUD state machine
          if (commandProcessor.isCommandAvailable()) {
            Command command = commandProcessor.getCommand();
            switch (command.getCommand()) {
              case 'A':
                stateMachine.aButtonPressed();
                break;
              case 'B':
                stateMachine.bButtonPressed();
                break;
              case 'X':
                stateMachine.xButtonPressed();
                break;
              case 'Y':
                stateMachine.yButtonPressed();
                break;
              case 'p': // left joystick x
                int panPct = (int)command.getValue();
                stateMachine.pan(panPct);
                break;
              case 't': // left joystick y
                int tiltPct = (int)command.getValue();
                stateMachine.tilt(tiltPct);
                break;
              case 'c':
                stateMachine.leftThumbstickButtonPressed();
                break;
              case 'e':
                stateMachine.leftShoulderButtonPressed();
                break;
              case 'f': // rightShoulderButtonPressed
                saveImages = true;
                break;
              default:
                System.err.println("Command not recognized.");
            }
          }

          // Update the HUD image with current state info
          outputImage = hud.update(inputImage, stateMachine);

          // Write out the HUD image
          imageSource.putFrame(outputImage);

          // This could/should be put into a future
          if (saveImages) {
            saveImages = false;
            saveImages(inputImage, outputImage);
          }

          // Get the next image
          inputImage = imagePump.awaitPumpCompletion();
        } else {
          // Get the next image, because the prior one was empty
          inputImage = imagePump.pump();
        }
      }
    } catch (Exception e) {
      System.err.println(e);
      return;
    } finally {
      if (commandProcessorThread != null) {
        commandProcessorThread.interrupt();
      }
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

  private HttpCamera setHttpCamera(String cameraName, MjpegServer server, String cameraURL, boolean noNT) {
    // If the camera URL is explicitly specified on the command line, then use it.
    if (cameraURL != "") {
      HttpCamera camera = null;
      camera = new HttpCamera("CoprocessorCamera", cameraURL);
      server.setSource(camera);
      return camera;
    } else if (!noNT) {   // get the camera URL from network tables
      // Start by grabbing the camera from NetworkTables
      NetworkTable publishingTable = NetworkTable.getTable("CameraPublisher");
      // Wait for robot to connect. Allow this to be attempted indefinitely
      while (true) {
        try {
          if (publishingTable.getSubTables().size() > 0) {
            break;
          }
          Thread.sleep(500);
          } catch (Exception e) {
              e.printStackTrace();
          }
      }


      HttpCamera camera = null;
      if (!publishingTable.containsSubTable(cameraName)) {
        return null;
      }
      ITable cameraTable = publishingTable.getSubTable(cameraName);
      String[] urls = cameraTable.getStringArray("streams", null);
      if (urls == null) {
        return null;
      }
      ArrayList<String> fixedUrls = new ArrayList<String>();
      for (String url : urls) {
        if (url.startsWith("mjpg")) {
          fixedUrls.add(url.split(":", 2)[1]);
        }
      }
      System.out.println(fixedUrls.toString());
      camera = new HttpCamera("CoprocessorCamera", fixedUrls.toArray(new String[0]));
      server.setSource(camera);
      return camera;
    }
    // It is possible for the camera to be null. If it is, that means no camera could
    // be found using NetworkTables to connect to.  And, user did not specify one on command line.
    // Create an HttpCamera by giving a specified stream
    // Note if this happens, no restream will be created.
    // We assume that you have started up a local mjpeg stream.
    System.out.println("Using hardcoded local http streaming camera...");
    HttpCamera camera = null;
    camera = new HttpCamera("CoprocessorCamera", 
      "http://127.0.0.1:1337/mjpeg_stream", 
      HttpCameraKind.kMJPGStreamer);
    server.setSource(camera);
    return camera;
  }
}