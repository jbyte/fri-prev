package compiler.data.asm;

import java.util.*;

import compiler.data.imc.*;

public class InterferenceNode{
    public TEMP tmp;
    public LinkedList<InterferenceNode> edges;

    public InterferenceNode(TEMP tmp){
        this.tmp = tmp;
        edges = new LinkedList<InterferenceNode>();
    }
}
