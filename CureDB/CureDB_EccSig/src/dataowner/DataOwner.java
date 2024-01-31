package dataowner;

import it.unisa.dia.gas.jpbc.Element;
import server.SearchRes;
import signature.MySigBasedECC;
import tools.SHA;
import tools.StringXor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

import static signature.MySigBasedECC.sign;
import static signature.MySigBasedECC.verify;

public class DataOwner {
    public HashSet<Integer> keywordSet = new HashSet<>();
    MySigBasedECC eccSig;
    String[] chdFix = new String[]{"00", "01", "10", "11"};
    int rangeBitSize;
    public int keywordsSize;
    public int minX,minY,maxX,maxY;
    VKFNode skTreeRoot;
    int[][] mix = new int[][] {{0,1,2,3}, {0,1,3,2}, {0,2,1,3}, {0,2,3,1}, {0,3,1,2}, {0,3,2,1}, {1,0,2,3}, {1,0,3,2}, {1,2,0,3}, {1,2,3,0}, {1,3,0,2}, {1,3,2,0}, {2,0,1,3}, {2,0,3,1}, {2,1,0,3}, {2,1,3,0}, {2,3,0,1}, {2,3,1,0}, {3,0,1,2}, {3,0,2,1}, {3,1,0,2}, {3,1,2,0}, {3,2,0,1}, {3,2,1,0}};
//    HashMap<String, Integer> path_index_map = new HashMap<>();
    String k1 = "10110011";
    String k2 = "10110010";
    String k3 = "10110000";
    HashMap<String, SigObj> BQFMap = new HashMap<>();
    HashMap<String, SigObj> path_id_map = new HashMap<>();
    HashMap<Integer, Integer> tokenState = new HashMap<>();
    public MySigBasedECC getRsaSig() {return eccSig;}
    public HashMap<String, SigObj> getBQFMap() {
        return BQFMap;
    }
    public int getRangeBitSize() {return rangeBitSize;}
    public DataOwner(String path, int rsaBitSize) throws Exception {
        eccSig = new MySigBasedECC();
        //读数据
        List<SpatialData> dataList = readSpatialData(path);
        //获取明文树
        skTreeRoot = buildTree(dataList);
        //加密范围
        encTreePrefix(skTreeRoot, "");

        //加密关键字
        HashMap<Integer, String> keyword_tau_map = new HashMap<>();
        HashMap<Integer, String> keyword_delta_map = new HashMap<>();

        for (int keyword : keywordSet) {

//        for (int keyword = 0; keyword < keywordsSize; ++keyword) {
            tokenState.put(keyword, 1);
            String tau_w = SHA.HASHDataToString(k1 + keyword + 1);
            String delta_w = SHA.HASHDataToString(k2 + keyword + 1);
            keyword_tau_map.put(keyword, tau_w);
            keyword_delta_map.put(keyword, delta_w);
        }
        encTreeKeyword(skTreeRoot, keyword_tau_map, keyword_delta_map);
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
        if (path.length() != rangeBitSize) {
            for (Map.Entry<Integer, int[]> entry : skTreeNode.keyword_chdVersion_map.entrySet()) {
                int keyword = entry.getKey();
                int[] chdVersion = entry.getValue();
                for (int c = 0; c < 4; ++c) {
                    if (skTreeNode.childes[c] != null && skTreeNode.childes[c].keyword_chdVersion_map.containsKey(keyword)) chdVersion[c] = 1;
                }
            }
        }
    }
    static class Tag {
        public int resTag = 0;//标记resList下标
        public int voPathTag = 0;//标记path下标
    };
    public boolean verifyRes(SearchToken searchToken, SearchRes searchRes) {
        List<String> messageList = new ArrayList<>();
        Tag tag = new Tag();
        int n = searchToken.tau_w_list.size();
        int[] version = new int[n];
        for (int i = 0; i < n; ++i) {
            version[i] = searchToken.tau_w_list.get(i).size();
        }
        getVOMessage_and(version, searchToken.tokenNode, searchToken.delta_w_list, messageList, searchRes.res, searchRes.voPath, "", tag);
        boolean res = verify(searchRes.aggSig, messageList);
//        if (res == false) {
//            int a = 1;
//        }
        return res;
    }
    //voPath: <0;0;0;0-2;1;0;<0;0;0;0-2;<>;0;1>>，其中<>为叶节点
    private void getVOMessage_and(int[] version, TokenNode tokenNode, List<List<String>> delta_w_list, List<String> messageList, List<Integer> resList, String voPath, String path, Tag tag) {
        //tokenNode 为空表示需要遍历该孩子节点
        if (tokenNode != null && tokenNode.child[0] == null && tokenNode.child[1] == null && tokenNode.child[2] == null && tokenNode.child[3] == null) {
            tokenNode = null;
        }
        tag.voPathTag++;//<
        int n = delta_w_list.size();
        if (voPath.charAt(tag.voPathTag) == '>') {//表示是<>，为叶节点
            int id = resList.get(tag.resTag);
            tag.resTag += 1;
            messageList.add(path + id);
            tag.voPathTag++;//>
        } else {
            //获取该节点处的孩子版本信息
            String[] chdVersionStr = new String[n];
            int[][] chdVersion = new int[4][n];
            for (int i = 0; i < n; ++i) {
                tag.voPathTag++;//(
                int start = tag.voPathTag;
                for (int c = 0; c < 4; ++c) {
                    int nextTag = tag.voPathTag;
                    while (voPath.charAt(nextTag) != ';' && voPath.charAt(nextTag) != ')') nextTag++;
                    chdVersion[c][i] = Integer.parseInt(voPath.substring(tag.voPathTag, nextTag));
                    tag.voPathTag = nextTag + 1;//)
                }
                chdVersionStr[i] = voPath.substring(start, tag.voPathTag - 1);
            }

            //加入在这一层的所有关键字的存在性证明
            for (int i = 0; i < n; ++i) {
                messageList.add(delta_w_list.get(i).get(version[i] - 1) + path + chdVersionStr[i]);
            }

            for (int c = 0; c < 4; ++c) {
                if (tokenNode != null && tokenNode.child[c] == null) {//如果这个孩子没有被查询则跳过
                    tag.voPathTag += 1;//;或者 >
                    continue;
                }
                boolean isContinue = true;
                for (int i = 0; i < n; ++i) {
                    if (chdVersion[c][i] == 0) {
                        isContinue = false;
                        tag.voPathTag += 1;//;或者 >
                        break;
                    }
                }
                if (isContinue && voPath.charAt(tag.voPathTag) == '<') {//表示该孩子需要遍历
                    TokenNode subTokenNode = (tokenNode == null ? null : tokenNode.child[c]);
                    getVOMessage_and(chdVersion[c], subTokenNode, delta_w_list, messageList, resList, voPath, path + chdFix[c], tag);
                    tag.voPathTag += 1;//，或者 >
                }
            }
        }
    }
    //VO:  <,,1`2<,,<0>,>,>  其中1`2表示在该路径下不存在第1和2关键字，<0>表示叶节点存在0关键字
    private void getVOMessage_or(TokenNode tokenNode, List<String> delta_w_list, List<String> messageList, List<Integer> resList, String voPath, String path, Tag tag) {
        //tokenNode 为空表示需要遍历该孩子节点
        if (tokenNode != null && tokenNode.child[0] == null && tokenNode.child[1] == null && tokenNode.child[2] == null && tokenNode.child[3] == null) {
            tokenNode = null;
        }

        List<String> tmpDelta_w_list = new ArrayList<>(delta_w_list);
        //获取不存在的关键字信息
        while (voPath.charAt(tag.voPathTag) != '<') {
            int nexTag = tag.voPathTag;
            while (voPath.charAt(nexTag) != '`' && voPath.charAt(nexTag) != '<') nexTag++;
            int keywordTag = Integer.parseInt(voPath.substring(tag.voPathTag, nexTag));
            messageList.add(delta_w_list.get(keywordTag) + path + 0);
            tag.voPathTag = nexTag;
            if (voPath.charAt(tag.voPathTag) == '`') tag.voPathTag++;
            tmpDelta_w_list.set(keywordTag, null);
        }

        tag.voPathTag++;//<
        if (path.length() == rangeBitSize) {//叶节点
            //获取存在的关键字
            int nexTag = tag.voPathTag;
            while (voPath.charAt(nexTag) != '>') nexTag++;
            int keywordTag = Integer.parseInt(voPath.substring(tag.voPathTag, nexTag));
            messageList.add(delta_w_list.get(keywordTag) + path + 1);
            tag.voPathTag = nexTag;
            //加入查询结果id信息
            messageList.add(resList.get(tag.resTag) + path);
            tag.resTag += 1;
            tag.voPathTag++;//>
        } else {
            for (int c = 0; c < 4; ++c) {
                if (tokenNode != null && tokenNode.child[c] == null) {//如果这个孩子没有被查询则跳过
                    tag.voPathTag += 1;//，或者 >
                    continue;
                }
                TokenNode subTokenNode = (tokenNode == null ? null : tokenNode.child[c]);
                if (voPath.charAt(tag.voPathTag) == '<') {//表示该孩子需要遍历
                    getVOMessage_or(subTokenNode, tmpDelta_w_list, messageList, resList, voPath, path + chdFix[c], tag);
                    tag.voPathTag += 1;//，或者 >
                } else {
                    int nextTag = tag.voPathTag;
                    while (voPath.charAt(nextTag) != ',' && voPath.charAt(nextTag) != '>') nextTag++;
                    if (nextTag == tag.voPathTag) {//说明这个孩子节点所有的关键字都不满足
                        for (String delta_w : tmpDelta_w_list) {
                            if (delta_w != null) messageList.add(delta_w + (path + chdFix[c]) + 0);
                        }
                    } else {
                        getVOMessage_or(subTokenNode, tmpDelta_w_list, messageList, resList, voPath, path + chdFix[c], tag);
                    }
                    System.out.println(voPath.charAt(tag.voPathTag));
                    ++tag.voPathTag;//，或者 >
                }
            }
        }
    }
    private void getVOMessage_not(TokenNode tokenNode, List<String> delta_w_list, List<String> messageList, List<Integer> resList, String voPath, String path, Tag tag) {
        //tokenNode 为空表示需要遍历该孩子节点
        if (tokenNode != null && tokenNode.child[0] == null && tokenNode.child[1] == null && tokenNode.child[2] == null && tokenNode.child[3] == null) {
            tokenNode = null;
        }

        List<String> tmpDelta_w_list = new ArrayList<>(delta_w_list);
        //获取不存在的关键字信息
        while (voPath.charAt(tag.voPathTag) != '<') {
            int nexTag = tag.voPathTag;
            while (voPath.charAt(nexTag) != '`' && voPath.charAt(nexTag) != '<') nexTag++;
            int keywordTag = Integer.parseInt(voPath.substring(tag.voPathTag, nexTag));
            messageList.add(delta_w_list.get(keywordTag) + path + 0);
            tag.voPathTag = nexTag + 1;
            tmpDelta_w_list.set(keywordTag, null);
        }

        tag.voPathTag++;//<
        if (path.length() == rangeBitSize) {//叶节点
            //获取存在的关键字
            int nexTag = tag.voPathTag;
            while (voPath.charAt(nexTag) != '>') nexTag++;
            int keywordTag = Integer.parseInt(voPath.substring(tag.voPathTag, nexTag));
            messageList.add(delta_w_list.get(keywordTag) + path + 1);
            tag.voPathTag = nexTag;
            //加入查询结果id信息
            messageList.add(resList.get(tag.resTag) + path);
            tag.resTag += 1;
            for (String delta_w : delta_w_list) {
                messageList.add(delta_w + path + 1);
            }
            tag.voPathTag++;//>
        } else {
            for (int c = 0; c < 4; ++c) {
                if (tokenNode != null && tokenNode.child[c] == null) {//如果这个孩子没有被查询则跳过
                    tag.voPathTag += 1;//，或者 >
                    continue;
                }
                if (voPath.charAt(tag.voPathTag) == '<') {//表示该孩子需要遍历
                    TokenNode subTokenNode = (tokenNode == null ? null : tokenNode.child[c]);
                    getVOMessage_or(subTokenNode, tmpDelta_w_list, messageList, resList, voPath, path + chdFix[c], tag);
                    tag.voPathTag += 1;//，或者 >
                } else {
                    int nextTag = tag.voPathTag;
                    while (voPath.charAt(nextTag) != ',' && voPath.charAt(nextTag) != '>') nextTag++;
                    int notExistTag = Integer.parseInt(voPath.substring(tag.voPathTag, nextTag));
                    messageList.add(delta_w_list.get(notExistTag) + (path + chdFix[c]) + 0);
                    tag.voPathTag = nextTag + 1;//，或者 >
                }
            }
        }
    }
    //非叶节点存储的是不存在证明，叶节点存储的是存在性证明

    private void encTreeKeyword(VKFNode skTreeNode, HashMap<Integer, String> keyword_tau_map, HashMap<Integer, String> keyword_delta_map) {
        if (skTreeNode == null) return;
        if (skTreeNode.prefix.length() == rangeBitSize) {
            Element[] sig = sign(skTreeNode.prefix + skTreeNode.data.id);
            path_id_map.put(skTreeNode.prefix, new SigObj(String.valueOf(skTreeNode.data.id), sig));
            return;
        }
        for (int keyword : skTreeNode.keyword_chdVersion_map.keySet()) {
            String tau_w = keyword_tau_map.get(keyword);
            String delta_w = keyword_delta_map.get(keyword);
            String tau_w_path = SHA.HASHDataToString(tau_w + skTreeNode.prefix);
            String delta_w_path = SHA.HASHDataToString(delta_w + skTreeNode.prefix);
            String chdVersion = skTreeNode.getChdVersionStr(keyword);
            String message = delta_w + skTreeNode.prefix + chdVersion;
            Element[] sig = sign(message);
            BQFMap.put(tau_w_path, new SigObj(StringXor.xor(delta_w_path, chdVersion), sig));
        }
        for (VKFNode chd : skTreeNode.childes) encTreeKeyword(chd, keyword_tau_map, keyword_delta_map);
    }

    private VKFNode buildTree(List<SpatialData> dataList) {
        VKFNode skTreeRoot = new VKFNode(minX, minY, maxX, maxY, dataList, keywordsSize, "");
        Queue<VKFNode> que = new LinkedList<VKFNode>();
        que.add(skTreeRoot);
        boolean isContinue = true;
        while (isContinue && !que.isEmpty()) {
            int s = que.size();
            isContinue = false;
            for (int i = 0; i < s; ++i) {
                VKFNode q = que.poll();
                int midX = q.minX + (q.maxX - q.minX) / 2;
                int midY = q.minY + (q.maxY - q.minY) / 2;
                if (q.childesData[0].size() > 0) {
                    if (q.childesData[0].size() > 1) isContinue = true;
                    q.childes[0] = new VKFNode(q.minX, q.minY, midX, midY, q.childesData[0], keywordsSize, q.prefix + "00");
                    que.add(q.childes[0]);
                }
                if (q.childesData[1].size() > 0) {
                    if (q.childesData[1].size() > 1) isContinue = true;
                    q.childes[1] = new VKFNode(q.minX, midY + 1, midX, q.maxY, q.childesData[1], keywordsSize, q.prefix + "01");
                    que.add(q.childes[1]);
                }
                if (q.childesData[2].size() > 0) {
                    if (q.childesData[2].size() > 1) isContinue = true;
                    q.childes[2] = new VKFNode(midX + 1, q.minY, q.maxX, midY, q.childesData[2], keywordsSize, q.prefix + "10");
                    que.add(q.childes[2]);
                }
                if (q.childesData[3].size() > 0) {
                    if (q.childesData[3].size() > 1) isContinue = true;
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
    public SearchToken getSearchToken(int[] point1, int[] point2, int[] keywords, int type) {
        TokenNode tokenNode = getSearchToken(point1[0], point1[1], point2[0], point2[1], minX, minY, maxX, maxY, "");
        List<List<String>> tau_w_list = new ArrayList<>();
        List<List<String>> delta_w_list = new ArrayList<>();
        for (int k : keywords) {
            List<String> tau_w_version = new ArrayList<>();
            List<String> delta_w_version = new ArrayList<>();
            for (int i = 1; i <= tokenState.get(k); ++i) {
                tau_w_version.add(SHA.HASHDataToString(k1 + k + i));
                delta_w_version.add(SHA.HASHDataToString(k2 + k + i));
            }
            tau_w_list.add(tau_w_version);
            delta_w_list.add(delta_w_version);
        }
        return new SearchToken(tokenNode, tau_w_list, delta_w_list, type);
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

    public HashMap<String, SigObj> update(int x, int y, int keyword, int id) {
        tokenState.put(keyword, tokenState.get(keyword) + 1);
        String tau_w = SHA.HASHDataToString(k1 + keyword + tokenState.get(keyword));
        String delta_w = SHA.HASHDataToString(k2 + keyword + tokenState.get(keyword));
        HashMap<String, SigObj> updMap = new HashMap<>();
        update(x, y, tau_w, delta_w, keyword, id, skTreeRoot, updMap, tokenState.get(keyword));
        return updMap;
    }
    private void update(int x, int y, String tau_w, String delta_w, int keyword, int id, VKFNode node, HashMap<String, SigObj> updMap, int version) {
        if (node.prefix.length() == rangeBitSize) {//到达叶节点
            return;
        }

        int midX = node.minX + (node.maxX - node.minX) / 2;
        int midY = node.minY + (node.maxY - node.minY) / 2;
        int r = Math.abs((k3 + node.prefix).hashCode()) % 24;
        int[] mixedPath = mix[r];

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
                    node.addKeyword(keyword);
                }
                node.setKeywordVersion(keyword, mixedPath[c], version);
                update(x, y, tau_w, delta_w, keyword, id, node.childes[mixedPath[c]], updMap, version);//更新子节点
                break;
            }
        }
        String tau_w_path = SHA.HASHDataToString(tau_w + node.prefix);
        String delta_w_path = SHA.HASHDataToString(delta_w + node.prefix);
        String chdVersion = node.getChdVersionStr(keyword);
        String message = delta_w + node.prefix + chdVersion;
        Element[] sig = sign(message);
        updMap.put(tau_w_path, new SigObj(StringXor.xor(delta_w_path, chdVersion), sig));
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

    public HashMap<String, SigObj> getPath_id_map() {
        return path_id_map;
    }
}
