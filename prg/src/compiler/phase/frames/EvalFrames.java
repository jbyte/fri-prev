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

    public EvalFrames(Attributes attrs) {
        this.attrs = attrs;
    }

    @Override
    public void visit(FunDecl funDecl) {
        for (int p = 0; p < funDecl.numPars(); p++)
            funDecl.par(p).accept(this);
        funDecl.type.accept(this);
    }

    @Override
    public void visit(FunDef funDef) {
        for (int p = 0; p < funDef.numPars(); p++)
            funDef.par(p).accept(this);
        funDef.type.accept(this);
        funDef.body.accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type.accept(this);
        Typ typ = attrs.typAttr.get(varDecl);
        attrs.accAttr.set(varDecl,new StaticAccess(varDecl.name,typ.size()));
    }

}
