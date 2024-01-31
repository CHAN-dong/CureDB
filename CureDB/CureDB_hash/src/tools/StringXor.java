package tools;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class StringXor {
    //加密异或  s1是大整数，s2是短10进制符串 + ("a" or "d")，输出10进制字符串

    public static String xor(String s1, String s2){
        char[] chars1 = s1.toCharArray();
        char[] chars2 = s2.toCharArray();
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < chars2.length; ++i) {
            res.append((char) ((int)chars1[i % chars1.length] ^ (int)chars2[i]));
        }
        return res.toString();
    }

    public static void main(String[] args) {
        String s1 = SHA.HASHDataToString("123");
        String s2 = SHA.HASHDataToString("2345");
        String enc = xor(s1, s2);
        String dex = xor(s1, enc);
        System.out.println(s2);
        System.out.println(dex);

    }
}
