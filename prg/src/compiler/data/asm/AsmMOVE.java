package compiler.data.asm;

import compiler.data.imc;

public class AsmMOVE extends AsmInst{

    public AsmMOVE(String mnemonic,String assem,TEMP def, TEMP use){
        super(mnemonic,assem,null,null,null);
        defs.add(def);
        uses.add(use);
    }
}
