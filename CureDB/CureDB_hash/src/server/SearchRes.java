package server;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import tools.SHA;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static tools.Utils.getFileSize;

public class SearchRes {
    public List<VOTreeNode> voTreeNodes;
    public List<Integer> res;

    public SearchRes(List<VOTreeNode> voTreeNodes, List<Integer> res) {
        this.voTreeNodes = voTreeNodes;
        this.res = res;
    }

    public double getVOSize() {
        System.setProperty("java.vm.name","Java HotSpot(TM) ");
        double objectSize = (ObjectSizeCalculator.getObjectSize(voTreeNodes)) / 8.0 / 1024 / 1024;
        return objectSize;

//        String path = "./src/vo.txt";
//        long ans = 0;
//        try {
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(path)));
//            objectOutputStream.writeObject(voTreeNodes);
//            objectOutputStream.close();
//            ans = getFileSize(path);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        System.out.println("VO大小：" +  ans / 1024.0 / 1024 + "mb");
    }

    private String getTreeStr(VOTreeNode node) {
        StringBuilder str = new StringBuilder();
        str.append("<");
        if (node.childHash == null) {//空
            return "";
        } else if (node.childHash.length == 1) {//叶节点
            return node.childHash[0];
//            return node.childHash[0];
        } else {
            for (int c = 0; c < 4; ++c) {
                if (node.childHash[c] != null) {
//                    str.append(node.childHash[c]);
                    str.append(SHA.PRF(node.childHash[c]));
                } else {
                    String treeStr = getTreeStr(node.child[c]);
                    str.append(treeStr);
                }
                if (c != 3) str.append(";");
            }
        }
        str.append(">");
        return str.toString();
    }

    @Override
    public String toString() {
        StringBuilder voString = new StringBuilder();
        voString.append(res.toString());
        for (VOTreeNode node : voTreeNodes) {
            voString.append("\n");
            String str = "";
            if (node != null) str = getTreeStr(node);
            voString.append(str);
        }
        return voString.toString();
    }
}
