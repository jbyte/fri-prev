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
				Symbol symWhere = nextSymbol();
				parseDeclarations();
				Symbol symEnd;
				if(laSymbol.token == Symbol.Token.END){
					symEnd = nextSymbol();
				}else{
					Report.warning(laSymbol, "Missing end inserted.");
					symEnd = nextSymbolIsError();
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
		endLog();
	}
	private void parseRelationalExpression_() throws IOException{
		begLog("RelativeExpression'");
		endLog();
	}
	private void parseAdditiveExpression() throws IOException{
		begLog("AdditiveExpression");
		endLog();
	}
	private void parseAdditiveExpression_() throws IOException{
		begLog("AdditiveExpression'");
		endLog();
	}
	private void parseMultiplicativeExpression() throws IOException{
		begLog("MultiplicativeExpression");
		endLog();
	}
	private void parseMultiplicativeExpression_() throws IOException{
		begLog("MultiplicativeExperssion'");
		endLog();
	}
	private void parsePrefixExpression() throws IOException{
		begLog("PrefixExpression");
		endLog();
	}
	private void parsePostfixExpression() throws IOException{
		begLog("PostfixExpression");
		endLog();
	}
	private void parsePostfixExpression_() throws IOException{
		begLog("PostfixExpression'");
		endLog();
	}
	private void parseAtomicExpression() throws IOException{
		begLog("AtomicExpression");
		endLog();
	}
	private void parseArgumentsOpt() throws IOException{
		begLog("ArgumentsOpt");
		endLog();
	}
	private void parseDeclarations() throws IOException{
		begLog("Declarations");
		endLog();
	}
	private void parseDeclarations_() throws IOException{
		begLog("Declarations'");
		endLog();
	}
	private void parseDeclaration() throws IOException{
		begLog("Declaration");
		endLog();
	}
	private void parseTypeDeclaration() throws IOException{
		begLog("TypeDeclaration");
		endLog();
	}
	private void parseFunctionDeclaration() throws IOException{
		begLog("FunctionDeclaration");
		endLog();
	}
	private void parseParametersOpt() throws IOException{
		begLog("ParametersOpt");
		endLog();
	}
	private void parseParameters() throws IOException{
		begLog("Parameters");
		endLog();
	}
	private void parseParameters_() throws IOException{
		begLog("Parameters'");
		endLog();
	}
	private void parseParameter() throws IOException{
		begLog("Parameter");
		endLog();
	}
	private void parseFunctionBodyOpt() throws IOException{
		begLog("FunctionBodyOpt");
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
		endLog();
	}
	private void parseComponents() throws IOException{
		begLog("Components");
		endLog();
	}
	private void parseComponents_() throws IOException{
		begLog("Components'");
		endLog();
	}
	private void parseComponent() throws IOException{
		begLog("Component");
		endLog();
	}


}
