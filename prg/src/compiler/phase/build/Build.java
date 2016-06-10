package compiler.phase.build;

import java.io.*;
import java.util.*;

import compiler.*;
import compiler.phase.*;

import compiler.phase.frames.*;
import compiler.phase.imcode.*;
import compiler.phase.codegen.*;

import compiler.data.asm.*;
import compiler.data.frg.*;
import compiler.data.frm.*;
import compiler.data.imc.*;

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
                epilogue.add(new AsmOPER("SETL", "$1,16"));
                epilogue.add(new AsmOPER("SUB", "$1,$253,$1"));
                epilogue.add(new AsmOPER("LDO", "$0,$1,0"));
                epilogue.add(new AsmOPER("SUB", "$1,$1,8"));
                epilogue.add(new AsmOPER("LDO", "$0,$1,0"));
                epilogue.add(new AsmOPER("PUT", "rJ,$1"));
                epilogue.add(new AsmOPER("ADD", "$254,$253,0"));
                epilogue.add(new AsmOPER("ADD", "$253,$0,0"));
                epilogue.add(new AsmOPER("POP", "0,0"));

                frag.asmcode.addAll(1, prologue);
                frag.asmcode.addAll(epilogue);
            }
        }

        Frame fr = new Frame(0,"",0,0,0,0,0);
        CodeFragment main = new CodeFragment(fr,0,0,0,null);
        main.asmcode = new LinkedList<AsmInst>();

        main.asmcode.add(new AsmOPER("LOC", "#100"));
        main.asmcode.add(new AsmLABEL("`l0",new LABEL("Main")));
        main.asmcode.add(new AsmOPER("PUT", "rG,$254"));
        main.asmcode.add(new AsmOPER("SET", "$254,7"));
        main.asmcode.add(new AsmOPER("SL", "$254,$254,60"));
        main.asmcode.add(new AsmOPER("SUB", "$254,$254,1"));
        main.asmcode.add(new AsmOPER("SET", "$253,$254"));
        main.asmcode.add(new AsmOPER("SET", "$255,4"));
        main.asmcode.add(new AsmOPER("SL", "$255,$255,60"));
        main.asmcode.add(new AsmOPER("PUSHJ", "$0,_"));
        main.asmcode.add(new AsmOPER("TRAP", "0,Halt,0"));

        task.fragments.put("",main);
    }

    public void writeToFile(){
        int labelLen = 4;
        boolean dataSeg = false;
        int i = 0;

        String outFile = task.srcFName.replaceFirst(".prev",".mms");
        File f = new File(outFile);

        for(Fragment frag : task.fragments.values()){
            if(frag instanceof DataFragment){
                int len = ((DataFragment)frag).label.length();

                if(len > labelLen) labelLen = len;

                dataSeg = true;
            }else if(frag instanceof ConstFragment){
                int len = ((ConstFragment)frag).label.length();

                if(len > labelLen) labelLen = len;

                dataSeg = true;
            }else if(frag instanceof CodeFragment){
                for(AsmInst inst : ((CodeFragment)frag).asmcode){
                    if(inst.labels.size() > 0){
                        int len = inst.labels.getFirst().label.length();

                        if(len > labelLen) labelLen = len;
                    }
                }
            }
        }

        try{
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f,false)));

            if(dataSeg){
                indent(labelLen+1,pw);
                pw.write("LOC Data_Segment\n");
                indent(labelLen+1,pw);
                pw.write("GREG @\n");

                for(Fragment tmp : task.fragments.values()){
                    if(tmp instanceof DataFragment){
                        DataFragment frag = (DataFragment)tmp;

                        String data = "";
                        for(long j=0; j<(frag.width/8)-1; j++) data += "0,";

                        pw.write(String.format("%-"+labelLen+"s %s",frag.label,"OCTA "+data+"0\n"));
                    }else if(tmp instanceof ConstFragment){
                        ConstFragment frag = (ConstFragment)tmp;

                        pw.write(String.format("%-"+labelLen+"s %s",frag.label,"BYTE \""+frag.string+"\"0\n"));
                    }
                }
                pw.write("\n");
            }
            for(Fragment tmp : task.fragments.values()){
                if(tmp instanceof CodeFragment){
                    CodeFragment frag = (CodeFragment)tmp;

                    for(int j=0; j<frag.asmcode.size(); j++){
                        AsmInst inst = frag.asmcode.get(j);

                        if(inst instanceof AsmLABEL){
                            if(j+1>=frag.asmcode.size()){
                                frag.asmcode.add(new AsmOPER("SWYM",""));
                            }

                            pw.write(String.format("%-"+labelLen+"s %s\n", inst.labels.getFirst().label,
                                        frag.asmcode.get(++j).format(frag.registers)));
                        }else{
                            indent(labelLen+1,pw);
                            pw.write(inst.format(frag.registers)+"\n");
                        }
                    }
                    pw.write("\n");
                }
            }
            pw.flush();
            pw.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private void indent(int indent, PrintWriter pw) throws IOException{
            for(int i=0; i<indent; i++)pw.write(" ");
    }
}
