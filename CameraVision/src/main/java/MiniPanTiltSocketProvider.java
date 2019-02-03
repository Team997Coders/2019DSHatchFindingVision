import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MiniPanTiltSocketProvider implements Closeable {
  private Socket socket;
  private final String host;
  private final int port;
  
  public MiniPanTiltSocketProvider(String host, int port) throws UnknownHostException, IOException {
    this.host = host;
    this.port = port;

    // Socket to talk to the MiniPanTilt on roborio
    this.socket = new Socket(host, port);
    // Disable NAGLE algo so that sent packets are not held up.
    // This seems to be disabled on the Windows loopback, so I do
    // not see performance problems there but do when deploying
    // the CameraVision app across the network.
    this.socket.setTcpNoDelay(true);
    System.out.println("MiniPanTilt socket connected.");
  }

  public Socket getSocket() {
    return socket;
  }
  
  public void close() throws IOException {
    if (socket != null) {
      socket.close();
    }
  }
}