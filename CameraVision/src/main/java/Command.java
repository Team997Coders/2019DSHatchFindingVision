public class Command {
  private char command;
  private double value;

  public Command(char command, double value) {
    this.command = command;
    this.value = value;
  }

  public Command(char command) {
    this(command, 0);
  }
  
  public char getCommand() { return command; }
  public double getValue() { return value; }
}