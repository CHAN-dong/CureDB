package dataowner;

import java.util.*;

public class VKFNode {
    SpatialData data = null;
    public int minX,minY,maxX,maxY;
    public VKFNode[] childes = new VKFNode[4];
    public String prefix;
    public int dataSize;
    public List<SpatialData>[] childesData = new List[4];
    public HashMap<Integer, String> keywordNodeHash = new HashMap<>();
    public HashMap<Integer, List<Integer>> keyword_chdVersion_map = new HashMap<>();

    public void addKeywordHash(int keyword, String hash) {
        keywordNodeHash.put(keyword, hash);
    }

    public VKFNode(int minX, int minY, int maxX, int maxY, String prefix) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.prefix = prefix;
    }

    public VKFNode(int minX, int minY, int maxX, int maxY, List<SpatialData> dataList, int keywordsSize, String prefix) {
        dataSize = dataList.size();
        if (dataSize == 1) data = dataList.get(0);
        childesData[0] = new ArrayList<>();
        childesData[1] = new ArrayList<>();
        childesData[2] = new ArrayList<>();
        childesData[3] = new ArrayList<>();
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.prefix = prefix;
        int midX = minX + (maxX - minX) / 2;
        int midY = minY + (maxY - minY) / 2;
        for (SpatialData data : dataList) {
            if (data.x <= midX && data.y <= midY) childesData[0].add(data);
            else if (data.x <= midX) childesData[1].add(data);
            else if (data.y <= midY) childesData[2].add(data);
            else childesData[3].add(data);
        }
    }
    public void addKeyword(int keyword, int version) {
        if (!keyword_chdVersion_map.containsKey(keyword)) {
            keyword_chdVersion_map.put(keyword, Arrays.asList(version, version, version, version));
        }
    }
    public String getChdVersionStr(int keyword) {
        if (!keyword_chdVersion_map.containsKey(keyword)) keyword_chdVersion_map.put(keyword, Arrays.asList(0, 0, 0, 0));
        List<Integer> chdVersion = keyword_chdVersion_map.get(keyword);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            builder.append(chdVersion.get(i));
            if (i != 3) builder.append(';');
        }
        return builder.toString();
    }

    public void setKeywordVersion(int keyword, int pos, int version) {
        keyword_chdVersion_map.get(keyword).set(pos, version);
    }
}
