package compiler.data.asm;

import java.util.*;

import compiler.data.imc.*;

public class InterferenceNode{
    public static final int POTENTIAL_SPILL = 1;
    public static final int ACTUAL_SPILL = 2;

    public TEMP tmp;
    public LinkedList<InterferenceNode> edges;

    public int reg;
    public int spill;

    public InterferenceNode(TEMP tmp){
        this.tmp = tmp;
        edges = new LinkedList<InterferenceNode>();

        reg = 0;
        spill = 0;
    }
}
