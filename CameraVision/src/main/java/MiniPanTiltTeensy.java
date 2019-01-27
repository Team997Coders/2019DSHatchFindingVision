import purejavacomm.*;
import java.util.*;
import java.io.*;

/**
 * This class implements the serial protocol implemented in the MiniPanTiltTeensy project to control an Adafruit mini pan tilt camera mount.
 * 
 * @author Chuck Benedict
 * 
 * @see <a href="https://github.com/Team997Coders/MiniPanTiltTeensy/blob/c5175c8d9cf6aac8fe2b29c6fd7d29d29b847805/src/main/java/CommandProcessor.java#L79">CommandProcessor.process()</a>
 */
public class MiniPanTiltTeensy implements Closeable {
  protected SerialPort port = null;
  protected InputStream in = null;
  protected OutputStream out = null;
  private static CommPortIdentifier teensyPortIdentifier = null;
  public final static byte T_READY_CMD = 'r';
  public final static byte T_CENTER_CMD = 'c';
  public final static String T_READY_REPLY = "Ready";
  public final static String T_OK_REPLY = "Ok";

  /**
   * Custom exception to indicate teensy not found
   */
  public class TeensyNotFoundException extends Exception {
    public TeensyNotFoundException () {}
    public TeensyNotFoundException (String message) {
      super (message);
    }
    public TeensyNotFoundException (Throwable cause) {
      super (cause);
    }
    public TeensyNotFoundException (String message, Throwable cause) {
      super (message, cause);
    }
  }

  public class TeensyCommunicationFailureException extends Exception {
    public TeensyCommunicationFailureException () {}
    public TeensyCommunicationFailureException (String message) {
      super (message);
    }
    public TeensyCommunicationFailureException (Throwable cause) {
      super (cause);
    }
    public TeensyCommunicationFailureException (String message, Throwable cause) {
      super (message, cause);
    }
  }

  public class TeensyCommunicationErrorException extends Exception {
    public TeensyCommunicationErrorException () {}
    public TeensyCommunicationErrorException (String message) {
      super (message);
    }
    public TeensyCommunicationErrorException (Throwable cause) {
      super (cause);
    }
    public TeensyCommunicationErrorException (String message, Throwable cause) {
      super (message, cause);
    }
  }

  public class CommunicationClosedException extends Exception {
    public CommunicationClosedException () {}
    public CommunicationClosedException (String message) {
      super (message);
    }
    public CommunicationClosedException (Throwable cause) {
      super (cause);
    }
    public CommunicationClosedException (String message, Throwable cause) {
      super (message, cause);
    }
  }

  /**
   * Construct the class and connect to the teensy. The com port is found automatically.
   * 
   * @throws TeensyNotFoundException  Thrown if teensy cannot be found.
   */
  public MiniPanTiltTeensy() throws TeensyNotFoundException {
    if (teensyPortIdentifier == null) {
      teensyPortIdentifier = findTeensyPort();
      if (teensyPortIdentifier == null) {
        throw new TeensyNotFoundException("No MiniPanTiltTeensy found.");
      }
    }
  }

  /**
   * Slew (both pan and tilt) the camera mount with values between -100..100,
   * which represents the percentage rate to slew to maximum.
   * 
   * @param panPct  Percentage of maximum rate to pan
   * @param tiltPct Percentage of maximum rate to tilt
   * @throws CommunicationClosedException         Thrown if communication to device is not open
   * @throws TeensyCommunicationErrorException    Thrown if unexcpeted communication occurs to device.
   * @throws TeensyCommunicationFailureException  Thrown if communication is interrupted/unable to occur in some way.
   */
  public void slew(int panPct, int tiltPct) throws 
      CommunicationClosedException, 
      TeensyCommunicationErrorException,
      TeensyCommunicationFailureException {
    try {
      if (port == null) {
          throw new CommunicationClosedException();
      } else {
        port.enableReceiveTimeout(100);
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
            throw new TeensyCommunicationErrorException("Unexpected reply received when tilting.");
          }
        } else {
          throw new TeensyCommunicationErrorException("Unexpected reply received when panning.");
        }
      }
    } catch (UnsupportedCommOperationException|IOException e) {
      System.err.println(e);
      throw new TeensyCommunicationFailureException(e);
    }
  }

  /**
   * Center the mount of both axes.
   * 
   * @return  True if successful.
   */
  public void center() throws
      CommunicationClosedException, 
      TeensyCommunicationErrorException,
      TeensyCommunicationFailureException {
    try {
      if (port == null) {
        throw new CommunicationClosedException();
      } else {
        port.enableReceiveTimeout(100);
        byte[] rcvdBuffer = new byte[2];
        byte[] sendBuffer = {T_CENTER_CMD};
        out.write(sendBuffer, 0, sendBuffer.length);
        int count = in.read(rcvdBuffer);
        if (!(count == 2 && new String(rcvdBuffer).contentEquals(T_OK_REPLY))) {
          throw new TeensyCommunicationErrorException("Unexpected reply received when centering.");
        }
      }
    } catch (UnsupportedCommOperationException|IOException e) {
      System.err.println(e);
      throw new TeensyCommunicationFailureException(e);
    }
  }

  /**
   * Enumerate COM ports to see if we can find the MiniPanTiltTeensy.
   * 
   * @return  Returns the CommPortIdentifier of the found COM port. Null if not found.
   */
  protected CommPortIdentifier findTeensyPort() {
    Enumeration<CommPortIdentifier> e = CommPortIdentifier.getPortIdentifiers();
    while(e.hasMoreElements()) {
        CommPortIdentifier commPortIdentifier = e.nextElement();
        System.out.println("Scanning port: " + commPortIdentifier.getName());
        try {
            openPort(commPortIdentifier);
            if (teensyReady()) {
                System.out.println("MiniPanTiltTeensy found, port: " + commPortIdentifier.getName());
                return commPortIdentifier;
            } else {
                closePort();
            }
        } catch(PortInUseException ex) {
            System.err.println("Port already in use: " + ex);
        } catch(IOException ex) {
            System.err.println("IO exception testing port: " + ex);
        } catch(InterruptedException ex) {
            System.err.println("Interrupted exception testing port: " + ex);
        } catch(UnsupportedCommOperationException ex) {
            System.err.println("Unsupported comm operation testing port: " + ex);
        }
    }
    return null;
  }

  /**
   * Receive teensy readiness.
   * 
   * @return  True if teensy is ready to go; false otherwise.
   */
  protected boolean teensyReady() {
    try {
      if (port == null) {
          return false;
      } else {
        port.enableReceiveTimeout(100);
        byte[] rcvdBuffer = new byte[5];
        int count = in.read(rcvdBuffer);
        if (count == 5 && new String(rcvdBuffer).contentEquals(T_READY_REPLY)) {
          return true;
        } else {
          byte[] sendBuffer = {T_READY_CMD};
          out.write(sendBuffer, 0, sendBuffer.length);
          count = in.read(rcvdBuffer);
          if (count == 2 && new String(rcvdBuffer).contains(T_OK_REPLY)) {
            return true;
          } else {
            return false;
          }
        }
      }
    } catch (Exception e) {
      System.err.println(e);
      return false;
    }
  }

  /**
   * Open up a comm port so this instance can attempt communication with the teensy.
   * 
   * @param commPortIdentifier    The comm port identifier to open
   * @throws PortInUseException
   * @throws UnsupportedCommOperationException
   * @throws IOException
   * @throws InterruptedException
   */
  protected void openPort(CommPortIdentifier commPortIdentifier) throws 
      PortInUseException, 
      UnsupportedCommOperationException, 
      IOException, 
      InterruptedException {
    port = (SerialPort) commPortIdentifier.open(
        "MiniPanTiltTeensy",    // Name of the application asking for the port 
        2000            // Wait max. 2 sec. to acquire port
    );
    port.setSerialPortParams(57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
    port.setDTR(true);
    out = port.getOutputStream();
    in = port.getInputStream();
    drain();
  }

  /**
   * Closes a comm port if it was opened by the instance.  This should be performed prior to letting
   * this instance get teed up for garbage collection, as the port may remain open until then.
   */
  protected void closePort() {
    if (port != null) {
      try {
        out.flush();
        port.setDTR(false);
        port.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      finally {
        port = null;
        in = null;
        out = null;
      }
    }
  }
  
  public void close() {
    closePort();
  }

  /**
   * Drains any cruft sitting in the teensy serial port receive buffer.
   * 
   * @throws InterruptedException
   * @throws IOException
   */
  protected void drain() throws InterruptedException, IOException {
    Thread.sleep(10);
    int n;
    while ((n = in.available()) > 0) {
      for (int i = 0; i < n; ++i)
        in.read();
      Thread.sleep(10);
    }
  }
}
