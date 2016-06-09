package compiler.phase.build;

import java.io.*;
import java.util.*;

import compiler.*;
import compiler.phase.frames.*;
import compiler.phase.imcode.*;
import compiler.phase.asmcode.*;

public class Build extends Phase{
    private Task task;

    public Build(Task task){
        super(task,"build");

        this.task = task;
    }

    public void build(){
        for(Fragment tmp : task.fragments.values()){
            if(tmp instanceof CodeFragment){
                CodeFragment frag = (CodeFragment)tmp;
                LinkedList<AsmInst> prologue = new LinkedList<AsmInst>();
                LinkedList<AsmInst> epilogue = new LinkedList<AsmInst>();

                prologue.add(new AsmOPER("ADD", "$0,$253,0"));
                prologue.add(new AsmOPER("ADD", "$253,254,0"));
                prologue.add(new AsmOPER("SETL", "$1,40"));
                prologue.add(new AsmOPER("SUB", "$254,$254,$1"));
                prologue.add(new AsmOPER("SETL", "$1,16"));
                prologue.add(new AsmOPER("SUB", "$1,$253,$1"));
                prologue.add(new AsmOPER("STO", "$0,$1,0"));
                prologue.add(new AsmOPER("GET", "$0,rJ"));
                prologue.add(new AsmOPER("SUB", "$1,$1,8"));
                prologue.add(new AsmOPER("STO", "$0,$1,0"));

                epilogue.add(new AsmOPER("STO", "$0,$253,0"));
                epilogue.add(new AsmOPER("SETL", "$1,16));
                epilogue.add(new AsmOPER("SUB", "$1,$253,$1"));
                epilogue.add(new AsmOPER("LDO", "$0,$1,0"));
                epilogue.add(new AsmOPER("SUB", "$1,$1,8"));
                epilogue.add(new AsmOPER("LDO", "$0,$1,0"));
                epilogue.add(new AsmOPER("PUT", "rJ,$1));
                epilogue.add(new AsmOPER("ADD", "$254,$253,0"));
                epilogue.add(new AsmOPER("ADD", "$253,$0,0"));
                epilogue.add(new AsmOPER("POP", "0,0"));
            }
        }
    }
}
