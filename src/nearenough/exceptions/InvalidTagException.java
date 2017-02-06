package nearenough.exceptions;

/**
 * Numeric tag value is not recognized
 */
public class InvalidTagException extends InvalidRoughTimeMessage {

  public InvalidTagException(String message) {
    super(message);
  }
}
