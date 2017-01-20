package nearenough.exceptions;

/** Readable message bytes <4 */
public class MessageTooShortException extends InvalidRoughTimeMessage {
  public MessageTooShortException(String message) {
    super(message);
  }
}
