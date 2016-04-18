package compiler.phase.seman;

import java.util.LinkedList;

import compiler.common.report.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.typ.*;

/**
 * Type checker.
 * 
 * <p>
 * Type checker checks type of all sentential forms of the program and resolves
 * the component names as this cannot be done earlier, i.e., in
 * {@link compiler.phase.seman.EvalDecl}.
 * </p>
 * 
 * @author sliva
 */
public class EvalTyp extends FullVisitor {

    private final Attributes attrs;

    private int iteration;
    
    public EvalTyp(Attributes attrs) {
        this.attrs = attrs;
        this.iteration = 0;
    }
    
    /** The symbol table. */
    private SymbolTable symbolTable = new SymbolTable();

    public void visit(ArrType arrType) {
        arrType.size.accept(this);
        arrType.elemType.accept(this);
        if(attrs.valueAttr.get(arrType.size).longValue()>0)
            attrs.typAttr.set(arrType,new ArrTyp(attrs.valueAttr.get(arrType.size).longValue()
                        ,attrs.typAttr.get(arrType.elemType)));
    }

    public void visit(AtomExpr atomExpr) {
        switch(atomExpr.type){
            case INTEGER:
                attrs.typAttr.set(atomExpr,new IntegerTyp());
                break;
            case BOOLEAN:
                attrs.typAttr.set(atomExpr,new BooleanTyp());
                break;
            case CHAR:
                attrs.typAttr.set(atomExpr,new CharTyp());
                break;
            case STRING:
                attrs.typAttr.set(atomExpr,new StringTyp());
                break;
            case VOID:
                attrs.typAttr.set(atomExpr,new VoidTyp());
                break;
            case PTR:
                attrs.typAttr.set(atomExpr,new PtrTyp(new VoidTyp()));
                break;
        }
    }

    public void visit(AtomType atomType) {
        switch(atomType.type){
            case INTEGER:
                attrs.typAttr.set(atomType,new IntegerTyp());
                break;
            case BOOLEAN:
                attrs.typAttr.set(atomType,new BooleanTyp());
                break;
            case CHAR:
                attrs.typAttr.set(atomType,new CharTyp());
                break;
            case STRING:
                attrs.typAttr.set(atomType,new StringTyp());
                break;
            case VOID:
                attrs.typAttr.set(atomType,new VoidTyp());
                break;
        }
    }

    public void visit(BinExpr binExpr) {
        binExpr.fstExpr.accept(this);
        binExpr.sndExpr.accept(this);
        Typ fstTyp = attrs.typAttr.get(binExpr.fstExpr);
        Typ sndTyp = attrs.typAttr.get(binExpr.sndExpr);
        if(Typ.equiv(fstTyp,new IntegerTyp()) && Typ.equiv(sndTyp,new IntegerTyp())){
            if(binExpr.oper==BinExpr.Oper.ADD ||binExpr.oper==BinExpr.Oper.SUB ||
                    binExpr.oper==BinExpr.Oper.MUL || binExpr.oper==BinExpr.Oper.DIV ||
                    binExpr.oper==BinExpr.Oper.MOD)
                attrs.typAttr.set(binExpr,new IntegerTyp());
            if(binExpr.oper==BinExpr.Oper.REC)
                attrs.typAttr.set(binExpr,sndTyp);
            if(binExpr.oper==BinExpr.Oper.EQU || binExpr.oper==BinExpr.Oper.NEQ ||
                    binExpr.oper==BinExpr.Oper.LTH || binExpr.oper==BinExpr.Oper.GTH ||
                    binExpr.oper==BinExpr.Oper.LEQ || binExpr.oper==BinExpr.Oper.GEQ)
                attrs.typAttr.set(binExpr,new BooleanTyp());
            if(binExpr.oper==BinExpr.Oper.ASSIGN && attrs.memAttr.get(binExpr.fstExpr))
                attrs.typAttr.set(binExpr,new VoidTyp());
        }else if(Typ.equiv(fstTyp,new BooleanTyp()) && Typ.equiv(sndTyp,new BooleanTyp())){
            if(binExpr.oper==BinExpr.Oper.AND || binExpr.oper==BinExpr.Oper.OR)
                attrs.typAttr.set(binExpr,new BooleanTyp());
            if(binExpr.oper==BinExpr.Oper.REC)
                attrs.typAttr.set(binExpr,sndTyp);
            if(binExpr.oper==BinExpr.Oper.EQU || binExpr.oper==BinExpr.Oper.NEQ ||
                    binExpr.oper==BinExpr.Oper.LTH || binExpr.oper==BinExpr.Oper.GTH ||
                    binExpr.oper==BinExpr.Oper.LEQ || binExpr.oper==BinExpr.Oper.GEQ)
                attrs.typAttr.set(binExpr,new BooleanTyp());
            if(binExpr.oper==BinExpr.Oper.ASSIGN && attrs.memAttr.get(binExpr.fstExpr))
                attrs.typAttr.set(binExpr,new VoidTyp());
        }else if(fstTyp instanceof ArrTyp && sndTyp instanceof IntegerTyp && binExpr.oper==BinExpr.Oper.ARR){
            attrs.typAttr.set(binExpr,((ArrTyp)fstTyp).elemTyp);
        }else{
            if(binExpr.oper==BinExpr.Oper.REC)
                if(Typ.equiv(fstTyp,sndTyp)){
                    attrs.typAttr.set(binExpr,sndTyp);
                }
            if(binExpr.oper==BinExpr.Oper.EQU || binExpr.oper==BinExpr.Oper.NEQ ||
                    binExpr.oper==BinExpr.Oper.LTH || binExpr.oper==BinExpr.Oper.GTH ||
                    binExpr.oper==BinExpr.Oper.LEQ || binExpr.oper==BinExpr.Oper.GEQ){
                if(Typ.equiv(fstTyp,sndTyp)){
                    attrs.typAttr.set(binExpr,new BooleanTyp());
                }
            }
            if(binExpr.oper==BinExpr.Oper.ASSIGN && attrs.memAttr.get(binExpr.fstExpr))
                attrs.typAttr.set(binExpr,new VoidTyp());
        }
    }

    public void visit(CastExpr castExpr) {
        castExpr.type.accept(this);
        castExpr.expr.accept(this);
        Typ typTyp = attrs.typAttr.get(castExpr.type);
        Typ exprTyp = attrs.typAttr.get(castExpr.expr);
        if(typTyp instanceof PtrTyp && Typ.equiv(exprTyp,new PtrTyp(new VoidTyp())))
            attrs.typAttr.set(castExpr,((PtrTyp)typTyp).baseTyp);
    }

    public void visit(CompDecl compDecl) {
        if(iteration==0){
            compDecl.type.accept(this);
            Typ typ = attrs.typAttr.get(compDecl.type);
            attrs.typAttr.set(compDecl,typ);
        }
    }

    public void visit(CompName compName) {
        if(iteration>0){
            Decl decl = attrs.declAttr.get(compName);
            Typ typ = attrs.typAttr.get(decl);
            attrs.typAttr.set(compName,typ);
        }
    }
    
    public void visit(DeclError declError) {
    }

    public void visit(Exprs exprs) {
        for (int e = 0; e < exprs.numExprs(); e++)
            exprs.expr(e).accept(this);
        Typ typ = null;
        for(int e=0; e<exprs.numExprs(); e++){
            typ = attrs.typAttr.get(exprs.expr(e));
            if(typ==null)break;
        }
        if(typ!=null)attrs.typAttr.set(exprs,typ);
    }

    public void visit(ExprError exprError) {
    }

    public void visit(ForExpr forExpr) {
        forExpr.var.accept(this);
        forExpr.loBound.accept(this);
        forExpr.hiBound.accept(this);
        forExpr.body.accept(this);
        Typ varTyp = attrs.typAttr.get(forExpr.var);
        Typ loTyp = attrs.typAttr.get(forExpr.loBound);
        Typ hiTyp = attrs.typAttr.get(forExpr.hiBound);
        Typ bodyTyp = attrs.typAttr.get(forExpr.body);
        if(Typ.equiv(varTyp,new IntegerTyp()) && Typ.equiv(loTyp,new IntegerTyp()) &&
                Typ.equiv(hiTyp,new IntegerTyp()) && bodyTyp!=null)
            attrs.typAttr.set(forExpr,new VoidTyp());
    }

    public void visit(FunCall funCall) {
        for (int a = 0; a < funCall.numArgs(); a++)
            funCall.arg(a).accept(this);
        Decl decl = attrs.declAttr.get(funCall);
        Typ typ = attrs.typAttr.get(decl);
        attrs.typAttr.set(funCall,((FunTyp)typ).resultTyp);
    }

    public void visit(FunDecl funDecl) {
        if(iteration==0){
            for (int p = 0; p < funDecl.numPars(); p++)
                funDecl.par(p).accept(this);
        }
        if(iteration==1){
            funDecl.type.accept(this);
            LinkedList<Typ> list = new LinkedList<Typ>();
            for(int p=0; p<funDecl.numPars(); p++)
                list.add(attrs.typAttr.get(funDecl.par(p)));
            attrs.typAttr.set(funDecl,new FunTyp(list,attrs.typAttr.get(funDecl.type)));
        }
    }

    public void visit(FunDef funDef) {
        if(iteration==0){
            for (int p = 0; p < funDef.numPars(); p++)
                funDef.par(p).accept(this);
        }
        if(iteration==1){
            funDef.type.accept(this);
            LinkedList<Typ> list = new LinkedList<Typ>();
            for(int p=0; p<funDef.numPars(); p++)
                list.add(attrs.typAttr.get(funDef.par(p)));
            attrs.typAttr.set(funDef,new FunTyp(list,attrs.typAttr.get(funDef.type)));
        }
        if(iteration==2){
            funDef.body.accept(this);
        }
    }

    public void visit(IfExpr ifExpr) {
        ifExpr.cond.accept(this);
        ifExpr.thenExpr.accept(this);
        ifExpr.elseExpr.accept(this);
        Typ condTyp = attrs.typAttr.get(ifExpr.cond);
        Typ thenTyp = attrs.typAttr.get(ifExpr.thenExpr);
        Typ elseTyp = attrs.typAttr.get(ifExpr.elseExpr);
        if(Typ.equiv(condTyp,new BooleanTyp()) && thenTyp!=null && elseTyp!=null)
            attrs.typAttr.set(ifExpr,new VoidTyp());
    }

    public void visit(ParDecl parDecl) {
        if(iteration==0){
            parDecl.type.accept(this);
            Typ typ = attrs.typAttr.get(parDecl.type);
            attrs.typAttr.set(parDecl,typ);
        }
    }

    public void visit(Program program) {
        program.expr.accept(this);
    }

    public void visit(PtrType ptrType) {
        ptrType.baseType.accept(this);
        attrs.typAttr.set(ptrType,new PtrTyp(attrs.typAttr.get(ptrType.baseType)));
    }

    public void visit(RecType recType) {
        for (int c = 0; c < recType.numComps(); c++)
            recType.comp(c).accept(this);
        LinkedList<Typ> list = new LinkedList<Typ>();
        for(int c=0; c<recType.numComps(); c++)
            list.add(attrs.typAttr.get(recType.comp(c)));
        attrs.typAttr.set(recType,new RecTyp(symbolTable.newNamespace(""),list));
    }

    public void visit(TypeDecl typDecl) {
        if(iteration==0){
            typDecl.type.accept(this);
            Typ typ = attrs.typAttr.get(typDecl.type);
            TypName typName = new TypName(typDecl.name);
            typName.setType(typ);
            attrs.typAttr.set(typDecl,typName);
        }
    }
    
    public void visit(TypeError typeError) {
    }

    public void visit(TypeName typeName) {
        Decl decl = attrs.declAttr.get(typeName);
        TypName typName = new TypName(typeName.name());
        if(decl!=null)typName.setType(attrs.typAttr.get(decl.type));
        attrs.typAttr.set(typeName,typName);
    }

    public void visit(UnExpr unExpr) {
        unExpr.subExpr.accept(this);
        Typ typ = attrs.typAttr.get(unExpr.subExpr);
        if(Typ.equiv(typ,new BooleanTyp()) && unExpr.oper==UnExpr.Oper.NOT)
            attrs.typAttr.set(unExpr,typ);
        else if(Typ.equiv(typ,new IntegerTyp()) && (unExpr.oper==UnExpr.Oper.ADD || 
                    unExpr.oper==UnExpr.Oper.SUB))
            attrs.typAttr.set(unExpr,typ);
        else if(typ instanceof PtrTyp)
            attrs.typAttr.set(unExpr,((PtrTyp)typ).baseTyp);
    }

    public void visit(VarDecl varDecl) {
        if(iteration==0){
            varDecl.type.accept(this);
            Typ typ = attrs.typAttr.get(varDecl.type);
            attrs.typAttr.set(varDecl,typ);
        }
    }

    public void visit(VarName varName) {
        if(iteration>0){
            Decl decl = attrs.declAttr.get(varName);
            attrs.typAttr.set(varName,attrs.typAttr.get(decl.type));
        }
    }

    public void visit(WhereExpr whereExpr) {
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
        iteration++;
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
        iteration++;
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);
        whereExpr.expr.accept(this);
        Typ decl = null;
        for(int d=0; d<whereExpr.numDecls(); d++){
            decl = attrs.typAttr.get(whereExpr.decl(d));
            if(decl==null)break;
        }
        Typ typ = attrs.typAttr.get(whereExpr.expr);
        if(decl!=null && typ!=null) attrs.typAttr.set(whereExpr,typ);
    }

    public void visit(WhileExpr whileExpr) {
        whileExpr.cond.accept(this);
        whileExpr.body.accept(this);
        Typ condTyp = attrs.typAttr.get(whileExpr.cond);
        Typ bodyTyp = attrs.typAttr.get(whileExpr.body);
        if(Typ.equiv(condTyp,new BooleanTyp()) && bodyTyp!=null)
            attrs.typAttr.set(whileExpr,new VoidTyp());
    }

}
