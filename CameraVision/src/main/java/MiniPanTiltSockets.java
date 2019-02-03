import java.io.*;
import java.net.Socket;

/**
 * This class implements the protocol (as a proxy) in the MiniPanTiltTeensy project to control
 * an Adafruit mini pan tilt camera mount and send them over network sockets.
 * 
 * @author Chuck Benedict
 * 
 * @see <a href="https://github.com/Team997Coders/MiniPanTiltTeensy/blob/c5175c8d9cf6aac8fe2b29c6fd7d29d29b847805/src/main/java/CommandProcessor.java#L79">CommandProcessor.process()</a>
 */
public class MiniPanTiltSockets extends MiniPanTilt implements IPanTiltMount {
  private Socket socket = null;

  /**
   * Construct the class and wire up streams.
   * 
   * @throws TeensyNotFoundException  Thrown if teensy cannot be found.
   */
  public MiniPanTiltSockets(Socket socket) throws IOException {
    super(socket.getInputStream(), socket.getOutputStream());
    this.socket = socket;
  }

  public void prepareForCommunication() throws CommunicationClosedException, CommunicationFailureException {
    // Nothing needed to do
  }
}
