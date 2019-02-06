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
  public MiniPanTiltSockets(Socket socket) throws IOException, CommunicationErrorException {
    super(socket.getInputStream(), socket.getOutputStream());
    this.socket = socket;
    makeReady();
  }

  /**
   * See if the protocol is ready to process commands.
   */
  private void makeReady() throws CommunicationErrorException {
    try {
      byte[] rcvdBuffer = new byte[5];
      int count = in.read(rcvdBuffer);
      if (count == 5 && new String(rcvdBuffer).contentEquals(MiniPanTilt.T_READY_REPLY)) {
        return;
      } else {
        byte[] sendBuffer = {MiniPanTilt.T_READY_CMD};
        out.write(sendBuffer, 0, sendBuffer.length);
        count = in.read(rcvdBuffer);
        if (count == 2 && new String(rcvdBuffer).contains(MiniPanTilt.T_OK_REPLY)) {
          return;
        } else {
          throw new CommunicationErrorException("Did not receive expected MiniPanTilt readiness handshake over sockets.");
        }
      }
    } catch (IOException e) {
      System.out.println(e.toString());
      throw new CommunicationErrorException("An unexpected exception occurred making MiniPanTilt protocol ready over sockets.");
    }
  }

  public void prepareForCommunication() throws CommunicationClosedException, CommunicationFailureException {
    // Nothing needed to do
  }
}
