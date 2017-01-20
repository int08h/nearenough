package nearenough.exceptions;

/** Tag's offset is out of bounds (outside message boundary) */
public class TagOffsetOverflowException extends InvalidRoughTimeMessage {
  public TagOffsetOverflowException(String message) {
    super(message);
  }
}
