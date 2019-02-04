import purejavacomm.*;
import java.io.*;

/**
 * This class provides MiniPanTilt reference wired up to a Teensy.
 * 
 * @author Chuck Benedict
 * 
 * @see <a href="https://github.com/Team997Coders/MiniPanTiltTeensy/blob/c5175c8d9cf6aac8fe2b29c6fd7d29d29b847805/src/main/java/CommandProcessor.java#L79">CommandProcessor.process()</a>
 */
public class MiniPanTiltTeensy extends MiniPanTilt implements IPanTiltMount {
  private SerialPort port = null;

  /**
   * Construct the class and connect to the teensy. The com port is found automatically.
   * 
   * @throws TeensyNotFoundException  Thrown if teensy cannot be found.
   */
  public MiniPanTiltTeensy(SerialPort port) throws IOException {
    super(port.getInputStream(), port.getOutputStream());
    this.port = port;
  }

  public void prepareForCommunication() throws CommunicationClosedException, CommunicationFailureException {
    try {
      if (port == null) {
        throw new CommunicationClosedException();
      } else {
        port.enableReceiveTimeout(100);
      }
    } catch (UnsupportedCommOperationException e) {
      throw new CommunicationFailureException(e);
    }    
  }
}
