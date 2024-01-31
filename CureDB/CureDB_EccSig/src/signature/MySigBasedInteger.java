package signature;

import tools.SHA;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class MySigBasedInteger {
    static BigInteger q = new BigInteger("267818515260450669996848160539887038577");//pk
    static BigInteger p = new BigInteger("78340123399837006783038224852656696572742508919089129509710706639118525780363");//pk
    static BigInteger g = new BigInteger("2");//pk
    static BigInteger x = new BigInteger("80792083476166325381101248087928505565953425263637689311640780504669642999859");//sk
    static BigInteger X = new BigInteger("29923740874007181117865071172709104632787621343669067869362966138788016489217");//pk


    public MySigBasedInteger(int blq) {

            BigInteger one = new BigInteger("1");
            BigInteger two = new BigInteger("2");
            BigInteger q, qp, p, a, g;
            int certainty = 100;
            SecureRandom sr = new SecureRandom();
            // blq长度的q， q是p-1的素因子
            //生成BigInteger伪随机数，它可能是（概率不小于1 - 1/2certainty）一个具有指定 bitLength 的素数
            q = new BigInteger(blq, certainty, sr);
            qp = BigInteger.ONE;
            do { // 选择一个素数 p
                p = q.multiply(qp).multiply(two).add(one);
                if(p.isProbablePrime(certainty))
                    break;
                qp = qp.add(BigInteger.ONE);
            } while(true);

            // g!=1
            do {
                a = (two.add(new BigInteger(blq, 100, sr))).mod(p);// (2+x) mod p
                BigInteger ga = (p.subtract(BigInteger.ONE)).divide(q);// (p-1)/q
                g = a.modPow(ga, p); // a^ga mod p = 1
            } while (g.compareTo(BigInteger.ONE) == 0);

            MySigBasedInteger.q = q;
            MySigBasedInteger.p = p;
            MySigBasedInteger.g = g;
            x = BigInteger.probablePrime(q.bitLength(), new Random());
            X = g.modPow(x, p);
        System.out.println(q.bitLength());
        System.out.println(p.bitLength());
        System.out.println(g.bitLength());

    }

//    public MySig(int bitSize) {
//        BigInteger _p = BigInteger.probablePrime(bitSize / 2, new Random());
//        BigInteger _q = BigInteger.probablePrime(bitSize / 2, new Random());
//
//        MySig.p = _p.multiply(_q);
//        MySig.q = _p.subtract(BigInteger.ONE).multiply(_q.subtract(BigInteger.ONE));
//        if (p.bitLength() == 256) {
//            int a = 1;
//        }
//        g = BigInteger.valueOf(2);
//        x = BigInteger.probablePrime(q.bitLength(), new Random());
//        X = g.modPow(x, p);
//    }

    public static BigInteger[] Sig(String message, BigInteger X, BigInteger x, BigInteger p, BigInteger q, BigInteger g) {
        BigInteger r = BigInteger.probablePrime(q.bitLength(), new Random());
        BigInteger R = g.modPow(r, p);
        BigInteger hr = SHA.HASHDataToBigInteger(R.toString()).mod(q);
        BigInteger s = r.multiply(hr).add(SHA.HASHDataToBigInteger(X + message).multiply(x)).mod(q);
        BigInteger d = R.modPow(hr, p);
        return new BigInteger[]{d, s};
    }

    public static boolean verify(BigInteger[] sign, BigInteger X, String message, BigInteger p, BigInteger g) {
        BigInteger v1 = g.modPow(sign[1], p);
        BigInteger v2 = sign[0].multiply(X.modPow(SHA.HASHDataToBigInteger(X + message), p)).mod(p);
        return v1.equals(v2);
    }

    public static void main(String[] args) {
        MySigBasedInteger mySig = new MySigBasedInteger(256);
        String message = "hello";
        BigInteger[] sig = Sig(message, X, x, p, q, g);
        boolean ans = verify(sig, X, message, p, g);
        System.out.println(ans);
    }


}
