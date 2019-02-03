import purejavacomm.*;
import java.util.*;
import java.io.*;

/**
 * This class provides MiniPanTilt reference wired up to a Teensy.
 * 
 * @author Chuck Benedict
 * 
 * @see <a href="https://github.com/Team997Coders/MiniPanTiltTeensy/blob/c5175c8d9cf6aac8fe2b29c6fd7d29d29b847805/src/main/java/CommandProcessor.java#L79">CommandProcessor.process()</a>
 */
public class MiniPanTiltTeensySerialPortFinder implements Closeable {
  private SerialPort port = null;
  private static CommPortIdentifier teensyPortIdentifier = null;
  private InputStream in = null;
  private OutputStream out = null;

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

  /**
   * Construct the class and connect to the teensy. The com port is found automatically.
   * 
   * @throws TeensyNotFoundException  Thrown if teensy cannot be found.
   * @throws IOException              Thrown if there are comm port IO errors
   */
  public MiniPanTiltTeensySerialPortFinder() throws TeensyNotFoundException, IOException {
    if (teensyPortIdentifier == null) {
      teensyPortIdentifier = findTeensyPort();
      if (teensyPortIdentifier == null) {
        throw new TeensyNotFoundException("No MiniPanTiltTeensy found.");
      }
    }
  }

  /**
   * Get the found serial port.
   * @return  The serial port.
   */
  public SerialPort getSerialPort() {
    return port;
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
        } catch(PortInUseException|IOException|InterruptedException|UnsupportedCommOperationException ex) {
            System.out.println(String.format("Error scanning port: %s", ex.toString()));
            ex.printStackTrace();
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
        if (count == 5 && new String(rcvdBuffer).contentEquals(MiniPanTilt.T_READY_REPLY)) {
          return true;
        } else {
          byte[] sendBuffer = {MiniPanTilt.T_READY_CMD};
          out.write(sendBuffer, 0, sendBuffer.length);
          count = in.read(rcvdBuffer);
          if (count == 2 && new String(rcvdBuffer).contains(MiniPanTilt.T_OK_REPLY)) {
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
