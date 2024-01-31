

package tools;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/*
SHA(Secure Hash Algorithm，安全散列算法），数字签名等密码学应用中重要的工具，
被广泛地应用于电子商务等信息安全领域。虽然，SHA与MD5通过碰撞法都被破解了，
但是SHA仍然是公认的安全加密算法，较之MD5更为安全
*/
    public class SHA {
    public static final String KEY_SHA = "SHA-256";

    public static String PRF(String inputStr){
        StringBuilder sha = new StringBuilder();
        try{
            //核心代码，调用java库实现的部分
            MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
            messageDigest.update(inputStr.getBytes());//字节型
            byte[] digest = messageDigest.digest();
            for (byte b : digest) {
                int v = b & 0xFF;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) sha.append(0);
                sha.append(hv);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sha.toString();
    }
    public static BigInteger HASHDataToBigInteger(String inputStr){
        byte[] digest = null;
        //System.out.println("原始数据:"+inputStr);
        try{
            //核心代码，调用java库实现的部分
            MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
            messageDigest.update(inputStr.getBytes());//字节型
            digest = messageDigest.digest();
//                System.out.println("SHA值:" + sha.toString(2));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new BigInteger(digest);
    }


        public static String HASHDataToString(String inputStr){
            byte[] digest = null;
            //System.out.println("原始数据:"+inputStr);
            try{
                //核心代码，调用java库实现的部分
                MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
                messageDigest.update(inputStr.getBytes());//字节型
                digest = messageDigest.digest();
//                System.out.println("SHA值:" + sha.toString(2));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return new String(digest);
        }

    public static void main(String args[]){
        long startTime, endTime;
        String inputStr = "19";//

        startTime = System.currentTimeMillis(); //获取开始时间
        System.out.println(SHA.PRF("19"));
        endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间：" + (endTime - startTime) + "ms"); //输出程序运行时间
        System.out.println("************************************");
        startTime = System.currentTimeMillis(); //获取开始时间
        System.out.println(SHA.HASHDataToString("19"));
        endTime = System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间：" + (endTime - startTime) + "ms"); //输出程序运行时间
        System.out.println("************************************");
        }


    }

