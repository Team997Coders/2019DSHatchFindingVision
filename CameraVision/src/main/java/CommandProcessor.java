import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CommandProcessor implements Runnable {
  private final ServerSocket serverSocket;
  private final CommandProcessorValueBuilder valueBuilder;
  private volatile boolean commandAvailable;
  private Command command;

  public CommandProcessor(ServerSocket serverSocket, CommandProcessorValueBuilder valueBuider) {
    this.serverSocket = serverSocket;
    this.valueBuilder = valueBuider;
    // Reset the value builder to make sure we are ready to build
    valueBuilder.reset();
    commandAvailable = false;
  }

  public boolean isCommandAvailable() {
    return commandAvailable;
  }

  public Command getCommand() {
    Command bufferedCommand = command;
    commandAvailable = false;
    return bufferedCommand;
  }

  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      // Get a connection socket so we can get the input stream
      // Only one accept will be allowed at a time...
      System.out.println("Awaiting command connection...");
      try (Socket socket = serverSocket.accept()) {
        // Disable NAGLE algo so that reply packets are not held up.
        socket.setTcpNoDelay(true);
        System.out.println("Command connection established...");
        try (InputStream inputStream = socket.getInputStream()) {
          while (!Thread.currentThread().isInterrupted()) {
            if (!commandAvailable) {
              try {  
                int input = inputStream.read();
                // If the input stream ends, exit the loop
                if (input == -1) {
                  System.out.println("Command connection closed.");
                  break;
                }
                char inputChar = (char) input;
                // What character did we get?
                switch(inputChar) {
                  case '0': case '1': case '2': case '3': case '4':
                  case '5': case '6': case '7': case '8': case '9':
                    valueBuilder.addNumeral(inputChar);
                    break;
                  case '-':
                    valueBuilder.setNegative();
                    break;
                  case 'A': // A button
                  case 'B': // B button
                  case 'X': // X button
                  case 'Y': // Y button
                  case 'c': // Left thumbstick button
                  case 'd': // Right thumbstick button
                  case 'e': // Left shoulder button
                  case 'f': // Right shoulder button
                  case 'g': // Left trigger button
                  case 'h': // Right trigger button
                    // We got a terminating command so make it ready
                    command = new Command(inputChar);
                    valueBuilder.reset();
                    commandAvailable = true;
                    break;
                  case 'p': // Left X joystick value
                    command = new Command(inputChar, valueBuilder.getValue());
                    valueBuilder.reset();
                    commandAvailable = true;
                    break;
                  case 't': // Left Y joystick value
                    command = new Command(inputChar, valueBuilder.getValue());
                    valueBuilder.reset();
                    commandAvailable = true;
                    break;
                }
              } catch (IOException e) {
                // TODO: Under what conditions will this happen?
                // Close the stream and listen for another connection...
                System.err.println(e);
                System.out.println("Command processor continuing...");
                break;
              }
            }
          }
        } catch (IOException e) {
          System.err.println(e);
          System.out.println("Command processor continuing...");
        }
      } catch (IOException e) {
        System.err.println(e);
        System.out.println("Command processor halted.");
        break;          
      }
    }
  }
}