package compiler.phase.seman;

import compiler.common.report.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;

/**
 * Computes the value of simple integer constant expressions.
 * 
 * <p>
 * Simple integer constant expressions consists of integer constants and five
 * basic arithmetic operators (<code>ADD</code>, <code>SUB</code>,
 * <code>MUL</code>, <code>DIV</code>, and <code>MOD</code>).
 * </p>
 * 
 * <p>
 * This is needed during type resolving and type checking to compute the correct
 * array types.
 * </p>
 * 
 * @author sliva
 */
public class EvalValue extends FullVisitor {

    private final Attributes attrs;
    
    public EvalValue(Attributes attrs) {
        this.attrs = attrs;
    }
    
    @Override
    public void visit(AtomExpr atomExpr){
        if(atomExpr.type == AtomExpr.AtomTypes.INTEGER){
            attrs.valueAttr.set(atomExpr,Long.parseLong(atomExpr.value));
        }
    }

    @Override
    public void visit(BinExpr binExpr){
        binExpr.fstExpr.accept(this);
        binExpr.sndExpr.accept(this);
        switch(binExpr.oper){
            case ADD:
                if(attrs.valueAttr.get(binExpr.fstExpr)!=null && attrs.valueAttr.get(binExpr.sndExpr)!=null){
                    attrs.valueAttr.set(binExpr,attrs.valueAttr.get(binExpr.fstExpr)+attrs.valueAttr.get(binExpr.sndExpr));
                }//else{
                    //attrs.valueAttr.set(binExpr,null);
                //}
                break;
            case SUB:
                if(attrs.valueAttr.get(binExpr.fstExpr)!=null && attrs.valueAttr.get(binExpr.sndExpr)!=null){
                    attrs.valueAttr.set(binExpr,attrs.valueAttr.get(binExpr.fstExpr)-attrs.valueAttr.get(binExpr.sndExpr));
                }
                break;
            case MUL:
                if(attrs.valueAttr.get(binExpr.fstExpr)!=null && attrs.valueAttr.get(binExpr.sndExpr)!=null){
                    attrs.valueAttr.set(binExpr,attrs.valueAttr.get(binExpr.fstExpr)*attrs.valueAttr.get(binExpr.sndExpr));
                }
                break;
            case DIV:
                if(attrs.valueAttr.get(binExpr.fstExpr)!=null && attrs.valueAttr.get(binExpr.sndExpr)!=null){
                    attrs.valueAttr.set(binExpr,attrs.valueAttr.get(binExpr.fstExpr)/attrs.valueAttr.get(binExpr.sndExpr));
                }
                break;
            case MOD:
                if(attrs.valueAttr.get(binExpr.fstExpr)!=null && attrs.valueAttr.get(binExpr.sndExpr)!=null){
                    attrs.valueAttr.set(binExpr,attrs.valueAttr.get(binExpr.fstExpr)%attrs.valueAttr.get(binExpr.sndExpr));
                }
                break;
        }
    }

    @Override
    public void visit(UnExpr unExpr){
        unExpr.subExpr.accept(this);
        switch(unExpr.oper){
            case ADD:
                if(attrs.valueAttr.get(unExpr.subExpr)!=null){
                    attrs.valueAttr.set(unExpr,+attrs.valueAttr.get(unExpr.subExpr));
                }
                break;
            case SUB:
                if(attrs.valueAttr.get(unExpr.subExpr)!=null){
                    attrs.valueAttr.set(unExpr,-attrs.valueAttr.get(unExpr.subExpr));
                }
                break;
        }
    }
}
