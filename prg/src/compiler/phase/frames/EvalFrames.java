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
        long offset = 0;
        for (int p = 0; p < funDecl.numPars(); p++){
            funDecl.par(p).accept(this);
            Typ typ = attrs.typAttr.get(funDecl.par(p));
            attrs.accAttr.set(funDecl.par(p),new OffsetAccess(offset,typ.size()));
            offset += typ.size();
        }
        funDecl.type.accept(this);
    }

    @Override
    public void visit(FunDef funDef) {
        long offset = 0;
        for (int p = 0; p < funDef.numPars(); p++){
            funDef.par(p).accept(this);
            Typ typ = attrs.typAttr.get(funDef.par(p));
            attrs.accAttr.set(funDef.par(p),new OffsetAccess(offset,typ.size()));
            offset += typ.size();
        }
        funDef.type.accept(this);
        funDef.body.accept(this);
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
        attrs.accAttr.set(varDecl,new StaticAccess(varDecl.name,typ.size()));
    }

}
