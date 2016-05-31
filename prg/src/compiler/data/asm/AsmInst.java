package compiler.data.asm;

import java.util.*;
import java.util.regex.*;

import compiler.data.imc.*;
import compiler.data.frg.*;

public abstract class AsmInst{
    public String mnemonic;
    public String assem;

    public LinkedList<TEMP> uses;

    public LinkedList<TEMP> defs;

    public LinkedList<LABEL> labels;

    public LinkedList<TEMP> in;
    public LinkedList<TEMP> out;

    /**
     * Description of the machine code
     *
     * @param assem
     *          Character representation of the instruction
     * @param defs
     *          Temporary variables (defined)
     * @param uses
     *          Temporary variables (used)
     * @param labels
     *          Labels
     *
     */
    public AsmInst(String mnemonic, String assem, LinkedList<TEMP> defs,
            LinkedList<TEMP> uses, LinkedList<LABEL> labels){
        this.mnemonic = mnemonic;
        this.assem = assem;
        this.defs = defs == null ? new LinkedList<TEMP>() : defs;
        this.uses = uses == null ? new LinkedList<TEMP>() : uses;
        this.labels = labels == null ? new LinkedList<LABEL>() : labels;

        in = null;
        out = null;
    }

    public String format(HashMap<TEMP,String> map){
        String str = String.format("%-5s %s", mnemonic, assem);
        for(int i=0; i<uses.size(); i++){
            TEMP tmp = uses.get(i);
            String reg = null;
            if(map != null) reg = map.get(tmp);
            else reg = "T"+tmp.name;
            if(reg==null){
                for(TEMP t : map.keySet()){
                    if(t.equals(tmp)){
                        reg = map.get(t);
                        break;
                    }
                }
                //reg = "T"+tmp.name;
            }
            str = str.replaceAll("`s"+i,Matcher.quoteReplacement(reg));
        }
        for(int i=0; i<defs.size(); i++){
            TEMP tmp = defs.get(i);
            String reg = null;
            if(map != null) reg = map.get(tmp);
            else reg = "T"+tmp.name;
            if(reg==null){
                for(TEMP t : map.keySet()){
                    if(t.equals(tmp)){
                        reg = map.get(t);
                        break;
                    }
                }
                //reg = "T"+tmp.name;
            }
            str = str.replaceAll("`d"+i,Matcher.quoteReplacement(reg));
        }
        for(int i=0; i<labels.size(); i++){
            LABEL lab = labels.get(i);
            str = str.replaceAll("`l"+i,lab.label);
        }
        return str;
    }
}
