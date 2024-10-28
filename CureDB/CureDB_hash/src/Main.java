import dataowner.DataOwner;
import dataowner.SearchToken;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import server.SearchRes;
import server.Server;

import java.util.HashMap;

class Experiment {
    double tokenGenTime = 0;
    double queryTime = 0;
    double verificationTime = 0;
    double voSize = 0;
    double tokenSize = 0;
    String tokenStr;
    String VOStr;
    long updTime = 0;
    double updTokenSize = 0;
}

public class Main {


    public static void main(String[] args) throws Exception {
        System.out.println("____________________________test____________________________");
        oneTest("test.txt");
//        oneTest("Uniform100w_1k.txt");
    }

    public static void OneQuery(DataOwner dataOwner, Server server, double rangePer, int keywordsNum, int n) {
        Experiment expData = new Experiment();
//        int[][][] searchKeyword = new int[][][] {
//                {{333}, {871}, {356}, {356}},
//                {{333, 858}, {871, 940}, {356, 659}, {356, 709}},
//                {{333, 858, 482}, {871, 940, 738}, {356, 659, 576}, {356, 709, 131}} ,
//                {{333, 858, 482, 131}, {871, 940, 738, 839}, {356, 659, 576, 273}, {356, 709, 131, 508}},
//                {{333, 858, 482, 131, 508}, {871, 940, 738, 839, 720}, {356, 659, 576, 273, 512}, {356, 709, 131, 508, 512}}
//        };

        for (int i = 0; i < n; ++i) {
            int[][] searchRange = dataOwner.getSearchRange(rangePer);
            int[] searchKeyword = dataOwner.getSearchKeyword(keywordsNum);
//            oneQueryMain(dataOwner, server, searchRange[0], searchRange[1], searchKeyword[keywordsNum - 1][i % 4], expData);
            oneQueryMain(dataOwner, server, searchRange[0], searchRange[1], searchKeyword, expData);
        }

        System.out.println("search token generation time：" + expData.tokenGenTime / 1000.0 / 1000/ n  + "ms");
        System.out.println("query time：" + expData.queryTime / 1000.0 / 1000 / n + "ms");
        System.out.println("verification time：" + expData.verificationTime / 1000000.0 / n + "ms");
        System.out.println("search token size：" + expData.tokenSize * 1024 / n + "kb");
        System.out.println("VO size:" + expData.voSize * 1024 / n + "kb");
//        System.out.println("search token serialized string：" + expData.tokenStr);
//        System.out.println("search VO:" + expData.VOStr);
    }

    public static void OneQuery(DataOwner dataOwner, Server server, int[] point1, int[] point2, int[] keywords) {
        Experiment expData = new Experiment();
        oneQueryMain(dataOwner, server, point1, point2, keywords,expData);
    }

    private static void oneQueryMain(DataOwner dataOwner, Server server, int[] point1, int[] point2, int[] keywords, Experiment expData) {
        long s, e;
        s = System.nanoTime();
        SearchToken searchToken = dataOwner.getSearchToken(point1, point2, keywords);
        e = System.nanoTime();
        expData.tokenGenTime += (e - s);

        s = System.nanoTime();
        SearchRes searchRes = server.search(searchToken);
        e = System.nanoTime();
        expData.queryTime += (e - s);

        expData.VOStr = searchRes.toString();
        expData.voSize += searchRes.getVOSize();

        s = System.nanoTime();
        boolean isPass = dataOwner.verifyRes(searchToken, searchRes);
        e = System.nanoTime();
        expData.verificationTime += (e - s);

        expData.voSize += searchRes.getVOSize();
        expData.tokenStr = searchToken.toString();
        expData.tokenSize += searchToken.getTokenSize();
    }


    public static void oneUpdate(DataOwner dataOwner, Server server, int n) {
        Experiment expData = new Experiment();
        System.setProperty("java.vm.name","Java HotSpot(TM) ");
        long s, e;

        for (int i = 0; i < n; ++i) {
            int x = (int) (Math.random() * (dataOwner.maxX - dataOwner.minX)) + dataOwner.minX;
            int y = (int) (Math.random() * (dataOwner.maxY - dataOwner.minY)) + dataOwner.minY;
            int keyword = (int) (Math.random() * dataOwner.keywordsSize) + 1;
            while (!dataOwner.keywordSet.contains(keyword)) keyword = (int) (Math.random() * dataOwner.keywordsSize) + 1;
            int id = (int) (Math.random() * 10000000);

            s = System.nanoTime();
            HashMap<String, String> updData = dataOwner.update(x, y, keyword, id);
            e = System.nanoTime();
            expData.updTime += (e - s);
            expData.updTokenSize += ObjectSizeCalculator.getObjectSize(updData);
        }

        System.out.println("update time:" + expData.updTime / 1000.0 / 1000 / n + "ms");
        System.out.println("updateToken size:" + expData.updTokenSize / 8.0 / 1024 / n + "kb");
    }

    //The keyword starts from 0
    public static void oneTest(String dataSetPath) throws Exception {
        long s,e;
//        String dataPath = "/usr/dong/dataset/" + dataSetPath;
        String dataPath = "./src/" + dataSetPath;

        s = System.nanoTime();
        DataOwner dataOwner = new DataOwner(dataPath);
        e = System.nanoTime();
        System.out.println("setup time：" + (e - s) / 1000.0 / 1000 + "ms");

        Server server = new Server(dataOwner.getBQFMap(), dataOwner.getRangeBitSize());

        System.out.println("*************************");

//        OneQuery(dataOwner, server, 0.01, 2, 20);

        //update
//        HashMap<String, String> updData = dataOwner.update(1, 1, 10, 2);
//        server.update(updData);
//        System.out.println(dataOwner.getRangeBitSize() + "---rangeBitSize");
//        oneUpdate(dataOwner, server, 100);



        System.out.println("-----------------------range effect-----------------------");
        for (double per = 0.02; per <= 0.1; per += 0.02) {
            System.out.println("********" + per + "," + 2 + "********");
            OneQuery(dataOwner, server, per, 2, 20);
        }
        System.out.println("-----------------------keywords effect-----------------------");
        for (int keyword = 1; keyword <= 5; ++keyword) {
            System.out.println("********" + 0.02 + "," + keyword + "********");
            OneQuery(dataOwner, server, 0.02, keyword, 20);
        }
//
        dataOwner = null;
        System.gc();
        Thread.sleep(1000);
//        server.getIndexSize();

    }

}
