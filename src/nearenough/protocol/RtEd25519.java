package nearenough.protocol;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public final class RtEd25519 {

  private static final EdDSAParameterSpec ED25519_SPEC = EdDSANamedCurveTable.getByName(
      EdDSANamedCurveTable.CURVE_ED25519_SHA512
  );

  public static final class Signer {

    private final Signature signer;

    public Signer(byte[] seedBytes) throws InvalidKeyException, SignatureException {
      EdDSAPrivateKeySpec privateSpec = new EdDSAPrivateKeySpec(seedBytes, ED25519_SPEC);
      PrivateKey privateKey = new EdDSAPrivateKey(privateSpec);

      this.signer = new EdDSAEngine(RtHashing.newSha512());
      signer.initSign(privateKey);
    }

    public void update(byte[] content) throws SignatureException {
      signer.update(content);
    }

    public byte[] sign() throws SignatureException {
      return signer.sign();
    }
  }

  public static final class Verifier {

    private final Signature verifier;

    public Verifier(byte[] publicKeyBytes) throws InvalidKeyException, SignatureException {
      EdDSAPublicKeySpec publicSpec = new EdDSAPublicKeySpec(publicKeyBytes, ED25519_SPEC);
      PublicKey publicKey = new EdDSAPublicKey(publicSpec);

      this.verifier = new EdDSAEngine(RtHashing.newSha512());
      verifier.initVerify(publicKey);
    }

    public void update(byte[] content) throws SignatureException {
      verifier.update(content);
    }

    public boolean verify(byte[] signature) throws SignatureException {
      return verifier.verify(signature);
    }
  }
}
