package signature;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import tools.SHA;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static tools.Utils.getFileSize;

public class MySigBasedECC {
    static Element x;//sk
    static Element P;//pk
    static Element X;//pk

    static Pairing pairing;

    public MySigBasedECC() {
//        PairingParameters params = PairingFactory.getPairingParameters("./jars/a.properties");
//        pairing = PairingFactory.getPairing(params);

        PairingParameters curveParams = new TypeACurveGenerator(160, 256).generate();
        pairing = PairingFactory.getPairing(curveParams);

        x = MySigBasedECC.pairing.getZr().newRandomElement().getImmutable();
        P = MySigBasedECC.pairing.getG1().newRandomElement().getImmutable();
        X = P.mulZn(x);
    }

    public static Element[] sign(String message) {
        Element r = pairing.getZr().newRandomElement().getImmutable();
        Element R = P.mulZn(r);
        Element hr = pairing.getZr().newElement(SHA.HASHDataToBigInteger(R.toString())).getImmutable();
        Element hm = pairing.getZr().newElement(SHA.HASHDataToBigInteger(X + message)).getImmutable();
        Element s = r.mulZn(hr).add(hm.mulZn(x)).getImmutable();
        Element d = R.mulZn(hr);
        return new Element[]{d, s};
    }

//    public static Element[] sign(String message) {
////        System.out.println(message);
////        return new Element[]{pairing.getG1().newRandomElement(), pairing.getZr().newRandomElement()};
//        return null;
//    }

    public static Element[] aggregate(List<Element[]> signList) {
        Element d = pairing.getG1().newZeroElement().getImmutable();
        Element z = pairing.getZr().newZeroElement().getImmutable();
        for (Element[] elements : signList) {
            d = d.add(elements[0]);
            z = z.add(elements[1]);
        }
        return new Element[]{d, z};
    }

    public static boolean verify(Element[] sign, List<String> messageList) {
        Element hm = pairing.getZr().newZeroElement().getImmutable();
        for (String message : messageList) hm = hm.add(pairing.getZr().newElement(SHA.HASHDataToBigInteger(X + message)));
        Element left = P.mulZn(sign[1]);
        Element right = sign[0].add(X.mulZn(hm));
        return left.equals(right);
    }

    public static boolean verify(Element[] sign, String message) {
        Element left = P.mulZn(sign[1]);
        Element right = sign[0].add(X.mulZn(pairing.getZr().newElement(SHA.HASHDataToBigInteger(X + message))));
        return left.equals(right);
    }

    public static void main(String[] args) throws IOException {
        long s, e;
        new MySigBasedECC();


        List<Element[]> signList = new ArrayList<>();
        int n = 1;

        List<String> messageList = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            messageList.add(String.valueOf(i));
        }

        s = System.nanoTime();
        for (int i = 0; i < n; ++i) {
            signList.add(sign(messageList.get(i)));
        }
        e = System.nanoTime();
        System.out.println("sign time:" + (e - s) * 1.0 / n / 100000 + "ms");

        s = System.nanoTime();
        Element[] sign = aggregate(signList);
        e = System.nanoTime();
        System.out.println("agg time:" + (e - s) / 1000000.0 + "ms");

        s = System.nanoTime();
        boolean isPass = verify(sign, messageList);
        e = System.nanoTime();
        System.out.println("verification time:" + (e - s) / 1000000.0 + "ms");
        System.out.println(isPass);
    }

}
