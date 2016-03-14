package compiler.phase.lexan;

import java.io.*;

import compiler.*;
import compiler.common.report.*;
import compiler.phase.*;

/**
 * The lexical analyzer.
 * 
 * @author sliva
 */
public class LexAn extends Phase {

	/** The source file. */
	private FileReader srcFile;

	/** The source file name. */
	private String srcName;

	/** Current position in the file. */
	private int line = 1;
	private int col = 0;

	/** Temporary character */
	private char tempc = '\0';

	/**
	 * Constructs a new lexical analyzer.
	 * 
	 * Opens the source file and prepares the buffer. If logging is requested,
	 * sets up the logger.
	 * 
	 * @param task.srcFName
	 *            The name of the source file name.
	 */
	public LexAn(Task task) {
		super(task, "lexan");

		// Open the source file.
		try {
			srcName = this.task.srcFName;
			srcFile = new FileReader(this.task.srcFName);
		} catch (FileNotFoundException ex) {
			throw new CompilerError("Source file '" + this.task.srcFName + "' not found.");
		}
	}

	/**
	 * Terminates lexical analysis. Closes the source file and, if logging has
	 * been requested, this method produces the report by closing the logger.
	 */
	@Override
	public void close() {
		// Close the source file.
		if (srcFile != null) {
			try {
				srcFile.close();
			} catch (IOException ex) {
				Report.warning("Source file '" + task.srcFName + "' cannot be closed.");
			}
		}
		super.close();
	}

	/**
	 * Returns the next lexical symbol from the source file.
	 * 
	 * @return The next lexical symbol.
	 */
	public Symbol lexAn() throws IOException {
		//int tline = 0, tcol = 0;
		//while(tline<line){
			//if(srcFile.read()=='\n')tline++;
		//}
		//while(tcol<col){
			//srcFile.read();
			//tcol++;
		//}

		char c = '\0';
		if(tempc=='\0' || tempc=='\r' || tempc=='\n' || tempc=='\t' || tempc==' ' || tempc=='#'){
			while(srcFile.ready()){
				if(tempc!='#')c = (char)srcFile.read();
				else c = tempc;
				if(c==' '){
					col++;
					continue;
				}else if(c=='\t'){
					col+=4;
					continue;
				}else if(c=='\n'){
					line++;
					col=0;
					continue;
				}else if(c=='\r'){
					continue;
				}else if(c=='#'){
					do{
						c = (char)srcFile.read();
					}while(c!='\n');
					col = 0;
					line++;
				}else break;
			}
			tempc = '\0';
		}else{
			c = tempc;
			tempc = '\0';
			if(c=='#'){
				do{
					c = (char)srcFile.read();
				}while(c!='\n');
				col = 0;
				line++;
			}
		}

		if(((int)c)==-1 || ((int)c)==0)
			return log(new Symbol(Symbol.Token.EOF,new Position(srcName,line,col)));
		col++;

		switch(c){
			case '+':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.ADD,new Position(srcName,line,col)));
			case '&':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.AND,new Position(srcName,line,col)));
			case '=':
				if((c=(char)srcFile.read())=='=')
					return log(new Symbol(Symbol.Token.EQU,new Position(srcName,line,col)));
				else{
					tempc = c;
					return log(new Symbol(Symbol.Token.ASSIGN,new Position(srcName,line,col)));
				}
			case ':':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.COLON,new Position(srcName,line,col)));
			case ',':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.COMMA,new Position(srcName,line,col)));
			case '}':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.CLOSING_BRACE,new Position(srcName,line,col)));
			case ']':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.CLOSING_BRACKET,new Position(srcName,line,col)));
			case ')':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.CLOSING_PARENTHESIS,new Position(srcName,line,col)));
			case '.':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.DOT,new Position(srcName,line,col)));
			case '/':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.DIV,new Position(srcName,line,col)));
			case '>':
				if((c=(char)srcFile.read())=='=')
					return log(new Symbol(Symbol.Token.GEQ,new Position(srcName,line,col)));
				else{
					tempc = c;
					return log(new Symbol(Symbol.Token.GTH,new Position(srcName,line,col)));
				}
			case '<':	
				if((c=(char)srcFile.read())=='=')
					return log(new Symbol(Symbol.Token.LEQ,new Position(srcName,line,col)));
				else{
					tempc = c;
					return log(new Symbol(Symbol.Token.LTH,new Position(srcName,line,col)));
				}
			case '@':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.MEM,new Position(srcName,line,col)));
			case '%':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.MOD,new Position(srcName,line,col)));
			case '*':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.MUL,new Position(srcName,line,col)));
			case '!':
				if((c=(char)srcFile.read())=='=')
					return log(new Symbol(Symbol.Token.NEQ,new Position(srcName,line,col)));
				else{
					tempc = c;
					return log(new Symbol(Symbol.Token.NOT,new Position(srcName,line,col)));
				}
			case '{':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.OPENING_BRACE,new Position(srcName,line,col)));
			case '[':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.OPENING_BRACKET,new Position(srcName,line,col)));
			case '(':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.OPENING_PARENTHESIS,new Position(srcName,line,col)));
			case '|':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.OR,new Position(srcName,line,col)));
			case '-':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.SUB,new Position(srcName,line,col)));
			case '^':
				tempc = '\0';
				return log(new Symbol(Symbol.Token.VAL,new Position(srcName,line,col)));
			case '"':
				String str = "";
				int i = 1;
				while((c=(char)srcFile.read())!='\"'){
					col++;
					i++;
					if((int)c>=32 && (int)c<=126){
						if(c=='\n' || c=='#') throw new CompilerError("Unexpected end of string.");
						else if(c=='\\'){
							c = (char)srcFile.read();
							col++;
							i++;
							if(c!='\\' && c!='\'' && c!='\"' && c!='t' && c!='n')
								throw new CompilerError("Invalid escape sequence: "+line+","+col);
							else str+="\\"+c;
						}else str+=c;
					}else throw new CompilerError("Invalid character name: "+(int)c+"("+c+").");
				}
				return log(new Symbol(Symbol.Token.CONST_STRING,"\""+str+"\"",new Position(srcName,line,col-i,srcName,line,col)));
			case '\'':
				int tmp = srcFile.read();
				col++;
				if(tmp>=32 && tmp<=126){
					if((char)tmp=='\\'){
						tmp = srcFile.read();
						col++;
						if((char)tmp=='\\' && (char)srcFile.read()=='\''){
							return log(new Symbol(Symbol.Token.CONST_CHAR,"\'\\\\\'",
								new Position(srcName,line,col-2,srcName,line,col+1)));
						}else if((char)tmp=='\'' && (char)srcFile.read()=='\''){
							return log(new Symbol(Symbol.Token.CONST_CHAR,"\'\\\'\'",
								new Position(srcName,line,col-2,srcName,line,col+1)));
						}else if((char)tmp=='\"' && (char)srcFile.read()=='\''){
							return log(new Symbol(Symbol.Token.CONST_CHAR,"\'\\\"\'",
								new Position(srcName,line,col-2,srcName,line,col+1)));
						}else if((char)tmp=='t' && (char)srcFile.read()=='\''){
							return log(new Symbol(Symbol.Token.CONST_CHAR,"\'\\t\'",
								new Position(srcName,line,col-2,srcName,line,col+1)));
						}else if((char)tmp=='n' && (char)srcFile.read()=='\''){
							return log(new Symbol(Symbol.Token.CONST_CHAR,"\'\\n\'",
								new Position(srcName,line,col-2,srcName,line,col+1)));
						}else throw new CompilerError("Invalid escape sequence.");
					}else if((char)tmp=='\'' || (char)tmp=='\"'){
						throw new CompilerError("Invalid character name: "+tmp+"("+(char)tmp+").");
					}else{
						if((char)srcFile.read()!='\'')
							throw new CompilerError("Character constaints consis of only"+
								" one character name in single quotes.");
						col++;
						return log(new Symbol(Symbol.Token.CONST_CHAR,"\'"+(char)tmp+"\'",
							new Position(srcName,line,col-2,srcName,line,col)));
					}
				}else throw new CompilerError("Invalid character name: "+tmp+"("+(char)tmp+").");
		}

		if(Character.isDigit(c)){
			long val = 0;
			int i = 0;
			do{
				val = 10*val + Character.digit(c,10);
				c = (char)srcFile.read();
				i++;
			}while(Character.isDigit(c));
			tempc = c;
			col+=i;
			return log(new Symbol(Symbol.Token.CONST_INTEGER,""+val,new Position(srcName,line,col-i,srcName,line,col)));
		}else if(((int)c)==10){
			return lexAn();
		}else{
			return log(new Symbol(Symbol.Token.BOOLEAN,new Position(srcName,line,col)));
		}
		//throw new CompilerError("symbol/identifier/number not implemented yet: "+(int)c+":"+line+","+col);
	}

	/**
	 * Prints out the symbol and returns it.
	 * 
	 * This method should be called by the lexical analyzer before it returns a
	 * symbol so that the symbol can be logged (even if logging of lexical
	 * analysis has not been requested).
	 * 
	 * @param symbol
	 *            The symbol to be printed out.
	 * @return The symbol received as an argument.
	 */
	private Symbol log(Symbol symbol) {
		symbol.log(logger);
		return symbol;
	}

}
