package dataowner;

import it.unisa.dia.gas.jpbc.Element;

import java.io.Serializable;

public class SigObj implements Serializable {
    public String chdVersion;//包括四个孩子节点的版本号
    public Element[] sig;

    public SigObj(String chdVersion, Element[] sig) {
        this.chdVersion = chdVersion;
        this.sig = sig;
    }
}
