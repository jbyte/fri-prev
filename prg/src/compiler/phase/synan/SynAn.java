package compiler.phase.synan;

import java.util.*;
import java.io.IOException;

import org.w3c.dom.*;

import compiler.*;
import compiler.common.logger.*;
import compiler.common.report.*;
import compiler.phase.*;
import compiler.phase.lexan.*;
import compiler.data.ast.*;

/**
 * The syntax analyzer.
 *
 * @author sliva
 */
public class SynAn extends Phase {

    /** The lexical analyzer. */
    private final LexAn lexAn;

    /**
     * Constructs a new syntax analyzer.
     *
     * @param lexAn
     *            The lexical analyzer.
     */
    public SynAn(Task task) {
        super(task, "imcode");
        this.lexAn = new LexAn(task);
        this.logger.setTransformer(
                new Transformer() {
                    // This transformer produces the
                    // left-most derivation.

                    private String nodeName(Node node) {
                        Element element = (Element) node;
                        String nodeName = element.getTagName();
                        if (nodeName.equals("nont")) {
                            return element.getAttribute("name");
                        }
                        if (nodeName.equals("symbol")) {
                            return element.getAttribute("name");
                        }
                        return null;
                    }

                    private void leftMostDer(Node node) {
                        System.out.println("in leftMostDer");
                        if (((Element) node).getTagName().equals("nont")) {
                            String nodeName = nodeName(node);
                            NodeList children = node.getChildNodes();
                            StringBuffer production = new StringBuffer();
                            production.append(nodeName + " -->");
                            for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
                                Node child = children.item(childIdx);
                                String childName = nodeName(child);
                                production.append(" " + childName);
                            }
                            Report.info(production.toString());
                            for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
                                Node child = children.item(childIdx);
                                leftMostDer(child);
                            }
                        }
                    }

                    public Document transform(Document doc) {
                        leftMostDer(doc.getDocumentElement().getFirstChild());
                        return doc;
                    }
                });
    }

    /**
     * Terminates syntax analysis. Lexical analyzer is not closed and, if
     * logging has been requested, this method produces the report by closing
     * the logger.
     */
    @Override
    public void close() {
        lexAn.close();
        super.close();
    }

    /** The parser's lookahead buffer. */
    private Symbol laSymbol;

    /**
     * Reads the next lexical symbol from the source file and stores it in the
     * lookahead buffer (before that it logs the previous lexical symbol, if
     * requested); returns the previous symbol.
     *
     * @return The previous symbol (the one that has just been replaced by the
     *         new symbol).
     */
    private Symbol nextSymbol() throws IOException{
        Symbol symbol = laSymbol;
        symbol.log(logger);
        laSymbol = lexAn.lexAn();
        return symbol;
    }

    /**
     * Logs the error token inserted when a missing lexical symbol has been
     * reported.
     *
     * @return The error token (the symbol in the lookahead buffer is to be used
     *         later).
     */
    private Symbol nextSymbolIsError() {
        Symbol error = new Symbol(Symbol.Token.ERROR, "", new Position("", 0, 0));
        error.log(logger);
        return error;
    }

    /**
     * Starts logging an internal node of the derivation tree.
     *
     * @param nontName
     *            The name of a nonterminal the internal node represents.
     */
    private void begLog(String nontName) {
        if (logger == null)
            return;
        logger.begElement("nont");
        logger.addAttribute("name", nontName);
    }

    /**
     * Ends logging an internal node of the derivation tree.
     */
    private void endLog() {
        if (logger == null)
            return;
        logger.endElement();
    }

    /**
     * The parser.
     *
     * This method performs the syntax analysis of the source file.
     */
    public Program synAn() throws IOException{
        laSymbol = lexAn.lexAn();
        Program prog = parseProgram();
        if (laSymbol.token != Symbol.Token.EOF)
            Report.warning(laSymbol, "Unexpected symbol(s) at the end of file.");

        return prog;
    }

    // All these methods are a part of a recursive descent implementation of an
    // LL(1) parser.

    private Program parseProgram()  throws IOException{
        Symbol tmp = laSymbol;
        begLog("Program");
        Expr expr = parseExpression();
        endLog();
        return new Program(expr,expr);
    }

    private Expr parseExpression() throws IOException{
        begLog("Expression");
        Expr expr = parseAssignmentExpression();
        expr = parseExpression_(expr);
        endLog();
        return expr;
    }

    private Expr parseExpression_(Expr expr) throws IOException{
        begLog("Expression'");
        switch(laSymbol.token){
            case WHERE:
                Symbol tmp = nextSymbol();
                LinkedList<Decl> decls = parseDeclarations();
                if(laSymbol.token == Symbol.Token.END){
                    tmp = nextSymbol();
                }else{
                    tmp = nextSymbolIsError();
                }
                expr = new WhereExpr(new Position(expr,tmp),expr,decls);
                expr = parseExpression_(expr);
                break;
            case END:
            case COMMA:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseExpressions() throws IOException{
        begLog("Expressions");
        Expr expr = parseExpression();
        expr = parseExpressions_(expr);
        endLog();
        return expr;
    }
    private Expr parseExpressions_(Expr expr) throws IOException{
        begLog("Expressions'");
        switch(laSymbol.token){
            case COMMA:
                LinkedList<Expr> exprs = new LinkedList<Expr>();
                if(expr instanceof Exprs){
                    for(int i=0; i<((Exprs)expr).numExprs(); i++){
                        exprs.add(((Exprs)expr).expr(i));
                    }
                }else{
                    exprs.add(expr);
                }
                Symbol tmp = nextSymbol();
                Expr exprL = expr;
                expr = parseExpression();
                exprs.add(expr);
                Expr list = new Exprs(new Position(exprL,expr),exprs);
                expr = parseExpressions_(list);
                break;
            case CLOSING_PARENTHESIS:
                break;
            default:
                //System.err.println(laSymbol.token);
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseAssignmentExpression() throws IOException{
        begLog("AssignmentExpression");
        Expr expr = parseDisjunctiveExpression();
        expr = parseAssignmentExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parseAssignmentExpression_(Expr expr) throws IOException{
        begLog("AssignmentExpression'");
        switch(laSymbol.token){
            case ASSIGN:
                Symbol tmp = nextSymbol();
                Expr exprR = parseDisjunctiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.ASSIGN,expr,exprR);
                expr = parseAssignmentExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseDisjunctiveExpression() throws IOException{
        begLog("DisjunctiveExpression");
        Expr expr = parseConjunctiveExpression();
        expr = parseDisjunctiveExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parseDisjunctiveExpression_(Expr expr) throws IOException{
        begLog("DisjunctiveExpression'");
        switch(laSymbol.token){
            case OR:
                Symbol tmp = nextSymbol();
                Expr exprR = parseConjunctiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.OR,expr,exprR);
                expr = parseDisjunctiveExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseConjunctiveExpression() throws IOException{
        begLog("ConjunctiveExpression");
        Expr expr = parseRelationalExpression();
        expr = parseConjunctiveExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parseConjunctiveExpression_(Expr expr) throws IOException{
        begLog("ConjunctiveExpression'");
        switch(laSymbol.token){
            case AND:
                Symbol tmp = nextSymbol();
                Expr exprR = parseRelationalExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.AND,expr,exprR);
                expr = parseConjunctiveExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case OR:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseRelationalExpression() throws IOException{
        begLog("RelationalExpression");
        Expr expr = parseAdditiveExpression();
        expr = parseRelationalExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parseRelationalExpression_(Expr expr) throws IOException{
        begLog("RelationalExpression'");
        Expr exprR;
        Symbol tmp;
        switch(laSymbol.token){
            case EQU:
                tmp = nextSymbol();
                exprR = parseAdditiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.EQU,expr,exprR);
                expr = parseRelationalExpression_(expr);
                break;
            case NEQ:
                tmp = nextSymbol();
                exprR = parseAdditiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.NEQ,expr,exprR);
                expr = parseRelationalExpression_(expr);
                break;
            case LTH:
                tmp = nextSymbol();
                exprR = parseAdditiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.LTH,expr,exprR);
                expr = parseRelationalExpression_(expr);
                break;
            case GTH:
                tmp = nextSymbol();
                exprR = parseAdditiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.GTH,expr,exprR);
                expr = parseRelationalExpression_(expr);
                break;
            case LEQ:
                tmp = nextSymbol();
                exprR = parseAdditiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.LEQ,expr,exprR);
                expr = parseRelationalExpression_(expr);
                break;
            case GEQ:
                tmp = nextSymbol();
                exprR = parseAdditiveExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.GEQ,expr,exprR);
                expr = parseRelationalExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case OR:
            case AND:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseAdditiveExpression() throws IOException{
        begLog("AdditiveExpression");
        Expr expr = parseMultiplicativeExpression();
        expr = parseAdditiveExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parseAdditiveExpression_(Expr expr) throws IOException{
        begLog("AdditiveExpression'");
        Expr exprR;
        Symbol tmp;
        switch(laSymbol.token){
            case ADD:
                tmp = nextSymbol();
                exprR = parseMultiplicativeExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.ADD,expr,exprR);
                expr = parseAdditiveExpression_(expr);
                break;
            case SUB:
                tmp = nextSymbol();
                exprR = parseMultiplicativeExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.SUB,expr,exprR);
                expr = parseAdditiveExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case OR:
            case AND:
            case EQU:
            case NEQ:
            case LTH:
            case GTH:
            case LEQ:
            case GEQ:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseMultiplicativeExpression() throws IOException{
        begLog("MultiplicativeExpression");
        Expr expr = parsePrefixExpression();
        expr = parseMultiplicativeExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parseMultiplicativeExpression_(Expr expr) throws IOException{
        begLog("MultiplicativeExperssion'");
        Expr exprR;
        Symbol tmp;
        switch(laSymbol.token){
            case MUL:
                tmp = nextSymbol();
                exprR = parsePrefixExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.MUL,expr,exprR);
                expr = parseMultiplicativeExpression_(expr);
                break;
            case DIV:
                tmp = nextSymbol();
                exprR = parsePrefixExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.DIV,expr,exprR);
                expr = parseMultiplicativeExpression_(expr);
                break;
            case MOD:
                tmp = nextSymbol();
                exprR = parsePrefixExpression();
                expr = new BinExpr(new Position(expr,exprR),BinExpr.Oper.MOD,expr,exprR);
                expr = parseMultiplicativeExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case OR:
            case AND:
            case EQU:
            case NEQ:
            case LTH:
            case GTH:
            case LEQ:
            case GEQ:
            case ADD:
            case SUB:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parsePrefixExpression() throws IOException{
        begLog("PrefixExpression");
        Expr expr;
        Symbol tmp;
        switch(laSymbol.token){
            case ADD:
                tmp = nextSymbol();
                expr = parsePrefixExpression();
                expr = new UnExpr(new Position(tmp,expr),UnExpr.Oper.ADD,expr);
                break;
            case SUB:
                tmp = nextSymbol();
                expr = parsePrefixExpression();
                expr = new UnExpr(new Position(tmp,expr),UnExpr.Oper.SUB,expr);
                break;
            case NOT:
                tmp = nextSymbol();
                expr = parsePrefixExpression();
                expr = new UnExpr(new Position(tmp,expr),UnExpr.Oper.NOT,expr);
                break;
            case MEM:
                tmp = nextSymbol();
                expr = parsePrefixExpression();
                expr = new UnExpr(new Position(tmp,expr),UnExpr.Oper.MEM,expr);
                break;
            case OPENING_BRACKET:
                tmp = nextSymbol();
                Type type = parseType();
                Symbol tmpE;
                if(laSymbol.token == Symbol.Token.CLOSING_BRACKET){
                    tmpE = nextSymbol();
                }else{
                    tmpE = nextSymbolIsError();
                }
                expr = parsePrefixExpression();
                expr = new CastExpr(new Position(tmp,tmpE),type,expr);
                break;
            case IDENTIFIER:
            case CONST_INTEGER:
            case CONST_BOOLEAN:
            case CONST_CHAR:
            case CONST_STRING:
            case CONST_NULL:
            case CONST_NONE:
            case OPENING_PARENTHESIS:
            case IF:
            case FOR:
            case WHILE:
                expr = parsePostfixExpression();
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parsePostfixExpression() throws IOException{
        begLog("PostfixExpression");
        Expr expr = parseAtomicExpression();
        expr = parsePostfixExpression_(expr);
        endLog();
        return expr;
    }
    private Expr parsePostfixExpression_(Expr expr) throws IOException{
        begLog("PostfixExpression'");
        Expr exprR;
        Symbol temp;
        switch(laSymbol.token){
            case OPENING_BRACKET:
                temp = nextSymbol();
                exprR = parseExpression();
                Symbol tempR;
                if(laSymbol.token == Symbol.Token.CLOSING_BRACKET){
                    tempR = nextSymbol();
                }else{
                    tempR = nextSymbolIsError();
                }
                expr = new BinExpr(new Position(expr,tempR),BinExpr.Oper.ARR,expr,exprR);
                expr = parsePostfixExpression_(expr);
                break;
            case DOT:
                temp = nextSymbol();
                Symbol tmp = null;
                if(laSymbol.token == Symbol.Token.IDENTIFIER){
                    tmp = laSymbol;
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                expr = new BinExpr(new Position(expr,tmp),BinExpr.Oper.REC,expr,new CompName(tmp,tmp.lexeme));
                expr = parsePostfixExpression_(expr);
                break;
            case VAL:
                temp = nextSymbol();
                expr = new UnExpr(new Position(expr,temp),UnExpr.Oper.VAL,expr);
                expr = parsePostfixExpression_(expr);
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case OR:
            case AND:
            case EQU:
            case NEQ:
            case LTH:
            case GTH:
            case LEQ:
            case GEQ:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case CLOSING_BRACKET:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseAtomicExpression() throws IOException{
        begLog("AtomicExpression");
        Expr expr;
        Symbol sym;
        Symbol tmp = null;
        switch(laSymbol.token){
            case CONST_INTEGER:
                sym = nextSymbol();
                expr = new AtomExpr(sym,AtomExpr.AtomTypes.INTEGER,sym.lexeme);
                break;
            case CONST_BOOLEAN:
                sym = nextSymbol();
                expr = new AtomExpr(sym,AtomExpr.AtomTypes.BOOLEAN,sym.lexeme);
                break;
            case CONST_CHAR:
                sym = nextSymbol();
                expr = new AtomExpr(sym,AtomExpr.AtomTypes.CHAR,sym.lexeme);
                break;
            case CONST_STRING:
                sym = nextSymbol();
                expr = new AtomExpr(sym,AtomExpr.AtomTypes.STRING,sym.lexeme);
                break;
            case CONST_NULL:
                sym = nextSymbol();
                expr = new AtomExpr(sym,AtomExpr.AtomTypes.PTR,sym.lexeme);
                break;
            case CONST_NONE:
                sym = nextSymbol();
                expr = new AtomExpr(sym,AtomExpr.AtomTypes.VOID,sym.lexeme);
                break;
            case IDENTIFIER:
                sym = nextSymbol();
                expr = parseArgumentsOpt();
                //if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
                    //tmp = nextSymbol();
                //}else{
                    //tmp = nextSymbolIsError();
                //}
                LinkedList<Expr> list = new LinkedList<Expr>();
                if(expr!=null){
                    if(expr instanceof Exprs){
                        for(int i=0; i<((Exprs)expr).numExprs(); i++){
                            list.add(((Exprs)expr).expr(i));
                        }
                    }else list.add(expr);
                    expr = new FunCall(new Position(sym,expr),sym.lexeme,list);
                }else expr = new VarName(sym,sym.lexeme);
                break;
            case OPENING_PARENTHESIS:
                sym = nextSymbol();
                expr = parseExpressions();
                if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                break;
            case IF:
                sym = nextSymbol();
                expr = parseExpression();
                if(laSymbol.token == Symbol.Token.THEN){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Expr thenExpr = parseExpression();
                if(laSymbol.token == Symbol.Token.ELSE){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Expr elseExpr = parseExpression();
                if(laSymbol.token == Symbol.Token.END){
                    tmp = nextSymbol();
                }else{
                    tmp = nextSymbolIsError();
                }
                expr = new IfExpr(new Position(sym,tmp),expr,thenExpr,elseExpr);
                break;
            case FOR:
                sym = nextSymbol();
                if(laSymbol.token == Symbol.Token.IDENTIFIER){
                    sym = nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                if(laSymbol.token == Symbol.Token.ASSIGN){
                    nextSymbol();
                }else{
                }
                expr = parseExpression();
                if(laSymbol.token == Symbol.Token.COMMA){
                    nextSymbol();
                }else{
                }
                Expr hi = parseExpression();
                if(laSymbol.token == Symbol.Token.COLON){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Expr body = parseExpression();
                if(laSymbol.token == Symbol.Token.END){
                    tmp = nextSymbol();
                }else{
                    tmp = nextSymbolIsError();
                }
                expr = new ForExpr(new Position(sym,tmp),new VarName(sym,sym.lexeme),expr,hi,body);
                break;
            case WHILE:
                sym = nextSymbol();
                expr = parseExpression();
                if(laSymbol.token == Symbol.Token.COLON){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                body = parseExpression();
                if(laSymbol.token == Symbol.Token.END){
                    tmp = nextSymbol();
                }else{
                    tmp = nextSymbolIsError();
                }
                expr = new WhileExpr(new Position(sym,tmp),expr,body);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseArgumentsOpt() throws IOException{
        begLog("ArgumentsOpt");
        Expr expr = null;
        switch(laSymbol.token){
            case OPENING_PARENTHESIS:
                nextSymbol();
                expr = parseArgumentsOpt_();
                if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                if(expr==null)
                    expr = new Exprs(new Position(laSymbol),new LinkedList<Expr>());
                break;
            case WHERE:
            case END:
            case COMMA:
            case ASSIGN:
            case OR:
            case AND:
            case EQU:
            case NEQ:
            case LTH:
            case GTH:
            case LEQ:
            case GEQ:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case OPENING_BRACKET:
            case CLOSING_BRACKET:
            case DOT:
            case VAL:
            case CLOSING_PARENTHESIS:
            case THEN:
            case ELSE:
            case COLON:
            case TYP:
            case FUN:
            case VAR:
            case EOF:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }
    private Expr parseArgumentsOpt_() throws IOException{
        begLog("ArgimentsOpt'");
        Expr expr = null;
        switch(laSymbol.token){
            case ADD:
            case SUB:
            case NOT:
            case MEM:
            case OPENING_BRACKET:
            case IDENTIFIER:
            case CONST_INTEGER:
            case CONST_BOOLEAN:
            case CONST_CHAR:
            case CONST_STRING:
            case CONST_NULL:
            case CONST_NONE:
            case OPENING_PARENTHESIS:
            case IF:
            case FOR:
            case WHILE:
                expr = parseExpressions();
                break;
            case CLOSING_PARENTHESIS:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;

    }
    private LinkedList<Decl> parseDeclarations() throws IOException{
        begLog("Declarations");
        Decl decl = parseDeclaration();
        LinkedList<Decl> decls = new LinkedList<Decl>();
        decls.add(decl);
        decls = parseDeclarations_(decls);
        endLog();
        return decls;
    }
    private LinkedList<Decl> parseDeclarations_(LinkedList<Decl> decls) throws IOException{
        begLog("Declarations'");
        switch(laSymbol.token){
            case TYP:
            case FUN:
            case VAR:
                Decl decl = parseDeclaration();
                decls.add(decl);
                decls = parseDeclarations_(decls);
                break;
            case END:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return decls;
    }
    private Decl parseDeclaration() throws IOException{
        begLog("Declaration");
        Decl decl;
        switch(laSymbol.token){
            case TYP:
                decl = parseTypeDeclaration();
                break;
            case FUN:
                decl = parseFunctionDeclaration();
                break;
            case VAR:
                decl = parseVariableDeclaration();
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return decl;
    }
    private Decl parseTypeDeclaration() throws IOException{
        begLog("TypeDeclaration");
        Decl decl;
        Symbol tmp;
        switch(laSymbol.token){
            case TYP:
                tmp = nextSymbol();
                Symbol sym = tmp;
                if(laSymbol.token == Symbol.Token.IDENTIFIER){
                    sym = nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                if(laSymbol.token == Symbol.Token.COLON){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Type type = parseType();
                decl = new TypeDecl(new Position(tmp,type),sym.lexeme,type);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return decl;
    }
    private Decl parseFunctionDeclaration() throws IOException{
        begLog("FunctionDeclaration");
        Decl decl;
        Symbol tmp;
        switch(laSymbol.token){
            case FUN:
                tmp = nextSymbol();
                Symbol sym = tmp;
                if(laSymbol.token == Symbol.Token.IDENTIFIER){
                    sym = nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                if(laSymbol.token == Symbol.Token.OPENING_PARENTHESIS){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                LinkedList<ParDecl> list = parseParametersOpt();
                if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                if(laSymbol.token == Symbol.Token.COLON){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Type type = parseType();
                Expr expr = parseFunctionBodyOpt();
                if(expr==null) decl = new FunDecl(new Position(tmp,type),sym.lexeme,list,type);
                else decl = new FunDef(new Position(tmp,expr),sym.lexeme,list,type,expr);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return decl;
    }
    private LinkedList<ParDecl> parseParametersOpt() throws IOException{
        begLog("ParametersOpt");
        LinkedList<ParDecl> list = new LinkedList<ParDecl>();
        switch(laSymbol.token){
            case IDENTIFIER:
                list = parseParameters();
            case CLOSING_PARENTHESIS:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return list;
    }
    private LinkedList<ParDecl> parseParameters() throws IOException{
        begLog("Parameters");
        ParDecl param = parseParameter();
        LinkedList<ParDecl> params = new LinkedList<ParDecl>();
        params.add(param);
        params = parseParameters_(params);
        endLog();
        return params;
    }
    private LinkedList<ParDecl> parseParameters_(LinkedList<ParDecl> list) throws IOException{
        begLog("Parameters'");
        switch(laSymbol.token){
            case IDENTIFIER:
                ParDecl param = parseParameter();
                list.add(param);
                list = parseParameters_(list);
            case CLOSING_PARENTHESIS:
                break;
            case COMMA:
                nextSymbol();
                list = parseParameters_(list);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return list;
    }
    private ParDecl parseParameter() throws IOException{
        begLog("Parameter");
        ParDecl param;
        switch(laSymbol.token){
            case IDENTIFIER:
                Symbol sym = nextSymbol();
                if(laSymbol.token == Symbol.Token.COLON){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Type type = parseType();
                param = new ParDecl(new Position(sym,type),sym.lexeme,type);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return param;
    }
    private Expr parseFunctionBodyOpt() throws IOException{
        begLog("FunctionBodyOpt");
        Expr expr = null;
        switch(laSymbol.token){
            case ASSIGN:
                nextSymbol();
                expr = parseExpression();
                break;
            case TYP:
            case FUN:
            case VAR:
            case END:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return expr;
    }

    private Decl parseVariableDeclaration() throws IOException{
        begLog("VariableDeclaration");
        Decl decl;
        switch (laSymbol.token) {
            case VAR:
                Symbol symVar = nextSymbol();
                Symbol symId;
                if (laSymbol.token == Symbol.Token.IDENTIFIER) {
                    symId = nextSymbol();
                } else {
                    symId = nextSymbolIsError();
                }
                if (laSymbol.token == Symbol.Token.COLON) {
                    nextSymbol();
                } else {
                    nextSymbolIsError();
                }
                Type type = parseType();
                decl = new VarDecl(new Position(symVar,type),symId.lexeme,type);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return decl;
    }

    private Type parseType() throws IOException{
        begLog("Type");
        Type type;
        Symbol sym;
        switch(laSymbol.token){
            case INTEGER:
                sym = nextSymbol();
                type = new AtomType(sym,AtomType.AtomTypes.INTEGER);
                break;
            case BOOLEAN:
                sym = nextSymbol();
                type = new AtomType(sym,AtomType.AtomTypes.BOOLEAN);
                break;
            case CHAR:
                sym = nextSymbol();
                type = new AtomType(sym,AtomType.AtomTypes.CHAR);
                break;
            case STRING:
                sym = nextSymbol();
                type = new AtomType(sym,AtomType.AtomTypes.STRING);
                break;
            case VOID:
                sym = nextSymbol();
                type = new AtomType(sym,AtomType.AtomTypes.VOID);
                break;
            case IDENTIFIER:
                sym = nextSymbol();
                type = new TypeName(sym,sym.lexeme);
                break;
            case ARR:
                sym = nextSymbol();
                if(laSymbol.token == Symbol.Token.OPENING_BRACKET){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Expr expr = parseExpression();
                if(laSymbol.token == Symbol.Token.CLOSING_BRACKET){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                type = parseType();
                type = new ArrType(new Position(sym,type),expr,type);
                break;
            case REC:
                sym = nextSymbol();
                Symbol tmp;
                if(laSymbol.token == Symbol.Token.OPENING_BRACE){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                LinkedList<CompDecl> list = parseComponents();
                if(laSymbol.token == Symbol.Token.CLOSING_BRACE){
                    tmp = nextSymbol();
                }else{
                    tmp = nextSymbolIsError();
                }
                type = new RecType(new Position(sym,tmp),list);
                break;
            case PTR:
                sym = nextSymbol();
                type = parseType();
                type = new PtrType(new Position(sym,type),type);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return type;
    }
    private LinkedList<CompDecl> parseComponents() throws IOException{
        begLog("Components");
        CompDecl comp = parseComponent();
        LinkedList<CompDecl> comps = new LinkedList<CompDecl>();
        comps.add(comp);
        comps = parseComponents_(comps);
        endLog();
        return comps;
    }
    private LinkedList<CompDecl> parseComponents_(LinkedList<CompDecl> list) throws IOException{
        begLog("Components'");
        switch(laSymbol.token){
            case COMMA:
                nextSymbol();
                CompDecl comp = parseComponent();
                list.add(comp);
                list = parseComponents_(list);
                break;
            case CLOSING_BRACE:
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return list;
    }
    private CompDecl parseComponent() throws IOException{
        begLog("Component");
        CompDecl comp;
        switch(laSymbol.token){
            case IDENTIFIER:
                Symbol sym = nextSymbol();
                if(laSymbol.token == Symbol.Token.COLON){
                    nextSymbol();
                }else{
                    nextSymbolIsError();
                }
                Type type = parseType();
                comp = new CompDecl(new Position(sym,type),sym.lexeme,type);
                break;
            default:
                throw new InternalCompilerError();
        }
        endLog();
        return comp;
    }


}
