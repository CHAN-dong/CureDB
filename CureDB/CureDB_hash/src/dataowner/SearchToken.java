package dataowner;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

import java.util.List;

public class SearchToken {
    public TokenNode tokenNode;
    public List<List<String>> tau_w_list;
    public List<List<String>> delta_w_list;
    public double getTokenSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        double objectSize = (ObjectSizeCalculator.getObjectSize(tokenNode) + ObjectSizeCalculator.getObjectSize(tau_w_list) + ObjectSizeCalculator.getObjectSize(delta_w_list)) / 8.0 / 1024 / 1024;
        return objectSize;
    }
    public SearchToken(TokenNode tokenNode, List<List<String>> tau_w_list, List<List<String>> delta_w_list) {
        this.tokenNode = tokenNode;
        this.tau_w_list = tau_w_list;
        this.delta_w_list = delta_w_list;
    }

    private String getTokenNodeStr(TokenNode node) {
        StringBuilder str = new StringBuilder();
        str.append("<");
        for (int i = 0; i < 4; ++i) {
            if (node.child[i] != null) {
                String sub = getTokenNodeStr(node.child[i]);
                str.append(sub);
            }
            if (i != 3) str.append(";");
        }
        if (str.length() == 4) {
            return "<>";
        }
        str.append(">");
        return str.toString();
    }


    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(getTokenNodeStr(tokenNode));
        str.append("\n");
//        for (List<String> tau_w_version : tau_w_list) {
//            for (String tau_w : tau_w_version)
//                str.append(tau_w).append(";");
//        }
//        str.append("\n");
//        for (List<String> delta_w_version : delta_w_list)
//            for (String delta_w : delta_w_version)
//                str.append(delta_w).append(";");
        return str.toString();
    }

}
