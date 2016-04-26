package compiler.phase.seman;

import compiler.common.report.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;

/**
 * Declaration resolver.
 *
 * <p>
 * Declaration resolver maps each AST node denoting a
 * {@link compiler.data.ast.Declarable} name to the declaration where
 * this name is declared. In other words, it links each use of each name to a
 * declaration of that name.
 * </p>
 *
 * @author sliva
 */
public class EvalDecl extends FullVisitor {

    private final Attributes attrs;

    private int iteration;
    private boolean tmp;

    public EvalDecl(Attributes attrs) {
        this.attrs = attrs;
        this.iteration = 1;
        this.tmp = false;
    }

    /** The symbol table. */
    private SymbolTable symbolTable = new SymbolTable();

    @Override
    public void visit(BinExpr binExpr){
        binExpr.fstExpr.accept(this);
        if(binExpr.fstExpr instanceof Declarable && binExpr.oper == BinExpr.Oper.REC){
            if(iteration==2){
                Decl decl = attrs.declAttr.get((Declarable)binExpr.fstExpr);
                if(decl.type instanceof TypeName)
                    symbolTable.enterNamespace(((TypeName)decl.type).name());
                //if(attrs.declAttr.get((Declarable)binExpr.fstExpr) instanceof TypeDecl)
                    //symbolTable.enterNamespace(attrs.declAttr.get(((Declarable)binExpr.fstExpr)).name);
                else
                    symbolTable.enterNamespace(((Declarable)binExpr.fstExpr).name());
                binExpr.sndExpr.accept(this);
                symbolTable.leaveNamespace();
            }
        }else binExpr.sndExpr.accept(this);
    }

    @Override
    public void visit(CompDecl compDecl){
        if(compDecl.type instanceof RecType){
            symbolTable.enterNamespace(compDecl.name);
            compDecl.type.accept(this);
            symbolTable.leaveNamespace();
        }else compDecl.type.accept(this);
        try{
            if(iteration==1)
                symbolTable.insDecl(symbolTable.newNamespace(""),compDecl.name,compDecl);
        }catch(CannotInsNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(CompName compName){
        try{
            if(iteration==2)
                attrs.declAttr.set(compName,symbolTable.fndDecl(symbolTable.newNamespace(""),compName.name()));
        }catch(CannotFndNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(FunCall funCall){
        for (int a = 0; a < funCall.numArgs(); a++)
            funCall.arg(a).accept(this);
        try{
            if(iteration==2)
                attrs.declAttr.set(funCall,symbolTable.fndDecl(funCall.name()));
        }catch(CannotFndNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(FunDecl funDecl){
        symbolTable.enterScope();
        for (int p = 0; p < funDecl.numPars(); p++)
            funDecl.par(p).accept(this);
        symbolTable.leaveScope();
        funDecl.type.accept(this);
        try{
            if(iteration==1)
                symbolTable.insDecl(symbolTable.newNamespace(""),funDecl.name,funDecl);
        }catch(CannotInsNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(FunDef funDef){
        symbolTable.enterScope();
        for (int p = 0; p < funDef.numPars(); p++)
            funDef.par(p).accept(this);
        int tmp = iteration;
        if(iteration==2)
            funDef.body.accept(this);
        iteration = tmp;
        if(iteration==2)
            symbolTable.leaveScope();
        funDef.type.accept(this);
        try{
            if(iteration==1)
                symbolTable.insDecl(symbolTable.newNamespace(""),funDef.name,funDef);
        }catch(CannotInsNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(ParDecl parDecl){
        if(parDecl.type instanceof RecType){
            symbolTable.enterNamespace(parDecl.name);
            parDecl.type.accept(this);
            symbolTable.leaveNamespace();
        }else parDecl.type.accept(this);
        try{
            if(iteration==1)
                symbolTable.insDecl(symbolTable.newNamespace(""),parDecl.name,parDecl);
        }catch(CannotInsNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(TypeDecl typeDecl){
        if(typeDecl.type instanceof RecType){
            symbolTable.enterNamespace(typeDecl.name);
            typeDecl.type.accept(this);
            symbolTable.leaveNamespace();
        }else typeDecl.type.accept(this);
        try{
            if(iteration==1)
                symbolTable.insDecl(symbolTable.newNamespace(""),typeDecl.name,typeDecl);
        }catch(CannotInsNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(TypeName typeName){
        try{
            if(iteration==2)
                attrs.declAttr.set(typeName,symbolTable.fndDecl(typeName.name()));
        }catch(CannotFndNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(VarDecl varDecl){
        if(varDecl.type instanceof RecType){
            symbolTable.enterNamespace(varDecl.name);
            varDecl.type.accept(this);
            symbolTable.leaveNamespace();
        }else varDecl.type.accept(this);
        try{
            if(iteration==1)
                symbolTable.insDecl(symbolTable.newNamespace(""),varDecl.name,varDecl);
        }catch(CannotInsNameDecl ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void visit(VarName varName){
        try{
            if(attrs.declAttr.get(varName)==null){
                Decl decl = symbolTable.fndDecl(varName.name());
                if(decl instanceof ParDecl && iteration==1){
                    attrs.declAttr.set(varName,decl);
                    tmp = true;
                }else if(iteration==2)
                    if(attrs.declAttr.get(varName)==null)
                        attrs.declAttr.set(varName,decl);
            }
        }catch(CannotFndNameDecl ex){
            if(iteration==2 && !tmp)
                ex.printStackTrace();
        }
        //try{
            //if(iteration==2 && !tmp)
                //attrs.declAttr.set(varName,symbolTable.fndDecl(varName.name()));
        //}catch(CannotFndNameDecl ex){
            //ex.printStackTrace();
        //}
    }

    @Override
    public void visit(WhereExpr whereExpr){
        symbolTable.enterScope();
        for (int d = 0; d < whereExpr.numDecls(); d++){
            iteration = 1;
            whereExpr.decl(d).accept(this);
        }
        for (int d = 0; d < whereExpr.numDecls(); d++){
            iteration = 2;
            whereExpr.decl(d).accept(this);
        }
        //iteration = 2;
        whereExpr.expr.accept(this);
        symbolTable.leaveScope();
        iteration = 1;
    }
}

