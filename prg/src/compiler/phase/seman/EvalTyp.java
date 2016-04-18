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
	
	public EvalTyp(Attributes attrs) {
		this.attrs = attrs;
	}
	
	/** The symbol table. */
	private SymbolTable symbolTable = new SymbolTable();

	public void visit(ArrType arrType) {
		arrType.size.accept(this);
		arrType.elemType.accept(this);
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
	}

	public void visit(CastExpr castExpr) {
		castExpr.type.accept(this);
		castExpr.expr.accept(this);
	}

	public void visit(CompDecl compDecl) {
		compDecl.type.accept(this);
	}

	public void visit(CompName compName) {
	}
	
	public void visit(DeclError declError) {
	}

	public void visit(Exprs exprs) {
		for (int e = 0; e < exprs.numExprs(); e++)
			exprs.expr(e).accept(this);
	}

	public void visit(ExprError exprError) {
	}

	public void visit(ForExpr forExpr) {
		forExpr.var.accept(this);
		forExpr.loBound.accept(this);
		forExpr.hiBound.accept(this);
		forExpr.body.accept(this);
	}

	public void visit(FunCall funCall) {
		for (int a = 0; a < funCall.numArgs(); a++)
			funCall.arg(a).accept(this);
	}

	public void visit(FunDecl funDecl) {
		for (int p = 0; p < funDecl.numPars(); p++)
			funDecl.par(p).accept(this);
		funDecl.type.accept(this);
	}

	public void visit(FunDef funDef) {
		for (int p = 0; p < funDef.numPars(); p++)
			funDef.par(p).accept(this);
		funDef.type.accept(this);
		funDef.body.accept(this);
	}

	public void visit(IfExpr ifExpr) {
		ifExpr.cond.accept(this);
		ifExpr.thenExpr.accept(this);
		ifExpr.elseExpr.accept(this);
	}

	public void visit(ParDecl parDecl) {
		parDecl.type.accept(this);
	}

	public void visit(Program program) {
		program.expr.accept(this);
	}

	public void visit(PtrType ptrType) {
		ptrType.baseType.accept(this);
	}

	public void visit(RecType recType) {
		for (int c = 0; c < recType.numComps(); c++)
			recType.comp(c).accept(this);
	}

	public void visit(TypeDecl typDecl) {
		typDecl.type.accept(this);
	}
	
	public void visit(TypeError typeError) {
	}

	public void visit(TypeName typeName) {
	}

	public void visit(UnExpr unExpr) {
		unExpr.subExpr.accept(this);
	}

	public void visit(VarDecl varDecl) {
		varDecl.type.accept(this);
	}

	public void visit(VarName varName) {
	}

	public void visit(WhereExpr whereExpr) {
		whereExpr.expr.accept(this);
		for (int d = 0; d < whereExpr.numDecls(); d++)
			whereExpr.decl(d).accept(this);
	}

	public void visit(WhileExpr whileExpr) {
		whileExpr.cond.accept(this);
		whileExpr.body.accept(this);
	}

}
