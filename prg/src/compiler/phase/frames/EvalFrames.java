package compiler.phase.frames;

import java.util.*;

import compiler.data.acc.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.frm.*;
import compiler.data.typ.*;

/**
 * Frame and access evaluator.
 *
 * @author sliva
 */
public class EvalFrames extends FullVisitor {

    private final Attributes attrs;
    private int level;
    private int numVar;
    private int numFun;
    private HashMap<String,ArrayList<Long>> map;
    private String inFun;

    public EvalFrames(Attributes attrs) {
        this.attrs = attrs;
        this.level = 0;
        this.numVar = 0;
        this.numFun = 0;
        this.map = new HashMap<String,ArrayList<Long>>();
        this.inFun = null;
    }

    @Override
    public void visit(FunCall funCall) {
        for (int a = 0; a < funCall.numArgs(); a++){
            funCall.arg(a).accept(this);
        }
    }

    @Override
    public void visit(FunDecl funDecl) {
        //level++;
        //long offset = 0;
        for (int p = 0; p < funDecl.numPars(); p++){
            funDecl.par(p).accept(this);
            //Typ typ = attrs.typAttr.get(funDecl.par(p));
            //attrs.accAttr.set(funDecl.par(p),new OffsetAccess(offset,typ.size()));
            //offset += typ.size();
        }
        funDecl.type.accept(this);
        //long inpCallSize = offset+8;
        //Typ typ = attrs.typAttr.get(funDecl.type);
        //inpCallSize += typ.size();
        //attrs.frmAttr.set(funDecl,new Frame(level,funDecl.name,inpCallSize,0,0,0,0));
        //level--;
    }

    @Override
    public void visit(FunDef funDef) {
        level++;
        long offset = 8;
        for (int p = 0; p < funDef.numPars(); p++){
            funDef.par(p).accept(this);
            Typ typ = attrs.typAttr.get(funDef.par(p));
            attrs.accAttr.set(funDef.par(p),new OffsetAccess(offset,typ.size()));
            offset += typ.size();
            if(inFun!=null){
                //Typ typ = attrs.typAttr.get(funCall.arg(a));
                ArrayList<Long> list = map.get(inFun);
                list.set(1,((Long)list.get(1)).longValue()+typ.size());
            }
        }

        funDef.type.accept(this);

        ArrayList<Long> list = new ArrayList<Long>();
        list.add(new Long(0));
        list.add(new Long(0));
        String tmp = inFun;


        map.put(funDef.name,list);
        inFun = funDef.name;
        funDef.body.accept(this);
        inFun = tmp;

        long inpCallSize = offset;
        list = map.get(funDef.name);
        long locVar = list.get(0);
        long outCallSize = list.get(1);
        if(outCallSize>0)
            outCallSize+=8;

        attrs.frmAttr.set(funDef,new Frame(level,"f"+numFun+"_"+funDef.name,inpCallSize,locVar,0,0,outCallSize));
        numFun++;
        level--;
    }

    @Override
    public void visit(RecType recType) {
        long offset = 8;
        for (int c = 0; c < recType.numComps(); c++){
            recType.comp(c).accept(this);
            Typ typ = attrs.typAttr.get(recType.comp(c));
            attrs.accAttr.set(recType.comp(c),new OffsetAccess(offset,typ.size()));
            offset += typ.size();
        }
    }

    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type.accept(this);
        Typ typ = attrs.typAttr.get(varDecl);
        if(inFun==null)
            attrs.accAttr.set(varDecl,new StaticAccess("v"+numVar+"_"+varDecl.name,typ.size()));
        else{
            ArrayList<Long> list = map.get(inFun);
            list.set(0,list.get(0)+typ.size());
            attrs.accAttr.set(varDecl,new OffsetAccess(-list.get(0),typ.size()));
        }
        numVar++;
    }

    @Override
    public void visit(WhereExpr whereExpr) {
        whereExpr.expr.accept(this);
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
    }

}
