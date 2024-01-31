package signature.test;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class RSA {
    private static final String ALGORITHM = "RSA";
    public String publicKey;
    public String privateKey;
    public RSA() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair=GenerateKey.generateKeyPair(512);
        publicKey = Base64.encode(keyPair.getPublic().getEncoded());
        privateKey = Base64.encode(keyPair.getPrivate().getEncoded());
    }

    public static String sign(List<String> valueList, String privateKey) throws Exception {
        byte[] privateKeyByteArr = Base64.decode(privateKey);
        PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKeyByteArr));
        Signature rsa = Signature.getInstance("SHA1withRSA");
        rsa.initSign(key);
        for (String value : valueList) {
            rsa.update(value.getBytes());
        }
        String signature = Base64.encode(rsa.sign());
        return signature;
    }

    public static boolean verify(List<String> messageList, String signature, String publicKey) throws Exception {
        byte[] publicKeyByteArr = Base64.decode(publicKey);
        PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKeyByteArr));
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(key);
        for (String message : messageList) {
            sig.update(message.getBytes());
        }
        return sig.verify(Base64.decode(signature));
    }

    public static void main(String[] args) throws Exception {
        RSA rsa = new RSA();
        List<String> valueList = new ArrayList<>();
        valueList.add("hello");
        valueList.add("word");
        valueList.add("!");
        long s = System.currentTimeMillis();
        String sign = sign(valueList, rsa.privateKey);
        long e = System.currentTimeMillis();
        System.out.println("签名时间：" + (e - s) + "ms");

        s = System.currentTimeMillis();
        boolean verify = verify(valueList, sign, rsa.publicKey);
        e = System.currentTimeMillis();
        System.out.println("验证时间：" + (e - s) + "ms");
        System.out.println("pass:" + verify);
    }
}
