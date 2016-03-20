package compiler.phase.synan;

import java.util.*;
import java.io.IOException;

import org.w3c.dom.*;

import compiler.*;
import compiler.common.logger.*;
import compiler.common.report.*;
import compiler.phase.*;
import compiler.phase.lexan.*;

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
		super(task, "synan");
		this.lexAn = new LexAn(task);
		this.logger.setTransformer(//
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
	public void synAn() throws IOException{
		laSymbol = lexAn.lexAn();
		parseProgram();
		if (laSymbol.token != Symbol.Token.EOF)
			Report.warning(laSymbol, "Unexpected symbol(s) at the end of file.");
	}

	// All these methods are a part of a recursive descent implementation of an
	// LL(1) parser.

	private void parseProgram()  throws IOException{
		begLog("Program");
		parseExpression();
		endLog();
	}

	private void parseExpression() throws IOException{
		begLog("Expression");
		parseAssignmentExpression();
		parseExpression_();
		endLog();
	}

	private void parseExpression_() throws IOException{
		begLog("Expression'");
		switch(laSymbol.token){
			case WHERE:
				nextSymbol();
				parseDeclarations();
				if(laSymbol.token == Symbol.Token.END){
					nextSymbol();
				}else{
					Report.warning(laSymbol, "Missing end inserted.");
					nextSymbolIsError();
				}
				parseExpression_();
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
	}
	private void parseExpressions() throws IOException{
		begLog("Expressions");
		parseExpression();
		parseExpressions_();
		endLog();
	}
	private void parseExpressions_() throws IOException{
		begLog("Expressions'");
		switch(laSymbol.token){
			case COMMA:
				nextSymbol();
				parseExpression();
				parseExpressions_();
				break;
			case CLOSING_PARENTHESIS:
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseAssignmentExpression() throws IOException{
		begLog("AssignmentExpression");
		parseDisjunctiveExpression();
		parseAssignmentExpression_();
		endLog();
	}
	private void parseAssignmentExpression_() throws IOException{
		begLog("AssignmentExpression'");
		switch(laSymbol.token){
			case ASSIGN:
				nextSymbol();
				parseDisjunctiveExpression();
				parseAssignmentExpression_();
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
	}
	private void parseDisjunctiveExpression() throws IOException{
		begLog("DisjunctiveExpression");
		parseConjunctiveExpression();
		parseDisjunctiveExpression_();
		endLog();
	}
	private void parseDisjunctiveExpression_() throws IOException{
		begLog("DisjunctiveExpression'");
		switch(laSymbol.token){
			case OR:
				nextSymbol();
				parseConjunctiveExpression();
				parseDisjunctiveExpression_();
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
	}
	private void parseConjunctiveExpression() throws IOException{
		begLog("ConjunctiveExpression");
		parseRelationalExpression();
		parseConjunctiveExpression_();
		endLog();
	}
	private void parseConjunctiveExpression_() throws IOException{
		begLog("ConjunctiveExpression'");
		switch(laSymbol.token){
			case AND:
				nextSymbol();
				parseRelationalExpression();
				parseConjunctiveExpression_();
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
	}
	private void parseRelationalExpression() throws IOException{
		begLog("RelativeExpression");
		parseAdditiveExpression();
		parseRelationalExpression_();
		endLog();
	}
	private void parseRelationalExpression_() throws IOException{
		begLog("RelativeExpression'");
		switch(laSymbol.token){
			case EQU:
			case NEQ:
			case LTH:
			case GTH:
			case LEQ:
			case GEQ:
				nextSymbol();
				parseAdditiveExpression();
				parseRelationalExpression_();
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
	}
	private void parseAdditiveExpression() throws IOException{
		begLog("AdditiveExpression");
		parseMultiplicativeExpression();
		parseAdditiveExpression_();
		endLog();
	}
	private void parseAdditiveExpression_() throws IOException{
		begLog("AdditiveExpression'");
		switch(laSymbol.token){
			case ADD:
			case SUB:
				nextSymbol();
				parseMultiplicativeExpression();
				parseAdditiveExpression_();
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
	}
	private void parseMultiplicativeExpression() throws IOException{
		begLog("MultiplicativeExpression");
		parsePrefixExpression();
		parseMultiplicativeExpression_();
		endLog();
	}
	private void parseMultiplicativeExpression_() throws IOException{
		begLog("MultiplicativeExperssion'");
		switch(laSymbol.token){
			case MUL:
			case DIV:
			case MOD:
				nextSymbol();
				parsePrefixExpression();
				parseMultiplicativeExpression_();
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
	}
	private void parsePrefixExpression() throws IOException{
		begLog("PrefixExpression");
		switch(laSymbol.token){
			case ADD:
			case SUB:
			case NOT:
			case MEM:
				nextSymbol();
				parsePrefixExpression();
				break;
			case OPENING_BRACKET:
				nextSymbol();
				parseType();
				if(laSymbol.token == Symbol.Token.CLOSING_BRACKET){
					nextSymbol();
				}else{
					Report.warning(laSymbol, "Missing \']\' inserted.");
					nextSymbolIsError();
				}
				parsePrefixExpression();
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
				parsePostfixExpression();
				break;
			default:
				System.out.println(laSymbol.token);
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parsePostfixExpression() throws IOException{
		begLog("PostfixExpression");
		parseAtomicExpression();
		parsePostfixExpression_();
		endLog();
	}
	private void parsePostfixExpression_() throws IOException{
		begLog("PostfixExpression'");
		switch(laSymbol.token){
			case OPENING_BRACKET:
				nextSymbol();
				parseExpression();
				if(laSymbol.token == Symbol.Token.CLOSING_BRACKET){
					nextSymbol();
				}else{
					Report.warning(laSymbol,"Missing \'[\' inserted.");
					nextSymbolIsError();
				}
				parsePostfixExpression_();
				break;
			case DOT:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.IDENTIFIER){
					nextSymbol();
				}else{
					Report.warning(laSymbol,"Missing identifier inserted.");
					nextSymbolIsError();
				}
				parsePostfixExpression_();
				break;
			case VAL:
				nextSymbol();
				parsePostfixExpression_();
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
	}
	private void parseAtomicExpression() throws IOException{
		begLog("AtomicExpression");
		switch(laSymbol.token){
			case CONST_INTEGER:
			case CONST_BOOLEAN:
			case CONST_CHAR:
			case CONST_STRING:
			case CONST_NULL:
			case CONST_NONE:
				nextSymbol();
				break;
			case IDENTIFIER:
				nextSymbol();
				parseArgumentsOpt();
				break;
			case OPENING_PARENTHESIS:
				nextSymbol();
				parseExpressions();
				if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
					nextSymbol();
				}else{
					Report.warning("Missing \')\' inserted.");
					nextSymbolIsError();
				}
				break;
			case IF:
				nextSymbol();
				parseExpression();
				if(laSymbol.token == Symbol.Token.THEN){
					nextSymbol();
				}else{
					Report.warning("Missing \'then\' inserted.");
					nextSymbolIsError();
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.ELSE){
					nextSymbol();
				}else{
					Report.warning("Missing \'else\' inserted.");
					nextSymbolIsError();
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.END){
					nextSymbol();
				}else{
					Report.warning("Missing \'end\' inserted.");
					nextSymbolIsError();
				}
				break;
			case FOR:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.IDENTIFIER){
					nextSymbol();
				}else{
					Report.warning("Missing identifier inserted.");
					nextSymbolIsError();
				}
				if(laSymbol.token == Symbol.Token.ASSIGN){
					nextSymbol();
				}else{
					Report.warning("Missing \'=\' inserted.");
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.COMMA){
					nextSymbol();
				}else{
					Report.warning("Missing \',\' inserted.");
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.COLON){
					nextSymbol();
				}else{
					Report.warning("Missing \':\' inserted.");
					nextSymbolIsError();
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.END){
					nextSymbol();
				}else{
					Report.warning("Missing \'end\' inserted.");
					nextSymbol();
				}
				break;
			case WHILE:
				nextSymbol();
				parseExpression();
				if(laSymbol.token == Symbol.Token.COLON){
					nextSymbol();
				}else{
					Report.warning("Missing \':\' inserted.");
					nextSymbolIsError();
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.END){
					nextSymbol();
				}else{
					Report.warning("Missing \'end\' inserted.");
					nextSymbolIsError();
				}
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseArgumentsOpt() throws IOException{
		begLog("ArgumentsOpt");
		switch(laSymbol.token){
			case OPENING_PARENTHESIS:
				nextSymbol();
				parseExpressions();
				if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
					nextSymbol();
				}else{
					Report.warning("Missing \')\' inserted.");
					nextSymbolIsError();
				}
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
	}
	private void parseDeclarations() throws IOException{
		begLog("Declarations");
		parseDeclaration();
		parseDeclarations_();
		endLog();
	}
	private void parseDeclarations_() throws IOException{
		begLog("Declarations'");
		switch(laSymbol.token){
			case TYP:
			case FUN:
			case VAR:
				parseDeclaration();
				parseDeclarations_();
				break;
			case END:
				break;
			default:
				System.out.println(laSymbol.token);
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseDeclaration() throws IOException{
		begLog("Declaration");
		switch(laSymbol.token){
			case TYP:
				parseTypeDeclaration();
				break;
			case FUN:
				parseFunctionDeclaration();
				break;
			case VAR:
				parseVariableDeclaration();
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseTypeDeclaration() throws IOException{
		begLog("TypeDeclaration");
		switch(laSymbol.token){
			case TYP:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.IDENTIFIER){
					nextSymbol();
				}else{
					Report.warning("Missing identifier inserted.");
					nextSymbolIsError();
				}
				if(laSymbol.token == Symbol.Token.COLON){
					nextSymbol();
				}else{
					Report.warning("Missing \':\' inserted.");
					nextSymbolIsError();
				}
				parseType();
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseFunctionDeclaration() throws IOException{
		begLog("FunctionDeclaration");
		switch(laSymbol.token){
			case FUN:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.IDENTIFIER){
					nextSymbol();
				}else{
					Report.warning("Missing identifier inserted.");
					nextSymbolIsError();
				}
				if(laSymbol.token == Symbol.Token.OPENING_PARENTHESIS){
					nextSymbol();
				}else{
					Report.warning("Missing \'(\' inserted.");
					nextSymbolIsError();
				}
				parseParametersOpt();
				if(laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS){
					nextSymbol();
				}else{
					Report.warning("Missing \')\' inserted.");
					nextSymbolIsError();
				}
				if(laSymbol.token == Symbol.Token.COLON){
					nextSymbol();
				}else{
					Report.warning("Missing \':\' inserted.");
					nextSymbolIsError();
				}
				parseType();
				parseFunctionBodyOpt();
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseParametersOpt() throws IOException{
		begLog("ParametersOpt");
		switch(laSymbol.token){
			case IDENTIFIER:
				parseParameters();
			case CLOSING_PARENTHESIS:
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseParameters() throws IOException{
		begLog("Parameters");
		parseParameter();
		parseParameters_();
		endLog();
	}
	private void parseParameters_() throws IOException{
		begLog("Parameters'");	
		switch(laSymbol.token){
			case IDENTIFIER:
				parseParameter();
				parseParameters_();
			case CLOSING_PARENTHESIS:
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseParameter() throws IOException{
		begLog("Parameter");
		switch(laSymbol.token){
			case IDENTIFIER:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.COLON){
					nextSymbol();
				}else{
					Report.warning("Missing \':\' inserted.");
					nextSymbolIsError();
				}
				parseType();
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseFunctionBodyOpt() throws IOException{
		begLog("FunctionBodyOpt");
		switch(laSymbol.token){
			case ASSIGN:
				nextSymbol();
				parseExpression();
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
	}
	
	private void parseVariableDeclaration() throws IOException{
		begLog("VariableDeclaration");
		switch (laSymbol.token) {
		case VAR: {
			Symbol symVar = nextSymbol();
			Symbol symId;
			if (laSymbol.token == Symbol.Token.IDENTIFIER) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing identifier inserted.");
				symId = nextSymbolIsError();
			}
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				nextSymbolIsError();
			}
			parseType();
			break;
		}
		default:
			throw new InternalCompilerError();
		}
		endLog();
	}
	
	private void parseType() throws IOException{
		begLog("Type");
		switch(laSymbol.token){
			case INTEGER:
			case BOOLEAN:
			case CHAR:
			case STRING:
			case VOID:
			case IDENTIFIER:
				nextSymbol();
				break;
			case ARR:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.OPENING_BRACKET){
					nextSymbol();
				}else{
					Report.warning("Missing \'[\' inserted.");
					nextSymbolIsError();
				}
				parseExpression();
				if(laSymbol.token == Symbol.Token.CLOSING_BRACKET){
					nextSymbol();
				}else{
					Report.warning("Missing \']\' inserted.");
					nextSymbolIsError();
				}
				parseType();
				break;
			case REC:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.OPENING_BRACE){
					nextSymbol();
				}else{
					Report.warning("Missing \'{\' inserted.");
					nextSymbolIsError();
				}
				parseComponents();
				if(laSymbol.token == Symbol.Token.CLOSING_BRACE){
					nextSymbol();
				}else{
					Report.warning("Missing \'}\' inserted.");
					nextSymbolIsError();
				}
				break;
			case PTR:
				nextSymbol();
				parseType();
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseComponents() throws IOException{
		begLog("Components");
		parseComponent();
		parseComponents_();
		endLog();
	}
	private void parseComponents_() throws IOException{
		begLog("Components'");
		switch(laSymbol.token){
			case IDENTIFIER:
				parseComponent();
				parseComponents_();
				break;
			case CLOSING_BRACE:
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}
	private void parseComponent() throws IOException{
		begLog("Component");
		switch(laSymbol.token){
			case IDENTIFIER:
				nextSymbol();
				if(laSymbol.token == Symbol.Token.COLON){
					nextSymbol();
				}else{
					Report.warning("Missing \':\' inserted.");
					nextSymbolIsError();
				}
				parseType();
				break;
			default:
				throw new InternalCompilerError();
		}
		endLog();
	}


}
