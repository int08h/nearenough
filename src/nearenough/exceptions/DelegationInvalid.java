package nearenough.exceptions;

/**
 * The DELE message is invalid. The signature doesn't verify or the delegation is outside its
 * time limits.
 */
public class DelegationInvalid extends InvalidRoughTimeMessage {
  public DelegationInvalid(String message) {
    super(message);
  }
}
