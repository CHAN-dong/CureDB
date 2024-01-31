package server;

import dataowner.SearchToken;
import dataowner.SigObj;
import dataowner.TokenNode;
import it.unisa.dia.gas.jpbc.Element;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import signature.MySigBasedECC;
import tools.SHA;
import tools.StringXor;
import java.math.BigInteger;
import java.util.*;
import static signature.MySigBasedECC.aggregate;


public class Server {
    String[] chdFix = new String[]{"00", "01", "10", "11"};
    int rangeBitSize;
    MySigBasedECC eccSig;
    HashMap<String, SigObj> BQFMap;
    HashMap<String, SigObj> path_id_map;
    public Server(HashMap<String, SigObj> BQFMap, HashMap<String, SigObj> path_id_map, int rangeBitSize, MySigBasedECC eccSig) {
        this.path_id_map = path_id_map;
        this.BQFMap = BQFMap;
        this.rangeBitSize = rangeBitSize;
        this.eccSig = eccSig;
    }

    public void update(HashMap<String, SigObj> updData) {
        BQFMap.putAll(updData);
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name","Java HotSpot(TM) ");
        double objectSize = (ObjectSizeCalculator.getObjectSize(BQFMap) + ObjectSizeCalculator.getObjectSize(path_id_map)) / 8.0 / 1024 / 1024;
        System.out.println("Index size：" + objectSize + "MB");
//        System.out.println("BQFMap size:" + BQFMap.size() + ", path_id_map size:" + path_id_map.size());
    }

    //<(2;1;0;1)(0;1;0;1)|;<(1;1;2;0)(0;1;1;0)|;<>;;>;;>
    public SearchRes search(SearchToken tokens) {
        List<Integer> res = new ArrayList<>();
        List<Element[]> sigList = new ArrayList<>();
        String voPath = null;
        int n = tokens.tau_w_list.size();
        int[] versions = new int[n];
        for (int i = 0; i < n; ++i) versions[i] = tokens.tau_w_list.get(i).size();
        voPath = search_and(versions, tokens.tau_w_list, tokens.delta_w_list, tokens.tokenNode,"", res, sigList);

        Element[] aggSig = aggregate(sigList);
        return new SearchRes(voPath, aggSig, res);
    }

    private String search_and(int[] versions, List<List<String>> tau_w_list, List<List<String>> delta_w_list, TokenNode node, String path, List<Integer> res, List<Element[]> sigList) {
        if (node != null && node.child[0] == null && node.child[1] == null && node.child[2] == null && node.child[3] == null) {
            node = null;//若node的所有孩子节点都不存在，则表示该节点的所有子树都需要遍历
        }
        int n = tau_w_list.size();
        //到了叶节点
        if (path.length() == rangeBitSize) {
            SigObj sigObj = path_id_map.get(path);
            res.add(Integer.valueOf(sigObj.chdVersion));
            sigList.add(sigObj.sig);
            return "<>";
        }

        StringBuilder voPath = new StringBuilder();
        voPath.append("<");

        int[][] chdVersionArr = new int[4][n];
        //遍历各关键字
        for (int i = 0; i < n; ++i) {
            String tau_w_path = SHA.HASHDataToString(tau_w_list.get(i).get(versions[i] - 1) + path);
            String delta_w_path = SHA.HASHDataToString(delta_w_list.get(i).get(versions[i] - 1) + path);
            SigObj sigObj = BQFMap.get(tau_w_path);
            sigList.add(sigObj.sig);
            String chdVersionStr = StringXor.xor(delta_w_path, sigObj.chdVersion);
            String[] chdVersion = chdVersionStr.split(";");
            for (int c = 0; c < 4; ++c) chdVersionArr[c][i] = Integer.parseInt(chdVersion[c]);
            voPath.append('(');
            voPath.append(chdVersionStr);
            voPath.append(')');
        }

        //继续遍历孩子节点
        for (int j = 0; j < 4; ++j) {
            if (node == null || node.child[j] != null) {
                boolean isContinue = true;
                for (int i = 0; i < n; ++i) {
                    if (chdVersionArr[j][i] == 0) {
                        isContinue = false;
                        break;
                    }
                }
                if (isContinue) {
                    TokenNode subNode = (node == null ? null : node.child[j]);
                    String subVOPath = search_and(chdVersionArr[j], tau_w_list, delta_w_list, subNode, path + chdFix[j], res, sigList);
                    voPath.append(subVOPath);
                }
            }
            if (j != 3) voPath.append(";");
        }

        voPath.append(">");
        return voPath.toString();
    }
}
