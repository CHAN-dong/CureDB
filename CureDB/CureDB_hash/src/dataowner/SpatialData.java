package dataowner;

import java.util.List;

public class SpatialData {
    public int x;
    public int y;
    public List<Integer> keywords;
    int id;

    public SpatialData(int x, int y, int id, List<Integer> keywords) {
        this.x = x;
        this.y = y;
        this.keywords = keywords;
        this.id = id;
    }
}
