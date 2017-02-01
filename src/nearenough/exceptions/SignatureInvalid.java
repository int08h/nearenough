package nearenough.exceptions;

/**
 * The signature doesn't verify
 */
public class SignatureInvalid extends InvalidRoughTimeMessage {
  public SignatureInvalid(String message) {
    super(message);
  }
}
