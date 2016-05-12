package compiler.phase.imcode;

import java.util.*;

import compiler.common.report.*;
import compiler.data.acc.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.frg.*;
import compiler.data.frm.*;
import compiler.data.typ.*;
import compiler.data.imc.*;

/**
 * Evaluates intermediate code.
 *
 * @author sliva
 */
public class EvalImcode extends FullVisitor {

    private final Attributes attrs;

    private final HashMap<String, Fragment> fragments;

    private Stack<CodeFragment> codeFragments = new Stack<CodeFragment>();

    public EvalImcode(Attributes attrs, HashMap<String, Fragment> fragments) {
        this.attrs = attrs;
        this.fragments = fragments;
    }

    @Override
    public void visit(ArrType arrType) {
        arrType.size.accept(this);
        arrType.elemType.accept(this);
    }

    @Override
    public void visit(AtomExpr atomExpr) {
        switch (atomExpr.type) {
        case INTEGER:
            try {
                long value = Long.parseLong(atomExpr.value);
                attrs.imcAttr.set(atomExpr, new CONST(value));
            } catch (NumberFormatException ex) {
                Report.warning(atomExpr, "Illegal integer constant.");
            }
            break;
        case BOOLEAN:
            if (atomExpr.value.equals("true"))
                attrs.imcAttr.set(atomExpr, new CONST(1));
            if (atomExpr.value.equals("false"))
                attrs.imcAttr.set(atomExpr, new CONST(0));
            break;
        case CHAR:
            if (atomExpr.value.charAt(1) == '\\'){
                char tmp = atomExpr.value.charAt(2);
                switch(atomExpr.value.charAt(2)){
            		case 'n':
            			tmp = '\n';
            			break;
            		case 't':
            			tmp = '\t';
            			break;
            	}
                attrs.imcAttr.set(atomExpr, new CONST(tmp));
            }
            else
                attrs.imcAttr.set(atomExpr, new CONST(atomExpr.value.charAt(1)));
            break;
        case STRING:
            String label = LABEL.newLabelName();
            attrs.imcAttr.set(atomExpr, new NAME(label));
            ConstFragment fragment = new ConstFragment(label, atomExpr.value);
            attrs.frgAttr.set(atomExpr, fragment);
            fragments.put(fragment.label, fragment);
            break;
        case PTR:
            attrs.imcAttr.set(atomExpr, new CONST(0));
            break;
        case VOID:
            attrs.imcAttr.set(atomExpr, new NOP());
            break;
        }
    }

    @Override
    public void visit(AtomType atomType) {
    }

    @Override
    public void visit(BinExpr binExpr) {
        binExpr.fstExpr.accept(this);
        binExpr.sndExpr.accept(this);
        IMCExpr fstExpr = (IMCExpr)attrs.imcAttr.get(binExpr.fstExpr);
        IMCExpr sndExpr = (IMCExpr)attrs.imcAttr.get(binExpr.sndExpr);

        switch(binExpr.oper){
            case OR:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.OR,fstExpr,sndExpr));
                break;
            case AND:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.AND,fstExpr,sndExpr));
                break;
            case EQU:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.EQU,fstExpr,sndExpr));
                break;
            case NEQ:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.NEQ,fstExpr,sndExpr));
                break;
            case LTH:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.LTH,fstExpr,sndExpr));
                break;
            case GTH:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.GTH,fstExpr,sndExpr));
                break;
            case LEQ:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.LEQ,fstExpr,sndExpr));
                break;
            case GEQ:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.GEQ,fstExpr,sndExpr));
                break;
            case ADD:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.ADD,fstExpr,sndExpr));
                break;
            case SUB:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.SUB,fstExpr,sndExpr));
                break;
            case MUL:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.MUL,fstExpr,sndExpr));
                break;
            case DIV:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.DIV,fstExpr,sndExpr));
                break;
            case MOD:
                attrs.imcAttr.set(binExpr,new BINOP(BINOP.Oper.MOD,fstExpr,sndExpr));
                break;
            case ASSIGN:
                attrs.imcAttr.set(binExpr,new MOVE(fstExpr,sndExpr));
                break;
            case ARR:
                Typ typ = attrs.typAttr.get(binExpr);
                attrs.imcAttr.set(binExpr,new MEM(new BINOP(BINOP.Oper.ADD,fstExpr,new BINOP(BINOP.Oper.MUL,sndExpr,new CONST(typ.size()))),typ.size()));
                break;
            case REC:
                Typ rectyp = attrs.typAttr.get(binExpr);
                attrs.imcAttr.set(binExpr,new MEM(new BINOP(BINOP.Oper.ADD,fstExpr,sndExpr),rectyp.size()));
                break;
        }
    }

    @Override
    public void visit(CastExpr castExpr) {
        castExpr.type.accept(this);
        castExpr.expr.accept(this);

        Typ typ = attrs.typAttr.get(castExpr);
        IMCExpr expr = (IMCExpr)attrs.imcAttr.get(castExpr.expr);
        attrs.imcAttr.set(castExpr,new MEM(expr,typ.size()));
    }

    @Override
    public void visit(CompDecl compDecl) {
        compDecl.type.accept(this);
    }

    @Override
    public void visit(CompName compName) {
        Decl decl = attrs.declAttr.get(compName);
        OffsetAccess acc = (OffsetAccess)attrs.accAttr.get((VarDecl)decl);

        attrs.imcAttr.set(compName,new CONST(acc.offset));
    }

    @Override
    public void visit(DeclError declError) {
    }

    @Override
    public void visit(Exprs exprs) {
        for (int e = 0; e < exprs.numExprs(); e++)
            exprs.expr(e).accept(this);

        Vector<IMCStmt> stmts = new Vector<IMCStmt>();
        for(int e=0; e<exprs.numExprs(); e++){
            IMC tmp = attrs.imcAttr.get(exprs.expr(e));
            IMCStmt add = null;
            if(tmp instanceof IMCStmt)
                add = (IMCStmt)tmp;
            else if(tmp instanceof IMCExpr)
                add = new ESTMT((IMCExpr)tmp);

            stmts.add(add);
        }
        attrs.imcAttr.set(exprs,new STMTS(stmts));
    }

    @Override
    public void visit(ExprError exprError) {
    }

    @Override
    public void visit(ForExpr forExpr) {
        forExpr.var.accept(this);
        forExpr.loBound.accept(this);
        forExpr.hiBound.accept(this);
        forExpr.body.accept(this);

        IMCExpr var = (IMCExpr)attrs.imcAttr.get(forExpr.var);
        IMCExpr lo = (IMCExpr)attrs.imcAttr.get(forExpr.loBound);
        IMCExpr hi = (IMCExpr)attrs.imcAttr.get(forExpr.hiBound);
        IMC tmp = attrs.imcAttr.get(forExpr.body);

        String pos = LABEL.newLabelName();
        String neg = LABEL.newLabelName();
        String loop = LABEL.newLabelName();

        LABEL lpos = new LABEL(pos);
        LABEL lneg = new LABEL(neg);
        LABEL lloop = new LABEL(loop);

        MOVE ass = new MOVE(var,lo);
        CJUMP cjump = new CJUMP(new BINOP(BINOP.Oper.LEQ,var,hi),pos,neg);
        IMCStmt body = null;
        if(tmp instanceof IMCStmt)
            body = (IMCStmt)tmp;
        else if(tmp instanceof IMCExpr)
            body = new ESTMT((IMCExpr)tmp);
        MOVE inc = new MOVE(var,new BINOP(BINOP.Oper.ADD,var,new CONST(1)));
        JUMP jump = new JUMP(loop);

        Vector<IMCStmt> stmts = new Vector<IMCStmt>();
        stmts.add(ass);
        stmts.add(lloop);
        stmts.add(cjump);
        stmts.add(lpos);
        stmts.add(body);
        stmts.add(inc);
        stmts.add(jump);
        stmts.add(lneg);

        attrs.imcAttr.set(forExpr,new STMTS(stmts));
    }

    @Override
    public void visit(FunCall funCall) {
        for (int a = 0; a < funCall.numArgs(); a++)
            funCall.arg(a).accept(this);

        Vector<IMCExpr> args = new Vector<IMCExpr>();
        Vector<Long> widths = new Vector<Long>();
        for(int a=0; a<funCall.numArgs(); a++){
            IMCExpr expr = (IMCExpr)attrs.imcAttr.get(funCall.arg(a));
            Typ typ = attrs.typAttr.get(funCall.arg(a));

            args.add(expr);
            widths.add(new Long(typ.size()));
        }

        Decl decl = attrs.declAttr.get(funCall);
        if(decl instanceof FunDef){
            Frame frame = attrs.frmAttr.get((FunDef)decl);
            attrs.imcAttr.set(funCall,new CALL(frame.label,args,widths));
        }else if(decl instanceof FunDecl)
            attrs.imcAttr.set(funCall,new CALL("_"+funCall.name(),args,widths));
    }

    @Override
    public void visit(FunDecl funDecl) {
        for (int p = 0; p < funDecl.numPars(); p++)
            funDecl.par(p).accept(this);
        funDecl.type.accept(this);
    }

    @Override
    public void visit(FunDef funDef) {
        Frame frame = attrs.frmAttr.get(funDef);
        int FP = TEMP.newTempName();
        int RV = TEMP.newTempName();
        CodeFragment tmpFragment = new CodeFragment(frame, FP, RV, null);
        codeFragments.push(tmpFragment);

        for (int p = 0; p < funDef.numPars(); p++)
            funDef.par(p).accept(this);
        funDef.type.accept(this);
        funDef.body.accept(this);

        codeFragments.pop();
        //IMCExpr expr = (IMCExpr) attrs.imcAttr.get(funDef.body);
        IMC tmp = attrs.imcAttr.get(funDef.body);
        IMCExpr expr = null;
        if(tmp instanceof IMCExpr)
            expr = (IMCExpr)tmp;
        else if(tmp instanceof IMCStmt){
            //expr = new SEXPR((IMCStmt)tmp,new NOP());
            ESTMT ret = null;
            int i = 1;
            while(((STMTS)tmp).stmts(((STMTS)tmp).numStmts()-i) instanceof LABEL) i++;
            if(((STMTS)tmp).stmts(((STMTS)tmp).numStmts()-i) instanceof STMTS)
                expr = new NOP();
            else{
                ret = ((ESTMT)((STMTS)tmp).stmts(((STMTS)tmp).numStmts()-i));
                expr = ret.expr;
            }
            expr = new SEXPR((IMCStmt)tmp,expr);
        }

        MOVE move = new MOVE(new TEMP(RV), expr);
        Fragment fragment = new CodeFragment(tmpFragment.frame, tmpFragment.FP, tmpFragment.RV, move);
        attrs.frgAttr.set(funDef, fragment);
        attrs.imcAttr.set(funDef, move);
        fragments.put(fragment.label, fragment);
    }

    @Override
    public void visit(IfExpr ifExpr) {
        ifExpr.cond.accept(this);
        ifExpr.thenExpr.accept(this);
        ifExpr.elseExpr.accept(this);

        String pos = LABEL.newLabelName();
        String neg = LABEL.newLabelName();
        String end = LABEL.newLabelName();

        LABEL lpos = new LABEL(pos);
        LABEL lneg = new LABEL(neg);
        LABEL lend = new LABEL(end);

        IMCExpr cond = (IMCExpr)attrs.imcAttr.get(ifExpr.cond);
        IMC tmpThen = attrs.imcAttr.get(ifExpr.thenExpr);
        IMC tmpElse = attrs.imcAttr.get(ifExpr.elseExpr);

        CJUMP cjump = new CJUMP(cond,pos,neg);
        IMCStmt then = null;
        IMCStmt els = null;
        JUMP jump = new JUMP(end);

        if(tmpThen instanceof IMCStmt)
            then = (IMCStmt)tmpThen;
        else if(tmpThen instanceof IMCExpr)
            then = new ESTMT((IMCExpr)tmpThen);

        if(tmpElse instanceof IMCStmt)
            els = (IMCStmt)tmpElse;
        else if(tmpElse instanceof IMCExpr)
            els = new ESTMT((IMCExpr)tmpElse);

        Vector<IMCStmt> stmts = new Vector<IMCStmt>();
        stmts.add(cjump);
        stmts.add(lpos);
        stmts.add(then);
        stmts.add(jump);
        stmts.add(lneg);
        stmts.add(els);
        stmts.add(lend);

        attrs.imcAttr.set(ifExpr,new STMTS(stmts));
    }

    @Override
    public void visit(ParDecl parDecl) {
        parDecl.type.accept(this);
    }

    @Override
    public void visit(Program program) {
        program.expr.accept(this);
        Frame fr = new Frame(0,"_",0,0,0,0,0);
        IMC prog = attrs.imcAttr.get(program.expr);
        attrs.imcAttr.set(program,prog);

        IMCStmt tmp = null;
        if(prog instanceof IMCStmt)
            tmp = (IMCStmt)prog;
        else if(prog instanceof IMCExpr)
            tmp = new ESTMT((IMCExpr)prog);

        Fragment frag = new CodeFragment(fr,0,0,tmp);
        attrs.frgAttr.set(program,frag);

        fragments.put(frag.label, frag);
    }

    @Override
    public void visit(PtrType ptrType) {
        ptrType.baseType.accept(this);
    }

    @Override
    public void visit(RecType recType) {
        for (int c = 0; c < recType.numComps(); c++)
            recType.comp(c).accept(this);
    }

    @Override
    public void visit(TypeDecl typDecl) {
        typDecl.type.accept(this);
    }

    @Override
    public void visit(TypeError typeError) {
    }

    @Override
    public void visit(TypeName typeName) {
    }

    @Override
    public void visit(UnExpr unExpr) {
        unExpr.subExpr.accept(this);
        IMCExpr subExpr = (IMCExpr)attrs.imcAttr.get(unExpr.subExpr);
        switch(unExpr.oper){
            case ADD:
                attrs.imcAttr.set(unExpr, new UNOP(UNOP.Oper.ADD, subExpr));
                break;
            case SUB:
                attrs.imcAttr.set(unExpr, new UNOP(UNOP.Oper.SUB, subExpr));
                break;
            case NOT:
                attrs.imcAttr.set(unExpr, new UNOP(UNOP.Oper.NOT, subExpr));
                break;
            case VAL:
                MEM tmp = (MEM)subExpr;
                attrs.imcAttr.set(unExpr, tmp.addr);
                break;
            case MEM:
                Typ typ = attrs.typAttr.get(unExpr);
                attrs.imcAttr.set(unExpr, new MEM(subExpr,typ.size()));
                break;
        }
    }

    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type.accept(this);
    }

    @Override
    public void visit(VarName varName) {
        Typ typ = attrs.typAttr.get(varName);
        Decl decl = attrs.declAttr.get(varName);
        Access acc = attrs.accAttr.get((VarDecl)decl);
        if(acc instanceof StaticAccess)
            attrs.imcAttr.set(varName,new MEM(new NAME("_"+varName.name()),typ.size()));
        else if(acc instanceof OffsetAccess)
            attrs.imcAttr.set(varName,new MEM(new BINOP(BINOP.Oper.ADD,new TEMP(TEMP.newTempName()),new CONST(-((OffsetAccess)acc).offset)),typ.size()));

        Fragment frag = new DataFragment("_"+varName.name(),typ.size());
        attrs.frgAttr.set(varName,frag);
        fragments.put(frag.label, frag);
    }

    @Override
    public void visit(WhereExpr whereExpr) {
        whereExpr.expr.accept(this);
        for (int d = 0; d < whereExpr.numDecls(); d++)
            whereExpr.decl(d).accept(this);

        IMC expr = attrs.imcAttr.get(whereExpr.expr);
        attrs.imcAttr.set(whereExpr,expr);
    }

    @Override
    public void visit(WhileExpr whileExpr) {
        whileExpr.cond.accept(this);
        whileExpr.body.accept(this);

        String pos = LABEL.newLabelName();
        String neg = LABEL.newLabelName();
        String loop = LABEL.newLabelName();

        LABEL lloop = new LABEL(loop);
        LABEL lpos = new LABEL(pos);
        LABEL lneg = new LABEL(neg);

        IMCExpr cond = (IMCExpr)attrs.imcAttr.get(whileExpr.cond);
        CJUMP cjump = new CJUMP(cond,pos,neg);
        JUMP jump = new JUMP(loop);

        IMC tmp = attrs.imcAttr.get(whileExpr.body);
        IMCStmt body = null;
        if(tmp instanceof IMCExpr)
            body = new ESTMT((IMCExpr)tmp);
        else if(tmp instanceof IMCStmt)
            body = (IMCStmt)tmp;

        Vector<IMCStmt> stmts = new Vector<IMCStmt>();
        stmts.add(lloop);
        stmts.add(cjump);
        stmts.add(lpos);
        stmts.add(body);
        stmts.add(jump);
        stmts.add(lneg);

        attrs.imcAttr.set(whileExpr,new STMTS(stmts));
    }

}
