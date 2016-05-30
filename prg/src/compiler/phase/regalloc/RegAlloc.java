package compiler.phase.regalloc;

import java.util.*;

import compiler.*;
import compiler.common.report.*;
import compiler.phase.*;
import compiler.phase.codegen.*;

import compiler.data.frg.*;
import compiler.data.imc.*;
import compiler.data.asm.*;

public class RegAlloc extends Phase{

    private Task task;
    private int regs;

    private CodeGen codegen;
    private LinkedList<InterferenceNode> stack;

    public RegAlloc(Task task){
        super(task,"regalloc");

        this.task = task;
        this.codegen = new CodeGen(task);
        this.regs = task.registers;
        this.stack = null;
    }

    public void allocate(){
        for(Fragment tmp : task.fragments.values()){
            if(tmp instanceof CodeFragment){
                CodeFragment frag = (CodeFragment)tmp;

                do{
                    build(frag);
                    stack = new LinkedList<InterferenceNode>();

                    do{
                        simplify(frag);
                    }while(spill(frag));

                }while(select(frag));

                frag.registers = new HashMap<TEMP,String>();
                frag.registers.put(new TEMP(frag.FP),"$65");

                for(InterferenceNode node : frag.graph){
                    frag.registers.put(node.tmp,"$"+node.reg);
                }

                LinkedHashMap<TEMP,InterferenceNode> graph = codegen.analyze(frag);

                for(int i=0; i<frag.asmcode.size(); i++){
                    AsmInst inst = frag.asmcode.get(i);

                    if(inst.mnemonic.equals("PUSHJ")){
                        AsmInst next = frag.asmcode.get(i+1);
                        LinkedList<InterferenceNode> edges = graph.get(inst.defs.getFirst()).edges;
                        int maxReg = 0;

                        for(InterferenceNode edge : edges){
                            int register = Integer.parseInt(frag.registers.get(edge.tmp).substring(1));
                            if(register>maxReg) maxReg = register;
                        }

                        frag.registers.put(inst.defs.getFirst(),"$"+(edges.size()==0 ? 0 : maxReg +1));

                        if(next.mnemonic.equals("SET") && frag.registers.get(next.defs.getFirst()).equals(frag.registers.get(next.uses.getFirst()))){
                            frag.asmcode.remove(i+1);
                        }
                    }
                }
            }
        }
    }

    private void build(CodeFragment frag){
        codegen.analyze(frag);
    }

    private void simplify(CodeFragment frag){
        boolean done = false;

        while(!done){
            done = true;

            Iterator<InterferenceNode> iter = frag.graph.iterator();

            while(iter.hasNext()){
                InterferenceNode node = iter.next();

                if(node.edges.size()<regs){
                    done = false;

                    stack.push(node);

                    for(InterferenceNode edge : frag.graph){
                        edge.edges.remove(node);
                    }
                }
                iter.remove();
            }
        }
    }

    private boolean spill(CodeFragment frag){
        if(frag.graph.size()==0) return false;

        InterferenceNode spill = null;
        int length = 0;

        for(InterferenceNode node : frag.graph){
            int def = 0;

            while(!frag.asmcode.get(def).defs.contains(node.tmp)) def++;

            int use = frag.asmcode.size() - 1;

            while(!frag.asmcode.get(use).uses.contains(node.tmp)) use--;

            if(use - def > length){
                spill = node;
                length = use - def;
            }
        }

        frag.graph.remove(spill);
        spill.spill = InterferenceNode.POTENTIAL_SPILL;
        stack.push(spill);

        for(InterferenceNode edge : spill.edges){
            edge.edges.remove(spill);
        }

        return true;
    }

    private boolean select(CodeFragment frag){
        boolean repeat = false;

        while(stack.size()>0){
            InterferenceNode node = stack.pop();
            frag.graph.add(node);

            int registers[] = new int[regs + 1];
            boolean ok = false;

            for(InterferenceNode edge : node.edges){
                registers[edge.reg] = 1;
            }

            for(int i=0; i<regs; i++){
                if(registers[i]==0){
                    node.reg = i;
                    ok = true;
                    break;
                }
            }

            for(AsmInst inst : frag.asmcode){
                if(inst.mnemonic.equals("PUSHJ") && inst.defs.contains(node.tmp)){
                    node.reg = regs;
                    ok = true;
                    break;
                }
            }

            if(!ok){
                if(node.spill == InterferenceNode.POTENTIAL_SPILL){
                    node.spill = InterferenceNode.ACTUAL_SPILL;
                    repeat = true;
                    break;
                }else throw new CompilerError("Unable to allocate a register to "+node.tmp.name+".");
            }
        }

        if(repeat) startOver(frag);

        return repeat;
    }

    private void startOver(CodeFragment frag){
        for(InterferenceNode node : frag.graph){
            if(node.spill != InterferenceNode.ACTUAL_SPILL) continue;

            long offset = frag.frame.outCallSize + frag.frame.tmpVarsSize;
            frag.frame.tmpVarsSize += 8;

            int def = 0;
            while(!frag.asmcode.get(def++).defs.contains(node.tmp)){}

            frag.asmcode.add(def,new AsmOPER("STO","`s0,`s1,"+offset,null,new LinkedList<TEMP>(Arrays.asList(node.tmp,new TEMP(frag.FP)))));

            for(int i=frag.asmcode.size()-1; i>def; i--){
                if(frag.asmcode.get(i).uses.contains(node.tmp)){
                    frag.asmcode.add(i,new AsmOPER("LDO","`d0,`s0,"+offset,new LinkedList<TEMP>(Arrays.asList(node.tmp)),new LinkedList<TEMP>(Arrays.asList(new TEMP(frag.FP)))));
                }
            }
        }
    }

    public void print(){
        for(Fragment tmp : task.fragments.values()){
            if(tmp instanceof CodeFragment){
                CodeFragment frag = (CodeFragment)tmp;
                System.out.println("Assembly for code fragment:");

                for(AsmInst inst : frag.asmcode){
                    System.out.println(inst.format(frag.registers));
                }
            }
        }
    }
}
