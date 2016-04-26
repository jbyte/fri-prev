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

    public EvalFrames(Attributes attrs) {
        this.attrs = attrs;
        this.level = 0;
        this.numVar = 0;
        this.numFun = 0;
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
        long offset = 0;
        for (int p = 0; p < funDef.numPars(); p++){
            funDef.par(p).accept(this);
            Typ typ = attrs.typAttr.get(funDef.par(p));
            attrs.accAttr.set(funDef.par(p),new OffsetAccess(offset,typ.size()));
            offset += typ.size();
        }
        funDef.type.accept(this);
        long inpCallSize = offset+8;
        Typ typ = attrs.typAttr.get(funDef.type);
        inpCallSize += typ.size();
        funDef.body.accept(this);
        attrs.frmAttr.set(funDef,new Frame(level,"f"+numFun+"_"+funDef.name,inpCallSize,0,0,0,0));
        numFun++;
        level--;
    }

    @Override
    public void visit(RecType recType) {
        long offset = 0;
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
        attrs.accAttr.set(varDecl,new StaticAccess("v"+numVar+"_"+varDecl.name,typ.size()));
        numVar++;
    }

    @Override
    public void visit(WhereExpr whereExpr) {
        whereExpr.expr.accept(this);
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
    }

}
