package nearenough.exceptions;

/**
 * Invalid num_tags value in request
 */
public class InvalidNumTagsException extends InvalidRoughTimeMessage {

  public InvalidNumTagsException(String message) {
    super(message);
  }
}
