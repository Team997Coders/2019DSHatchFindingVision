import java.io.*;

/**
 * This class implements the serial protocol implemented in the MiniPanTiltTeensy project to control an Adafruit mini pan tilt camera mount.
 * 
 * @author Chuck Benedict
 * 
 * @see <a href="https://github.com/Team997Coders/MiniPanTiltTeensy/blob/c5175c8d9cf6aac8fe2b29c6fd7d29d29b847805/src/main/java/CommandProcessor.java#L79">CommandProcessor.process()</a>
 */
public abstract class MiniPanTilt implements IPanTiltMount {
  protected InputStream in = null;
  protected OutputStream out = null;
  public final static byte T_READY_CMD = 'r';
  public final static byte T_CENTER_CMD = 'c';
  public final static byte T_ANGLE_CMD = 'a';
  public final static String T_READY_REPLY = "Ready";
  public final static String T_OK_REPLY = "Ok";

  public MiniPanTilt(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }

  /**
   * This function is called prior to operating on the communication device
   * (sending or receiving). Use it to make sure things are ready if you need.
   */
  public abstract void prepareForCommunication() throws CommunicationClosedException, CommunicationFailureException;

  /**
   * Slew (both pan and tilt) the camera mount with values between -100..100,
   * which represents the percentage rate to slew to maximum.
   * 
   * @param panPct  Percentage of maximum rate to pan
   * @param tiltPct Percentage of maximum rate to tilt
   * @throws CommunicationClosedException   Thrown if communication to device is not open
   * @throws CommunicationErrorException    Thrown if unexcpeted communication occurs to device.
   * @throws CommunicationFailureException  Thrown if communication is interrupted/unable to occur in some way.
   */
  public void slew(int panPct, int tiltPct) throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException {
    try {
      prepareForCommunication();
      byte[] rcvdBuffer = new byte[2];
      String message = Integer.toString(panPct) + "p";
      byte[] sendBuffer = message.getBytes("US-ASCII");
      out.write(sendBuffer, 0, sendBuffer.length);
      int count = in.read(rcvdBuffer);
      if (count == 2 && new String(rcvdBuffer).contentEquals(T_OK_REPLY)) {
        message = Integer.toString(tiltPct) + "t";
        sendBuffer = message.getBytes("US-ASCII");
        out.write(sendBuffer, 0, sendBuffer.length);
        count = in.read(rcvdBuffer);
        if (!(count == 2 && new String(rcvdBuffer).contentEquals(T_OK_REPLY))) {
          throw new CommunicationErrorException("slew error: Unexpected reply received when tilting.");
        }
      } else {
        throw new CommunicationErrorException("slew error: Unexpected reply received when panning.");
      }
    } catch (IOException e) {
      System.err.println(e);
      throw new CommunicationFailureException(e);
    }
  }

  /**
   * Get the pan and tilt angles in degrees from the mount. Center is 90 degrees.
   * Range is 0 to 180 degrees.
   * 
   * @return  An Angles object with the pan and tile angles.
   * @throws CommunicationClosedException
   * @throws CommunicationErrorException
   * @throws CommunicationFailureException
   */
  public Angles getAngles() throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException {
    try {
      prepareForCommunication();
      byte[] rcvdBuffer = new byte[5];
      byte[] sendBuffer = {T_ANGLE_CMD};
      out.write(sendBuffer, 0, sendBuffer.length);
      int count = in.read(rcvdBuffer);
      if (!(count == 5)) {
        throw new CommunicationErrorException("pan error: Unexpected reply received when getting angles.");
      }
      byte[] tiltBytes = new byte[2];
      tiltBytes[0] = rcvdBuffer[0];
      tiltBytes[1] = rcvdBuffer[1];
      byte[] panBytes = new byte[2];
      panBytes[0] = rcvdBuffer[3];
      panBytes[1] = rcvdBuffer[4];
      int tilt = Integer.parseInt(new String(tiltBytes, "US-ASCII"), 16);
      int pan = Integer.parseInt(new String(panBytes, "US-ASCII"), 16);
      return new Angles(tilt, pan);
    } catch (IOException e) {
      System.err.println(e);
      throw new CommunicationFailureException(e);
    }    
  }

  /**
   * Pan the camera mount with values between -100..100,
   * which represents the percentage of maximum pan rate.
   * 
   * @param panPct                                Percentage of maximum pan rate
   * @throws CommunicationClosedException         Thrown if communication to device is not open
   * @throws CommunicationErrorException    Thrown if unexcpeted communication occurs to device.
   * @throws CommunicationFailureException  Thrown if communication is interrupted/unable to occur in some way.
   */
  public void pan(int panPct) throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException {
    try {
      prepareForCommunication();
      byte[] rcvdBuffer = new byte[2];
      String message = Integer.toString(panPct) + "p";
      byte[] sendBuffer = message.getBytes("US-ASCII");
      out.write(sendBuffer, 0, sendBuffer.length);
      int count = in.read(rcvdBuffer);
      if (!(count == 2 && new String(rcvdBuffer).contentEquals(T_OK_REPLY))) {
        throw new CommunicationErrorException("pan error: Unexpected reply received when panning.");
      }
    } catch (IOException e) {
      System.err.println(e);
      throw new CommunicationFailureException(e);
    }
  }

  /**
   * Tilt the camera mount with values between -100..100,
   * which represents the percentage of maximum tilt rate.
   * 
   * @param tiltPct                               Percentage of maximum tilt rate
   * @throws CommunicationClosedException         Thrown if communication to device is not open
   * @throws TeensyCommunicationErrorException    Thrown if unexcpeted communication occurs to device.
   * @throws TeensyCommunicationFailureException  Thrown if communication is interrupted/unable to occur in some way.
   */
  public void tilt(int tiltPct) throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException {
    try {
      prepareForCommunication();
      byte[] rcvdBuffer = new byte[2];
      String message = Integer.toString(tiltPct) + "t";
      byte[] sendBuffer = message.getBytes("US-ASCII");
      out.write(sendBuffer, 0, sendBuffer.length);
      int count = in.read(rcvdBuffer);
      if (!(count == 2 && new String(rcvdBuffer).contentEquals(T_OK_REPLY))) {
        throw new CommunicationErrorException("tilt error: Unexpected reply received when tilting.");
      }
    } catch (IOException e) {
      System.err.println(e);
      throw new CommunicationFailureException(e);
    }
  }

  /**
   * Center the mount of both axes.
   * 
   * @return  True if successful.
   */
  public void center() throws
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException {
    try {
      prepareForCommunication();
      byte[] rcvdBuffer = new byte[2];
      byte[] sendBuffer = {T_CENTER_CMD};
      out.write(sendBuffer, 0, sendBuffer.length);
      int count = in.read(rcvdBuffer);
      if (!(count == 2 && new String(rcvdBuffer).contentEquals(T_OK_REPLY))) {
        throw new CommunicationErrorException("center error: Unexpected reply received when centering.");
      }
    } catch (IOException e) {
      System.err.println(e);
      throw new CommunicationFailureException(e);
    }
  }
}
