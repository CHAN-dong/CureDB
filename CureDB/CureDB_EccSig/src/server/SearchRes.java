package server;

import it.unisa.dia.gas.jpbc.Element;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import java.util.List;
public class SearchRes {
    public Element[] aggSig;
    public String voPath;
    public List<Integer> res;

    @Override
    public String toString() {
        return "SearchRes{" +
                "\n" + aggSig +
                "\n" + voPath +
                "\n res=" + res +
                '}';
    }

    public SearchRes(String voPath, Element[] aggSig, List<Integer> res) {
        this.voPath = voPath;
        this.aggSig = aggSig;
        this.res = res;
    }

    public double getVOSize() {
        System.setProperty("java.vm.name","Java HotSpot(TM) ");
        double objectSize = (ObjectSizeCalculator.getObjectSize(aggSig) + ObjectSizeCalculator.getObjectSize(voPath) - 343064) / 8.0 / 1024 / 1024;
        return objectSize;
//        String path = "./src/vo.txt";
//        long ans = 0;
//        try {
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(path)));
//            objectOutputStream.writeObject(aggSig);
//            objectOutputStream.writeObject(voPath);
//            objectOutputStream.close();
//            ans = getFileSize(path);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        System.out.println("VO大小：" +  ans / 1024.0 / 1024 + "mb");
    }
}
