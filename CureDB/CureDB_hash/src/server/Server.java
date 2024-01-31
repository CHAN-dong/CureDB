package server;

import dataowner.SearchToken;
import dataowner.TokenNode;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import jdk.nashorn.internal.parser.Token;
import tools.SHA;
import tools.StringXor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static tools.Utils.getFileSize;

public class Server {
    HashMap<String, String> BQFMap;
    int rangeBitSize;
    String[] chdFix = new String[]{"00", "01", "10", "11"};
    public Server(HashMap<String, String> BQFMap, int rangeBitSize) {
        this.BQFMap = BQFMap;
        this.rangeBitSize = rangeBitSize;
    }

    public void update(HashMap<String, String> updMap) {
        BQFMap.putAll(updMap);
    }
    public void getIndexSize() {
        System.setProperty("java.vm.name","Java HotSpot(TM) ");
        double objectSize = (ObjectSizeCalculator.getObjectSize(BQFMap)) / 8.0 / 1024 / 1024;
        System.out.println("index size：" + objectSize + "MB");
//        System.out.println("BQF size:" + BQFMap.size());

//        long ans = 0;
//        try {
//            String path = "D:\\mycode\\mywork\\SBQF\\index.txt";
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(path)));
//            objectOutputStream.writeObject(BQFMap);
//            ans = getFileSize(path);
//            objectOutputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("index size：" +  ans / 1024.0 / 1024 + "mb");
    }
    public SearchRes search(SearchToken tokens) {
        List<Integer> res = new ArrayList<>();
        List<VOTreeNode> voTreeNodes = null;
        int n = tokens.tau_w_list.size();
        int[] versions = new int[n];
        for (int i = 0; i < n; ++i) versions[i] = tokens.tau_w_list.get(i).size() - 1;
        voTreeNodes = search_and(versions, tokens.tau_w_list, tokens.delta_w_list, tokens.tokenNode, "", res);
        return new SearchRes(voTreeNodes, res);
    }
    //type = 0：∩       type = 1：∪       type = 2：！
    private List<VOTreeNode> search_and(int[] versions, List<List<String>> tau_w_list, List<List<String>> delta_w_list, TokenNode node, String path, List<Integer> res) {
        if (node != null && node.child[0] == null && node.child[1] == null && node.child[2] == null && node.child[3] == null) {
            node = null;//若node的所有孩子节点都不存在，则表示该节点的所有子树都需要遍历
        }
        int n = tau_w_list.size();
        List<VOTreeNode> voTreeNodes = new ArrayList<>(Collections.nCopies(n, null));
        List<String> tau_w_path_List = new ArrayList<>(n);
        //判断该path下是否都有各关键字
        for (int i = 0; i < n; ++i) {
            String tau_w_path = SHA.HASHDataToString(tau_w_list.get(i).get(versions[i]) + path);
            tau_w_path_List.add(tau_w_path);
            if (!BQFMap.containsKey(tau_w_path)) {//如果有关键字在该路径下不存在，则直接返回
                voTreeNodes.set(i, new VOTreeNode());
                return voTreeNodes;
            }
        }
        //该路径下各关键字都有孩子,获取相应的孩子哈希
        voTreeNodes = new ArrayList<>(n);
        int id = -1;

        //获取4个孩子节点版本
        int[][] chdVersionArr = new int[4][n];

        for (int i = 0; i < n; ++i) {
            VOTreeNode chdNode = new VOTreeNode();
            chdNode.child = new VOTreeNode[4];
            String delta_w_path = SHA.HASHDataToString(delta_w_list.get(i).get(versions[i]) + path);
            String value = StringXor.xor(delta_w_path, BQFMap.get(tau_w_path_List.get(i)));
            if (value.length() < 10) {//到了叶节点
                chdNode.childHash = new String[1];
                chdNode.childHash[0] = value;
                id = Integer.parseInt(value);
            } else {
                String[] hashAndVersion = value.split("----");
                chdNode.childHash = hashAndVersion[0].split(";;");
                String[] versionStr = hashAndVersion[1].split(";");
                for (int c = 0; c < 4; ++c) {
                    chdVersionArr[c][i] = Integer.parseInt(versionStr[c]);
                }
            }
            voTreeNodes.add(chdNode);
        }
        if (id != -1) {
            res.add(id);
            return voTreeNodes;
        }
        //继续遍历
        for (int j = 0; j < 4; ++j) {
            if (node == null || node.child[j] != null) {
                TokenNode subNode = (node == null ? null : node.child[j]);
                List<VOTreeNode> subVO = search_and(chdVersionArr[j], tau_w_list, delta_w_list, subNode, path + chdFix[j], res);
                for (int i = 0; i < n; ++i) {
                    if (subVO.get(i) != null) {
                        voTreeNodes.get(i).child[j] = subVO.get(i);
                        voTreeNodes.get(i).childHash[j] = null;
                    }
                }
            }
        }
        return voTreeNodes;
    }
    private List<VOTreeNode> search_or(List<String> tau_w_list, List<String> delta_w_list, TokenNode node, String path, List<Integer> res) {
        if (node != null && node.child[0] == null && node.child[1] == null && node.child[2] == null && node.child[3] == null) {
            node = null;//若node的所有孩子节点都不存在，则表示该节点的所有子树都需要遍历
        }

        int n = tau_w_list.size();
        List<VOTreeNode> voTreeNodes = new ArrayList<>(Collections.nCopies(n, null));

        List<String> tmpTau_w_list = new ArrayList<>(tau_w_list);

        boolean contain = false;
        boolean isFirstNotNull = true;
        //判断该path下是否都有各关键字
        for (int i = 0; i < n; ++i) {
            if (tmpTau_w_list.get(i) == null) continue;
            String tau_w_path = SHA.HASHDataToString(tmpTau_w_list.get(i) + path);
            if (!BQFMap.containsKey(tau_w_path)) {//关键字在该路径下不存在，则在遍历孩子时不需要遍历该节点
                if (isFirstNotNull) {//表示该节点不存在该关键字中止遍历，返回空节点
                    voTreeNodes.set(i, new VOTreeNode());
                }
                tmpTau_w_list.set(i, null);
            } else {
                contain = true;
                if (path.length() == rangeBitSize) {//为叶节点
                    VOTreeNode voTreeNode = new VOTreeNode();
                    String delta_w_path = SHA.HASHDataToString(delta_w_list.get(i) + path);
                    String chdHash = StringXor.xor(delta_w_path, BQFMap.get(tau_w_path));
                    voTreeNode.childHash = new String[1];
                    voTreeNode.childHash[0] = chdHash;
                    int id = Integer.parseInt(chdHash);
                    res.add(id);
                    voTreeNodes.set(i, voTreeNode);
                    return voTreeNodes;
                }
                break;
            }
            isFirstNotNull = false;
        }
        if (!contain) return voTreeNodes;//该节点没有所有关键字，中止遍历

        //遍历孩子节点
        for (int j = 0; j < 4; ++j) {
            if (node == null || node.child[j] != null) {
                TokenNode subNode = (node == null ? null : node.child[j]);
                List<VOTreeNode> subVO = search_or(tmpTau_w_list, delta_w_list, subNode, path + chdFix[j], res);
                for (int i = 0; i < n; ++i) {
                    if (tmpTau_w_list.get(i) == null) continue;
                    if (subVO.get(i) != null) {//孩子j节点存在查询结果
                        if (voTreeNodes.get(i) == null) {
                            VOTreeNode voTreeNode = new VOTreeNode();
                            voTreeNode.child = new VOTreeNode[4];
                            voTreeNodes.set(i, voTreeNode);
                        }
                        voTreeNodes.get(i).child[j] = subVO.get(i);
                    }
                }
            }
        }

        //给返回节点赋予孩子哈希值
        for (int i = 0; i < n; ++i) {
            if (voTreeNodes.get(i) == null) continue;
            VOTreeNode voNode = voTreeNodes.get(i);
            String tau_w_path = SHA.HASHDataToString(tmpTau_w_list.get(i) + path);
            String delta_w_path = SHA.HASHDataToString(delta_w_list.get(i) + path);
            String chdHash = StringXor.xor(delta_w_path, BQFMap.get(tau_w_path));
            voNode.childHash = chdHash.split(";;");
            for (int c = 0; c < 4; ++c) {
                if (voNode.child[c] != null) voNode.childHash[c] = null;
            }
        }

        return voTreeNodes;
    }
}
