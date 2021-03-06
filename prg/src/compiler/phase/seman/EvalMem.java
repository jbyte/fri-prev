package compiler.phase.seman;

import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.typ.*;

/**
 * @author sliva
 */
public class EvalMem extends FullVisitor {
    
    private final Attributes attrs;
    
    public EvalMem(Attributes attrs) {
        this.attrs = attrs;
    }

    public void visit(ArrType arrType) {
        arrType.size.accept(this);
        arrType.elemType.accept(this);
        //attrs.memAttr.set(arrType,false);
    }

    public void visit(AtomExpr atomExpr) {
        attrs.memAttr.set(atomExpr,false);
    }

    public void visit(AtomType atomType) {
        //attrs.memAttr.set(atomType,false);
    }

    public void visit(BinExpr binExpr) {
        binExpr.fstExpr.accept(this);
        binExpr.sndExpr.accept(this);
        Typ fst = attrs.typAttr.get(binExpr.fstExpr);
        Typ snd = attrs.typAttr.get(binExpr.sndExpr);
        if(fst!=null && snd!=null && binExpr.oper==BinExpr.Oper.REC)
            attrs.memAttr.set(binExpr,true);
        else if(fst instanceof ArrTyp && Typ.equiv(snd,new IntegerTyp()) && 
                binExpr.oper==BinExpr.Oper.ARR)
            attrs.memAttr.set(binExpr,true);
        else
            attrs.memAttr.set(binExpr,false);
    }

    public void visit(CastExpr castExpr) {
        castExpr.type.accept(this);
        castExpr.expr.accept(this);
        attrs.memAttr.set(castExpr,false);
    }

    public void visit(CompDecl compDecl) {
        compDecl.type.accept(this);
        //attrs.memAttr.set(compDecl,false);
    }

    public void visit(CompName compName) {
        attrs.memAttr.set(compName,false);
    }
    
    public void visit(DeclError declError) {
    }

    public void visit(Exprs exprs) {
        for (int e = 0; e < exprs.numExprs(); e++)
            exprs.expr(e).accept(this);
        attrs.memAttr.set(exprs,false);
    }

    public void visit(ExprError exprError) {
    }

    public void visit(ForExpr forExpr) {
        forExpr.var.accept(this);
        forExpr.loBound.accept(this);
        forExpr.hiBound.accept(this);
        forExpr.body.accept(this);
        attrs.memAttr.set(forExpr,false);
    }

    public void visit(FunCall funCall) {
        for (int a = 0; a < funCall.numArgs(); a++)
            funCall.arg(a).accept(this);
        attrs.memAttr.set(funCall,false);
    }

    public void visit(FunDecl funDecl) {
        for (int p = 0; p < funDecl.numPars(); p++)
            funDecl.par(p).accept(this);
        funDecl.type.accept(this);
        //attrs.memAttr.set(funDecl,false);
    }

    public void visit(FunDef funDef) {
        for (int p = 0; p < funDef.numPars(); p++)
            funDef.par(p).accept(this);
        funDef.type.accept(this);
        funDef.body.accept(this);
        //attrs.memAttr.set(funDef,false);
    }

    public void visit(IfExpr ifExpr) {
        ifExpr.cond.accept(this);
        ifExpr.thenExpr.accept(this);
        ifExpr.elseExpr.accept(this);
        attrs.memAttr.set(ifExpr,false);
    }

    public void visit(ParDecl parDecl) {
        parDecl.type.accept(this);
        //attrs.memAttr.set(parDecl,false);
    }

    public void visit(Program program) {
        program.expr.accept(this);
        attrs.memAttr.set(program,false);
    }

    public void visit(PtrType ptrType) {
        ptrType.baseType.accept(this);
        //attrs.memAttr.set(ptrType,false);
    }

    public void visit(RecType recType) {
        for (int c = 0; c < recType.numComps(); c++)
            recType.comp(c).accept(this);
        //attrs.memAttr.set(recType,false);
    }

    public void visit(TypeDecl typDecl) {
        typDecl.type.accept(this);
        //attrs.memAttr.set(typDecl,false);
    }
    
    public void visit(TypeError typeError) {
    }

    public void visit(TypeName typeName) {
        //attrs.memAttr.set(typeName,false);
    }

    public void visit(UnExpr unExpr) {
        unExpr.subExpr.accept(this);
        Typ typ = attrs.typAttr.get(unExpr.subExpr);
        if(typ instanceof PtrTyp){
            if(((PtrTyp)typ).baseTyp!=null)
                attrs.memAttr.set(unExpr,true);
            else
                attrs.memAttr.set(unExpr,false);
        }else
            attrs.memAttr.set(unExpr,false);
    }

    public void visit(VarDecl varDecl) {
        varDecl.type.accept(this);
        //attrs.memAttr.set(varDecl,false);
    }

    public void visit(VarName varName) {
        Typ typ = attrs.typAttr.get(varName);
        if(typ!=null)
            attrs.memAttr.set(varName,true);
        else
            attrs.memAttr.set(varName,false);
    }

    public void visit(WhereExpr whereExpr) {
        whereExpr.expr.accept(this);
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
        attrs.memAttr.set(whereExpr,false);
    }

    public void visit(WhileExpr whileExpr) {
        whileExpr.cond.accept(this);
        whileExpr.body.accept(this);
        attrs.memAttr.set(whileExpr,false);
    }

}

