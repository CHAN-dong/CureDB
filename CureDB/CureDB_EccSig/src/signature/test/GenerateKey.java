
package signature.test;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.security.*;

public class GenerateKey {
    public static KeyPair generateKeyPair(Integer keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

        keyGen.initialize(keySize, random);
        KeyPair generateKeyPair = keyGen.generateKeyPair();
        return generateKeyPair;
    }

    public static String generateRSAKeys(Integer keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = generateKeyPair(keySize);
        String publicKey = Base64.encode(keyPair.getPublic().getEncoded());
        String privateKey = Base64.encode(keyPair.getPrivate().getEncoded());
        String keys="****publicKey start*****"+publicKey+"   ****publicKey end**** ****privateKey start****"+
                privateKey+"  ****privateKey end**** do not copy asteric(*) ****";
        return keys;
    }

}