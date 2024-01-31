package dataowner;

import server.SearchRes;
import server.VOTreeNode;
import tools.SHA;
import tools.StringXor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class DataOwner {
    public HashSet<Integer> keywordSet = new HashSet<>();
    String[] chdFix = new String[]{"00", "01", "10", "11"};
    int rangeBitSize;
    public int keywordsSize;
    public int minX,minY,maxX,maxY;
    int[][] mix = new int[][] {{0,1,2,3}, {0,1,3,2}, {0,2,1,3}, {0,2,3,1}, {0,3,1,2}, {0,3,2,1}, {1,0,2,3}, {1,0,3,2}, {1,2,0,3}, {1,2,3,0}, {1,3,0,2}, {1,3,2,0}, {2,0,1,3}, {2,0,3,1}, {2,1,0,3}, {2,1,3,0}, {2,3,0,1}, {2,3,1,0}, {3,0,1,2}, {3,0,2,1}, {3,1,0,2}, {3,1,2,0}, {3,2,0,1}, {3,2,1,0}};
    String k1 = "10110011";
    String k2 = "10110010";
    String k3 = "10110000";
    HashMap<Integer, Integer> tokenState = new HashMap<>();
    HashMap<String, String> rootHashes = new HashMap<>();
    HashMap<String, String> BQFMap = new HashMap<>();
    VKFNode skTreeRoot;
    public int getRangeBitSize() {return rangeBitSize;}
    public HashMap<String, String> getBQFMap() {
        return BQFMap;
    }
    public DataOwner(String path) throws Exception {
        //读数据
        List<SpatialData> dataList = readSpatialData(path);
        //获取明文树
        skTreeRoot = buildTree(dataList);
        //加密范围
        encTreePrefix(skTreeRoot, "");
        //计算各关键字树哈希值
        getNodeHash(skTreeRoot);
        //加密关键字
        HashMap<Integer, String> keyword_tau_map = new HashMap<>();
        HashMap<Integer, String> keyword_delta_map = new HashMap<>();
        for (int keyword = 0; keyword < keywordsSize; ++keyword) {
            tokenState.put(keyword, 0);
            String tau_w = SHA.HASHDataToString(k1 + keyword + tokenState.get(keyword));
            String delta_w = SHA.HASHDataToString(k2 + keyword + tokenState.get(keyword));
            keyword_tau_map.put(keyword, tau_w);
            keyword_delta_map.put(keyword, delta_w);
        }
        encTreeKeyword(skTreeRoot, keyword_tau_map, keyword_delta_map);
        //记录各关键字树根哈希值
        for (Map.Entry<Integer, String> entry : skTreeRoot.keywordNodeHash.entrySet()) {
            rootHashes.put(keyword_tau_map.get(entry.getKey()), SHA.HASHDataToString(entry.getValue()));
        }
    }
    public void encTreePrefix(VKFNode skTreeNode, String path) {
        if (skTreeNode == null) return;
        int r = Math.abs((k3 + path).hashCode()) % 24;
         int[] mixedPath = mix[r];
        //交换孩子节点位置
        VKFNode[] tmpChdNode = Arrays.copyOf(skTreeNode.childes, 4);
        for (int i = 0; i < 4; ++i) {
            skTreeNode.childes[mixedPath[i]] = tmpChdNode[i];
        }

        for (int i = 0; i < 4; ++i) {
            encTreePrefix(skTreeNode.childes[i], path + chdFix[i]);
        }
        skTreeNode.prefix = path;
    }
    public boolean verifyRes(SearchToken searchToken, SearchRes searchRes) {
        int n = searchToken.tau_w_list.size();
        getVOHash(searchToken.tokenNode, searchRes.voTreeNodes);

        //对比根节点哈希
        for (int j = 0; j < n; ++j) {
            VOTreeNode node = searchRes.voTreeNodes.get(j);
            if (node == null) continue;
            StringBuilder chdHash = new StringBuilder();
            for (int i = 0; i < 4; ++i) {
                if (node.childHash[i] != null) chdHash.append(node.childHash[i]);
                if (i != 3) chdHash.append(";;");
            }
            String rootHash = SHA.HASHDataToString(chdHash.toString());
            if (!rootHash.equals(rootHashes.get(searchToken.tau_w_list.get(j).get(0)))) {
                return false;
            }
        }
        return true;
    }
    private void getVOHash(TokenNode tokenNode, List<VOTreeNode> nodeList) {
        //tokenNode 为空表示需要遍历该孩子节点
        if (tokenNode != null && tokenNode.child[0] == null && tokenNode.child[1] == null && tokenNode.child[2] == null && tokenNode.child[3] == null) {
            tokenNode = null;
        }
        int n = nodeList.size();
        //若是叶节点，不需要计算哈希值
        if (nodeList.get(0) == null || nodeList.get(0).childHash.length == 1 && nodeList.get(0).childHash[0].length() < 10) return;
        //遍历四个孩子分支
        for (int c = 0; c < 4; ++c) {
            List<VOTreeNode> subNodeList = new ArrayList<>(n);
            boolean isContinue = false;
            if (tokenNode == null || tokenNode.child[c] != null) {//若该路径第c个孩子需要遍历
                for (VOTreeNode voTreeNode : nodeList) {//遍历所有关键字树第c个孩子节点
                    if (voTreeNode == null) {//没有遍历这个关键字树
                        subNodeList.add(null);
                    } else if (voTreeNode.child[c] != null && voTreeNode.child[c].childHash == null) {//若有某关键字树孩子节点不为空，并且孩子哈希值字段为空，表示孩子节点没有该关键字
                        voTreeNode.childHash[c] = SHA.HASHDataToString("");
                        subNodeList.add(null);
                    } else {
                        subNodeList.add(voTreeNode.child[c]);
                        isContinue = true;
                    }
                }
            }

            //仍然有关键字树需要递归获取孩子哈希值
            if (isContinue) {
                TokenNode subTokenNode = (tokenNode == null ? null : tokenNode.child[c]);
                getVOHash(subTokenNode, subNodeList);//递归获取孩子节点哈希
                //计算所有关键字树在该路径下第c个孩子哈希值
                for (int i = 0; i < n; ++i) {
                    if (subNodeList.get(i) == null) continue;
                    if (subNodeList.get(i).childHash[0].length() < 10) {//孩子是叶节点
                        nodeList.get(i).childHash[c] = SHA.HASHDataToString(subNodeList.get(i).childHash[0]);
                    } else {//孩子是中间节点
                        StringBuilder chdHashes = new StringBuilder();
                        for (int j = 0; j < 4; ++j) {
                            chdHashes.append(subNodeList.get(i).childHash[j]);
                            if (j != 3) chdHashes.append(";;");
                        }
                        nodeList.get(i).childHash[c] = SHA.HASHDataToString(chdHashes.toString());
                    }
                }
            }
        }
    }
    private void getNodeHash(VKFNode node) {
        if (node == null) return;
        if (node.prefix.length() == rangeBitSize) {
            for (int keyword : node.data.keywords) {
                node.addKeywordHash(keyword, String.valueOf(node.data.id));//叶节点只保存相应的id，没有孩子哈希值
            }
            return;
        }
        for (VKFNode chd : node.childes) getNodeHash(chd);
        for (VKFNode chd : node.childes) {
            if (chd != null) {
                for (Map.Entry<Integer, String> entry : chd.keywordNodeHash.entrySet()) {//遍历所有孩子的关键字
                    int keyword = entry.getKey();
                    StringBuilder hash = new StringBuilder();
                    if (!node.keywordNodeHash.containsKey(keyword) || node.keywordNodeHash.get(keyword) == null) {//若该关键字没有遍历
                        for (int c = 0; c < 4; ++c) {//获取到所有孩子该关键字的哈希
                            if (node.childes[c] != null && node.childes[c].keywordNodeHash.containsKey(keyword) && node.childes[c].keywordNodeHash.get(keyword) != null) {
                                hash.append(SHA.HASHDataToString(node.childes[c].keywordNodeHash.get(keyword)));
                            } else {
                                hash.append(SHA.HASHDataToString(""));
                            }
                            if (c != 3) hash.append(";;");
                        }
                        node.addKeywordHash(keyword, hash.toString());
                    }
                }
            }
        }
    }
    private void encTreeKeyword(VKFNode skTreeNode, HashMap<Integer, String> keyword_tau_map, HashMap<Integer, String> keyword_delta_map) {
        if (skTreeNode == null) return;

        for (Map.Entry<Integer, String> entry : skTreeNode.keywordNodeHash.entrySet()) {
            int keyword = entry.getKey();
            StringBuilder hash = new StringBuilder(entry.getValue());
            String tau_w = keyword_tau_map.get(keyword);
            String delta_w = keyword_delta_map.get(keyword);
            String tau_w_path = SHA.HASHDataToString(tau_w + skTreeNode.prefix);
            String delta_w_path = SHA.HASHDataToString(delta_w + skTreeNode.prefix);
            if (skTreeNode.prefix.length() != rangeBitSize) {
                hash.append("----").append(skTreeNode.getChdVersionStr(keyword));
            }
            String encHash = StringXor.xor(delta_w_path, hash.toString());
            BQFMap.put(tau_w_path, encHash);
        }

        for (VKFNode chd : skTreeNode.childes) {
            encTreeKeyword(chd, keyword_tau_map, keyword_delta_map);
        }
    }
    private VKFNode buildTree(List<SpatialData> dataList) {
        VKFNode skTreeRoot = new VKFNode(minX, minY, maxX, maxY, dataList, keywordsSize, "");
        Queue<VKFNode> que = new LinkedList<VKFNode>();
        que.add(skTreeRoot);
        boolean isContinue = true;
        int level = 0;
        while (isContinue && !que.isEmpty()) {
//            System.out.println("第" + ++level + "层 :" + que.size());
            int s = que.size();
            isContinue = false;
            for (int i = 0; i < s; ++i) {
                VKFNode q = que.poll();
                int midX = q.minX + (q.maxX - q.minX) / 2;
                int midY = q.minY + (q.maxY - q.minY) / 2;
                if (q.childesData[0].size() > 0) {
                    if (q.childesData[0].size() > 1) {
                        isContinue = true;
                    }

                    q.childes[0] = new VKFNode(q.minX, q.minY, midX, midY, q.childesData[0], keywordsSize, q.prefix + "00");
                    que.add(q.childes[0]);
                }
                if (q.childesData[1].size() > 0) {
                    if (q.childesData[1].size() > 1) {
                        isContinue = true;
                    }

                    q.childes[1] = new VKFNode(q.minX, midY + 1, midX, q.maxY, q.childesData[1], keywordsSize, q.prefix + "01");
                    que.add(q.childes[1]);
                }
                if (q.childesData[2].size() > 0) {
                    if (q.childesData[2].size() > 1) {
                        isContinue = true;
                    }

                    q.childes[2] = new VKFNode(midX + 1, q.minY, q.maxX, midY, q.childesData[2], keywordsSize, q.prefix + "10");
                    que.add(q.childes[2]);
                }
                if (q.childesData[3].size() > 0) {
                    if (q.childesData[3].size() > 1) {
                        isContinue = true;
                    }

                    q.childes[3] = new VKFNode(midX + 1, midY + 1, q.maxX, q.maxY, q.childesData[3], keywordsSize, q.prefix + "11");
                    que.add(q.childes[3]);
                }
                q.childesData = null;
            }
            if (!isContinue) {
                rangeBitSize = que.peek().prefix.length();
            }
        }
        return skTreeRoot;
    }
    public List<SpatialData> readSpatialData(String path) throws Exception{
        int minX,minY,maxX,maxY;
        minX = minY = Integer.MAX_VALUE;
        maxX = maxY = Integer.MIN_VALUE;
        List<SpatialData> data = new ArrayList<>();
        File file = new File(path);
        if(file.isFile()&&file.exists()){
            InputStreamReader fla = new InputStreamReader(new FileInputStream(file));
            BufferedReader scr = new BufferedReader(fla);
            String str = null;
            while((str = scr.readLine()) != null){
                String[] s = str.split(" ");
                int id = Integer.parseInt(s[0]);
                int x = Integer.parseInt(s[1]);
                int y = Integer.parseInt(s[2]);
                minX = Math.min(x, minX);
                minY = Math.min(y, minY);
                maxX = Math.max(x, maxX);
                maxY = Math.max(y, maxY);
                List<Integer> keywords = new ArrayList<>();
                for(int i = 3;i < s.length;i++){
                    int keyword = Integer.parseInt(s[i]);
                    keywordsSize = Math.max(keywordsSize, keyword);
                    keywords.add(keyword - 1);//让关键字从0开始
                    keywordSet.add(keyword - 1);
                }
                data.add(new SpatialData(x, y, id, keywords));
            }
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            scr.close();
            fla.close();
        }
        return data;
    }
    public SearchToken getSearchToken(int[] point1, int[] point2, int[] keywords) {
        TokenNode tokenNode = getSearchToken(point1[0], point1[1], point2[0], point2[1], minX, minY, maxX, maxY, "");
        List<List<String>> tau_w_list = new ArrayList<>();
        List<List<String>> delta_w_list = new ArrayList<>();
        for (int k : keywords) {
            List<String> tau_w_version = new ArrayList<>();
            List<String> delta_w_version = new ArrayList<>();
            for (int i = 0; i <= tokenState.get(k); ++i) {
                tau_w_version.add(SHA.HASHDataToString(k1 + k + i));
                delta_w_version.add(SHA.HASHDataToString(k2 + k + i));
            }
            tau_w_list.add(tau_w_version);
            delta_w_list.add(delta_w_version);
        }
        return new SearchToken(tokenNode, tau_w_list, delta_w_list);
    }
    private TokenNode getSearchToken(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, String path) {
        if (x1 > x2 || y1 > y2) return null;
        TokenNode node = new TokenNode();
        if (x1 == x3 && x2 == x4 && y1 == y3 && y2 == y4) {
//            System.out.println(x1 + "," + y1 + "  " + x2 + "," + y2);
            return node;
        }
        int midX = x3 + (x4 - x3) / 2;
        int midY = y3 + (y4 - y3) / 2;

        int r = Math.abs((k3 + path).hashCode()) % 24;
        int[] mixedPath = mix[r];
        node.child[mixedPath[0]] = getSearchToken(x1, y1, Math.min(midX, x2), Math.min(midY, y2), x3, y3, midX, midY, path + chdFix[mixedPath[0]]);
        node.child[mixedPath[1]] = getSearchToken(x1, Math.max(midY + 1, y1), Math.min(midX, x2), y2, x3, midY + 1, midX, y4, path + chdFix[mixedPath[1]]);
        node.child[mixedPath[2]] = getSearchToken(Math.max(midX + 1, x1), y1, x2, Math.min(midY, y2), midX + 1, y3, x4, midY, path + chdFix[mixedPath[2]]);
        node.child[mixedPath[3]] = getSearchToken(Math.max(midX + 1, x1), Math.max(midY + 1, y1), x2, y2, midX + 1, midY + 1, x4, y4, path + chdFix[mixedPath[3]]);
        return node;
    }

    public HashMap<String, String> update(int x, int y, int keyword, int id) {
        tokenState.put(keyword, tokenState.get(keyword) + 1);
        String tau_w = SHA.HASHDataToString(k1 + keyword + tokenState.get(keyword));
        String delta_w = SHA.HASHDataToString(k2 + keyword + tokenState.get(keyword));
        HashMap<String, String> updMap = new HashMap<>();
        update(x, y, tau_w, delta_w, keyword, id, skTreeRoot, updMap, tokenState.get(keyword));
        rootHashes.put(SHA.HASHDataToString(k1 + keyword + 0), SHA.HASHDataToString(skTreeRoot.keywordNodeHash.get(keyword)));
        return updMap;
    }
    private void update(int x, int y, String tau_w, String delta_w, int keyword, int id, VKFNode node, HashMap<String, String> updMap, int version) {

//        System.out.println(node.prefix);

        if (node.prefix.length() == rangeBitSize) {//到达叶节点
            node.keywordNodeHash.put(keyword, String.valueOf(id));//叶节点只保存相应的id，没有孩子哈希值
            String tau_w_path = SHA.HASHDataToString(tau_w + node.prefix);
            String delta_w_path = SHA.HASHDataToString(delta_w + node.prefix);
            String encHash = StringXor.xor(delta_w_path, String.valueOf(id));
            updMap.put(tau_w_path, encHash);
            return;
        }
        int midX = node.minX + (node.maxX - node.minX) / 2;
        int midY = node.minY + (node.maxY - node.minY) / 2;
        int r = Math.abs((k3 + node.prefix).hashCode()) % 24;
        int[] mixedPath = mix[r];

        String[] chdHash = null;
        if (node.keywordNodeHash.containsKey(keyword)) chdHash = node.keywordNodeHash.get(keyword).split(";;");
        else chdHash = new String[4];
        for (int c = 0; c < 4; ++c) {
            int chd_minX = 0, chd_minY = 0, chd_maxX = 0, chd_maxY = 0;
            switch (c) {
                case 0: chd_minX = node.minX; chd_minY = node.minY; chd_maxX = midX; chd_maxY = midY;
                    break;
                case 1: chd_minX = node.minX; chd_minY = midY + 1; chd_maxX = midX; chd_maxY = node.maxY;
                    break;
                case 2: chd_minX = midX + 1; chd_minY = node.minY; chd_maxX = node.maxX; chd_maxY = midY;
                    break;
                case 3: chd_minX = midX + 1; chd_minY = midY + 1; chd_maxX = node.maxX; chd_maxY = node.maxY;
                    break;
            }
            if (x >= chd_minX && x <= chd_maxX && y >= chd_minY && y <= chd_maxY) {
                if (node.childes[mixedPath[c]] == null) {
                    node.childes[mixedPath[c]] = new VKFNode(chd_minX, chd_minY, chd_maxX, chd_maxY, node.prefix + chdFix[mixedPath[c]]);
                }
                //更改关键字树的孩子节点版本号
                if (!node.keyword_chdVersion_map.containsKey(keyword)) {//节点不包含该关键字
                    node.addKeyword(keyword, version);
                } else {
                    node.setKeywordVersion(keyword, mixedPath[c], version);
                }
                update(x, y, tau_w, delta_w, keyword, id, node.childes[mixedPath[c]], updMap, version);//更新子节点
                chdHash[mixedPath[c]] = SHA.HASHDataToString(node.childes[mixedPath[c]].keywordNodeHash.get(keyword));//更新当前节点哈希值
            }
            if (chdHash[mixedPath[c]] == null) chdHash[mixedPath[c]] = SHA.HASHDataToString("");
        }
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            hash.append(chdHash[i]);
            if (i != 3) hash.append(";;");
        }
        node.keywordNodeHash.put(keyword, hash.toString());
        if (node.prefix.length() != rangeBitSize) {
            hash.append("----").append(node.getChdVersionStr(keyword));
        }
        String tau_w_path = SHA.HASHDataToString(tau_w + node.prefix);
        String delta_w_path = SHA.HASHDataToString(delta_w + node.prefix);
        String encHash = StringXor.xor(delta_w_path, hash.toString());
        updMap.put(tau_w_path, encHash);
    }

    public int[][] getSearchRange(double per) {
        long area = (long) (maxX - minX) * (maxY - minY);
        int dis = (int) (Math.sqrt(area * per) + 0.5);
        return new int[][]{new int[]{minX, minY}, new int[]{minX + dis, minY + dis}};
    }

    public int[] getSearchKeyword(int num) {
        int[] keywords = new int[num];
        HashSet<Integer> st = new HashSet<>();
        for (int i = 0; i < num; ++i) {
            int keyword = (int) (Math.random() * keywordsSize);
            while (st.contains(keyword) || !keywordSet.contains(keyword)) {
                keyword = (int) (Math.random() * keywordsSize);
            }
            st.add(keyword);
            keywords[i] = keyword;
        }
        return keywords;
    }
}
