package dataowner;

import java.util.*;

public class VKFNode {
    public HashMap<Integer, int[]> keyword_chdVersion_map = new HashMap<>();
    SpatialData data = null;
    public int minX,minY,maxX,maxY;
    public VKFNode[] childes = new VKFNode[4];
    public String prefix;
    public int dataSize;
    public List<SpatialData>[] childesData = new List[4];
//    public HashSet<Integer> keywordSet = new HashSet<>();
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
            for (int keyword : data.keywords) {
                if (!keyword_chdVersion_map.containsKey((keyword))) {
                    keyword_chdVersion_map.put(keyword, new int[]{0,0,0,0});
                }
            }
            if (data.x <= midX && data.y <= midY) childesData[0].add(data);
            else if (data.x <= midX) childesData[1].add(data);
            else if (data.y <= midY) childesData[2].add(data);
            else childesData[3].add(data);
        }
    }
    public VKFNode(int minX, int minY, int maxX, int maxY, String prefix) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.prefix = prefix;
    }
    public void addKeyword(int keyword) {
        if (!keyword_chdVersion_map.containsKey(keyword)) {
            keyword_chdVersion_map.put(keyword, new int[] {0,0,0,0});
        }
    }

    public void setKeywordVersion(int keyword, int pos, int version) {
        keyword_chdVersion_map.get(keyword)[pos] = version;
    }

    public String getChdVersionStr(int keyword) {
        int[] chdVersion = keyword_chdVersion_map.get(keyword);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            builder.append(chdVersion[i]);
            if (i != 3) builder.append(';');
        }
        return builder.toString();
    }
}
