package server;

import java.io.Serializable;

public class VOTreeNode implements Serializable {
    public String[] childHash;
    public VOTreeNode[] child;
}
