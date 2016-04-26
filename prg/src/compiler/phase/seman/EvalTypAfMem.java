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
        //if(fst!=null && snd!=null && attrs.memAttr.get(binExpr.fstExpr) &&
                    //Typ.equiv(fst,snd) && binExpr.oper==BinExpr.Oper.ASSIGN)
            //attrs.typAttr.set(binExpr,new VoidTyp());
        if(snd instanceof AssignableTyp && attrs.memAttr.get(binExpr.fstExpr) &&
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
        //if(typ!=null && tmp==null) attrs.typAttr.set(exprs,typ);
        if(tmp==null) attrs.typAttr.set(exprs,typ);
    }

    @Override
    public void visit(ForExpr forExpr) {
        forExpr.var.accept(this);
        forExpr.loBound.accept(this);
        forExpr.hiBound.accept(this);
        //tmp = iteration;
        forExpr.body.accept(this);
        //iteration = tmp;
        Typ varTyp = attrs.typAttr.get(forExpr.var);
        Typ loTyp = attrs.typAttr.get(forExpr.loBound);
        Typ hiTyp = attrs.typAttr.get(forExpr.hiBound);
        Typ bodyTyp = attrs.typAttr.get(forExpr.body);
        if(Typ.equiv(varTyp,new IntegerTyp()) && Typ.equiv(loTyp,new IntegerTyp()) &&
                Typ.equiv(hiTyp,new IntegerTyp()) && bodyTyp!=null &&
                attrs.typAttr.get(forExpr)==null)
            attrs.typAttr.set(forExpr,new VoidTyp());
    }

    @Override
    public void visit(IfExpr ifExpr) {
        ifExpr.cond.accept(this);
        //tmp = iteration;
        ifExpr.thenExpr.accept(this);
        //iteration = tmp;
        ifExpr.elseExpr.accept(this);
        //iteration = tmp;
        Typ condTyp = attrs.typAttr.get(ifExpr.cond);
        Typ thenTyp = attrs.typAttr.get(ifExpr.thenExpr);
        Typ elseTyp = attrs.typAttr.get(ifExpr.elseExpr);
        if(Typ.equiv(condTyp,new BooleanTyp()) && thenTyp!=null && elseTyp!=null &&
                attrs.typAttr.get(ifExpr)==null)
            attrs.typAttr.set(ifExpr,new VoidTyp());
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
        for(int d=0; d<whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
        whereExpr.expr.accept(this);
        Typ typ = attrs.typAttr.get(whereExpr);
        Typ tmp = attrs.typAttr.get(whereExpr.expr);
        //if(typ==null && tmp!=null) attrs.typAttr.set(whereExpr,tmp);
        if(typ==null) attrs.typAttr.set(whereExpr,tmp);
    }

    public void visit(WhileExpr whileExpr) {
        whileExpr.cond.accept(this);
        //tmp = iteration;
        whileExpr.body.accept(this);
        //iteration = tmp;
        Typ condTyp = attrs.typAttr.get(whileExpr.cond);
        Typ bodyTyp = attrs.typAttr.get(whileExpr.body);
        if(Typ.equiv(condTyp,new BooleanTyp()) && bodyTyp!=null &&
                attrs.typAttr.get(whileExpr)==null)
            attrs.typAttr.set(whileExpr,new VoidTyp());
    }

}
