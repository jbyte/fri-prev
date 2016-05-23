package compiler.data.asm;

import java.util.*;

import compiler.data.imc.*;

public class AsmOPER extends AsmInst{

    public AsmOPER(String mnemonic,String assem,LinkedList<TEMP> defs,
            LinkedList<TEMP> uses, LinkedList<LABEL> labels){
        super(mnemonic,assem,defs,uses,labels);
    }
    public AsmOPER(String mnemonic,String assem,LinkedList<TEMP> defs,LinkedList<TEMP> uses){
        super(mnemonic,assem,defs,uses,null);
    }
    public AsmOPER(String mnemonic,String assem){
        super(mnemonic,assem,null,null,null);
    }
}
