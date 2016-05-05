package compiler.phase.seman;

import compiler.common.report.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.typ.*;

public class EvalTypAfMem extends FullVisitor{

    private final Attributes attrs;

    public EvalTypAfMem(Attributes attrs){
        this.attrs = attrs;
    }

    @Override
    public void visit(BinExpr binExpr){
        binExpr.fstExpr.accept(this);
        binExpr.sndExpr.accept(this);
        Typ fst = attrs.typAttr.get(binExpr.fstExpr);
        Typ snd = attrs.typAttr.get(binExpr.sndExpr);
        if(fst!=null && snd!=null && attrs.memAttr.get(binExpr.fstExpr) &&
                    Typ.equiv(fst,snd) && binExpr.oper==BinExpr.Oper.ASSIGN)
            attrs.typAttr.set(binExpr,new VoidTyp());
    }

    @Override
    public void visit(Exprs exprs){
        for(int e=0; e<exprs.numExprs(); e++)
            exprs.expr(e).accept(this);
        Typ typ = null;
        for(int e=0; e<exprs.numExprs(); e++){
            typ = attrs.typAttr.get(exprs.expr(e));
            if(typ==null) break;
        }
        Typ tmp = attrs.typAttr.get(exprs);
        if(typ!=null && tmp==null) attrs.typAttr.set(exprs,typ);
    }

    @Override
    public void visit(UnExpr unExpr){
        unExpr.subExpr.accept(this);
        Typ typ = attrs.typAttr.get(unExpr.subExpr);
        if(typ!=null && attrs.memAttr.get(unExpr.subExpr) &&
                unExpr.oper==UnExpr.Oper.MEM)
            attrs.typAttr.set(unExpr,new PtrTyp(typ));
    }

    @Override
    public void visit(WhereExpr whereExpr){
        whereExpr.expr.accept(this);
        Typ typ = attrs.typAttr.get(whereExpr);
        Typ tmp = attrs.typAttr.get(whereExpr.expr);
        if(typ==null && tmp!=null) attrs.typAttr.set(whereExpr,tmp);
    }
}
