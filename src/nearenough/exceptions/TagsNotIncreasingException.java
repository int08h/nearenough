package nearenough.exceptions;

/** Tags are not in strictly increasing order */
public class TagsNotIncreasingException extends InvalidRoughTimeMessage {

  public TagsNotIncreasingException(String message) {
    super(message);
  }
}
