package compiler.data.asm;

import compiler.data.imc.*;

public class AsmLABEL extends AsmInst{

    public AsmLABEL(String assem, LABEL label){
        super(assem,"",null,null,null);
        labels.add(label);
    }
}
