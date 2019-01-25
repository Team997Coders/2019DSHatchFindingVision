/**
 * This class provides a convenience wrapper for building numbers
 * being entered from the socket command interface.
 */
public class CommandProcessorValueBuilder {
  private int percentage = 0;
  private boolean negation = false;

  public double getValue() {
    return negation ? percentage * -1 / 100.0 : percentage / 100.0;
  }

  public void setPositive() {
    negation = false;
  }

  public void setNegative() {
    negation = true;
  }

  public void reset() {
    negation = false;
    percentage = 0;
  }

  public void addNumeral(char numeral) {
    percentage = (percentage * 10) + (numeral - '0');
  }
}
