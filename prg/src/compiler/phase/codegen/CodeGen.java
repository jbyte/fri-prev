package compiler.phase.codegen;

import java.util.*;

import compiler.*;
import compiler.common.report.*;
import compiler.phase.*;

import compiler.data.frg.*;
import compiler.data.imc.*;
import compiler.data.asm.*;

public class CodeGen extends Phase{

    private Task task;
    private CodeFragment frag;

    public CodeGen(Task task){
        super(task,"codegen");

        this.task = task;
    }

    public void generate(){
        for(Fragment tmp : task.fragments.values()){
            if(tmp instanceof CodeFragment){
                frag = (CodeFragment)tmp;
                frag.asmcode = new LinkedList<AsmInst>(Arrays.asList(new AsmLABEL("`l0",
                                new LABEL(frag.frame.label))));

                for(IMCStmt stm : ((STMTS)frag.linCode).stmts()){
                    parse(stm);
                }
            }
        }
        optimize();
    }

    private void parse(IMCStmt stm){
        LinkedList<TEMP> uses = new LinkedList<TEMP>();
        LinkedList<LABEL> labels = new LinkedList<LABEL>();

        if(stm instanceof MOVE){
            MOVE move = (MOVE)stm;
            if(move.dst instanceof MEM){
                uses.add(parse(move.src));
                uses.add(parse((MEM)move.dst));
                frag.asmcode.add(new AsmOPER("STO","`s0,`s1,0",null,uses));
            }
            if(move.dst instanceof TEMP){
                frag.asmcode.add(new AsmMOVE("SET","`d0,`d1",parse(move.dst),parse(move.src)));
            }
        }else if(stm instanceof CJUMP){
            uses.add(parse(((CJUMP)stm).cond));
            labels.add(new LABEL(((CJUMP)stm).posLabel));
            frag.asmcode.add(new AsmOPER("BNZ","`s0,`l0",null,uses,labels));
        }else if(stm instanceof JUMP){
            labels.add(new LABEL(((JUMP)stm).label));
            frag.asmcode.add(new AsmOPER("JMP","`l0",null,null,labels));
        }else if(stm instanceof LABEL){
            frag.asmcode.add(new AsmLABEL("`l0",(LABEL)stm));
        }
    }

    private TEMP parse(IMCExpr expr){
        TEMP tmp = null;
        //System.out.println(expr);

        LinkedList<TEMP> defs = new LinkedList<TEMP>();
        LinkedList<TEMP> uses = new LinkedList<TEMP>();
        LinkedList<LABEL> labels = new LinkedList<LABEL>();

        if(expr instanceof BINOP){
            BINOP binop = (BINOP)expr;
            String oper = null;

            switch(binop.oper){
                case OR:
                    oper = "OR";
                    break;
                case AND:
                    oper = "AND";
                    break;
                case ADD:
                    oper = "ADD";
                    break;
                case SUB:
                    oper = "SUB";
                    break;
                case MUL:
                    oper = "MUL";
                    break;
                case DIV:
                    oper = "DIV";
                    break;
                case NEQ:
                case EQU:
                case LTH:
                case GTH:
                case LEQ:
                case GEQ:
                    oper = "CMP";
                    break;
            }

            defs.add(tmp = new TEMP(TEMP.newTempName()));
            uses.add(parse(binop.expr1));
            uses.add(parse(binop.expr2));

            frag.asmcode.add(new AsmOPER(oper,"`d0,`s0,`s1",defs,uses));

            if(oper.equals("CMP")){
                switch(binop.oper){
                    case EQU:
                        frag.asmcode.add(new AsmOPER("ZSZ","`d0,`s0,1",defs,defs));
                        break;
                    case NEQ:
                        frag.asmcode.add(new AsmOPER("ZSNZ","`d0,`s0,1",defs,defs));
                        break;
                    case LTH:
                        frag.asmcode.add(new AsmOPER("ZSN","`d0,`s0,1",defs,defs));
                        break;
                    case GTH:
                        frag.asmcode.add(new AsmOPER("ZSP","`d0,`s0,1",defs,defs));
                        break;
                    case LEQ:
                        frag.asmcode.add(new AsmOPER("ZSNP","`d0,`s0,1",defs,defs));
                        break;
                    case GEQ:
                        frag.asmcode.add(new AsmOPER("ZSNN","`d0,`s0,1",defs,defs));
                        break;
                }
            }
        }

        if(expr instanceof TEMP){
            tmp = (TEMP)expr;
        }

        if(expr instanceof CONST){
            long constant = ((CONST)expr).value;
            long value = Math.abs(constant);

            defs.add(tmp = new TEMP(TEMP.newTempName()));

            if(constant<0 && value <= 0xFFL){
                frag.asmcode.add(new AsmOPER("NEG","`d0,0,"+value,defs,null));
                return tmp;
            }

            frag.asmcode.add(new AsmOPER("SET","`d0,"+(value & 0xFFFFL),defs,null));

            LinkedList<TEMP> setDef = new LinkedList<TEMP>();

            if(value>0xFFFFL){
                setDef.add(new TEMP(TEMP.newTempName()));

                uses.add(setDef.getFirst());
                uses.add(tmp);

                frag.asmcode.add(new AsmOPER("SETL","`d0,"+(value & 0xFFFFL),defs,null));
                frag.asmcode.add(new AsmOPER("SETML","`d0,"+((value & 0xFFFF0000L) >> 16),setDef,null));
                frag.asmcode.add(new AsmOPER("OR","`d0,`s0,`s1",defs,uses));
            }

            if(value>0xFFFFFFFFL){
                frag.asmcode.add(new AsmOPER("SETMH","`d0,"+((value & 0xFFFF00000000L) >> 32),setDef,null));
                frag.asmcode.add(new AsmOPER("OR","`d0,`s0,`s1",defs,uses));
            }

            if(value>0xFFFFFFFFFFFFL){
                frag.asmcode.add(new AsmOPER("SETH","`d0,"+((value & 0xFFFF000000000000L) >> 48),setDef,null));
                frag.asmcode.add(new AsmOPER("OR","`d0,`s0,`s1",defs,uses));
            }

            if(constant<0){
                uses = new LinkedList<TEMP>(Arrays.asList(tmp));
                defs = new LinkedList<TEMP>(Arrays.asList(tmp = new TEMP(TEMP.newTempName())));
                frag.asmcode.add(new AsmOPER("NEG","`d0,0,`s0",defs,uses));
            }
        }

        if(expr instanceof MEM){
            uses.add(parse(((MEM)expr).addr));
            defs.add(tmp = new TEMP(TEMP.newTempName()));
            frag.asmcode.add(new AsmOPER("LDO","`d0,`s0,0",defs,uses));
        }

        if(expr instanceof NAME){
            defs.add(tmp = new TEMP(TEMP.newTempName()));
            labels.add(new LABEL(((NAME)expr).name));
            frag.asmcode.add(new AsmOPER("LDA","`d0,`l0",defs,null,labels));
        }

        if(expr instanceof CALL){
            CALL call = (CALL)expr;

            for(int i=call.numArgs()-1; i>=0; i--){
                frag.asmcode.add(new AsmOPER("STO","`s0,`s1,"+(i*8),null,
                            new LinkedList<TEMP>(Arrays.asList(parse(call.args(i)),new TEMP(frag.FP)))));
            }

            defs.add(tmp = new TEMP(TEMP.newTempName()));
            labels.add(new LABEL(call.label));
            frag.asmcode.add(new AsmOPER("PUSHJ","`d0,`l0",defs,null,labels));
        }

        //if(tmp==null) System.out.println("null:"+tmp);
        return tmp;
    }

    private void optimize(){
        for(Fragment tmp : task.fragments.values()){
            if(tmp instanceof CodeFragment){
                optimize((CodeFragment)tmp);
            }
        }
    }

    private void optimize(CodeFragment frag){
        for(int i=0; i<frag.asmcode.size()-1; i++){
            AsmInst inst = frag.asmcode.get(i);
            if(inst.mnemonic.equals("SET") && inst.uses.size()==0){
                TEMP def = inst.defs.getFirst();
                String constant = inst.assem.substring(inst.assem.indexOf(',')+1);

                boolean ok = false;

                for(int j=0; j<frag.asmcode.size(); j++){
                    AsmInst use = frag.asmcode.get(j);
                    int idx = use.uses.indexOf(def);

                    if(use.defs.contains(def) && i!=j) break;
                    else if(idx==1 && (ok = use.uses.remove(def))){
                        use.assem = use.assem.substring(0,use.assem.lastIndexOf(',')+1)+constant;
                    }else if(use.mnemonic.equals("STO") && idx==0 && (ok = use.uses.remove(def))){
                        use.mnemonic = "STCO";
                        use.assem = constant+",`s0"+use.assem.substring(use.assem.lastIndexOf(','));
                    }else if(use instanceof AsmMOVE && idx==0 && (ok = use.uses.remove(def))){
                        frag.asmcode.remove(j);
                        frag.asmcode.add(j,new AsmOPER("SET",use.assem.substring(0,use.assem.indexOf(',')+1)+
                                    constant, use.defs, use.uses));
                    }
                }
                if(ok) frag.asmcode.remove(i--);
            }else if(inst instanceof AsmLABEL){
                AsmInst next = frag.asmcode.get(i+1);
                if(next instanceof AsmLABEL){
                    for(int j=0; j<frag.asmcode.size(); j++){
                        AsmInst tmp = frag.asmcode.get(j);
                        if(tmp instanceof AsmOPER && tmp.labels.remove(next.labels.getFirst())){
                            tmp.labels.add(inst.labels.getFirst());
                        }
                    }
                    frag.asmcode.remove(i+1);
                }
            }else if(!inst.mnemonic.equals("PUSHJ")){
                AsmInst use = frag.asmcode.get(i+1);

                if(use instanceof AsmMOVE && use.uses.contains(inst.defs.getFirst())){
                    inst.defs.set(0,use.defs.getFirst());
                    frag.asmcode.remove(i+1);
                    i--;
                }
            }
        }
    }

    public void print(){
        for(Fragment tmp : task.fragments.values()){
            if(tmp instanceof CodeFragment){
                CodeFragment frag = (CodeFragment)tmp;

                for(AsmInst inst : frag.asmcode){
                    System.out.println(inst.format());
                }
            }
        }
    }
}
