package cop5556sp17;

import java.util.ArrayList;
import java.util.Arrays;

public class Scanner {
	/**
	 * Kind enum
	 */
	
	public static enum Kind {
		IDENT(""), INT_LIT(""), KW_INTEGER("integer"), KW_BOOLEAN("boolean"), 
		KW_IMAGE("image"), KW_URL("url"), KW_FILE("file"), KW_FRAME("frame"), 
		KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"), 
		SEMI(";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"), 
		RBRACE("}"), ARROW("->"), BARARROW("|->"), OR("|"), AND("&"), 
		EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), LE("<="), GE(">="), 
		PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"), 
		ASSIGN("<-"), OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE("convolve"), 
		KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH("screenwidth"), 
		OP_WIDTH("width"), OP_HEIGHT("height"), KW_XLOC("xloc"), KW_YLOC("yloc"), 
		KW_HIDE("hide"), KW_SHOW("show"), KW_MOVE("move"), OP_SLEEP("sleep"), 
		KW_SCALE("scale"), EOF("eof");

		Kind(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}

	
	/**
	 * @author geetanjli
	 * Set of states that has been used for designing scanner
	 */
	public static enum State{
		START, AFTER_EQ, IN_IDENT, IN_DIGIT, AFTER_OR, AFTER_NOT, AFTER_MINUS, AFTER_LESS, AFTER_GREATER, AFTER_BACKSLASH;	
	}
	
	
	
/**
 * Thrown by Scanner when an illegal character is encountered
 */
	@SuppressWarnings("serial")
	public static class IllegalCharException extends Exception {
		public IllegalCharException(String message) {
			super(message);
		}
	}
	
	/**
	 * Thrown by Scanner when an int literal is not a value that can be represented by an int.
	 */
	@SuppressWarnings("serial")
	public static class IllegalNumberException extends Exception {
	public IllegalNumberException(String message){
		super(message);
		}
	}
	

	/**
	 * Holds the line and position in the line of a token.
	 */
	static class LinePos {
		public final int line;
		public final int posInLine;
		
		public LinePos(int line, int posInLine) {
			super();
			this.line = line;
			this.posInLine = posInLine;
		}

		@Override
		public String toString() {
			return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
		}
	}
		

	

	/**
	 * @author geetanjli
	 * Token class will store relevant information about the token
	 */
	public class Token {
		public final Kind kind;
		public final int pos;  //position in input array
		public final int length;  //length of token

		//returns the text of this Token
		public String getText() {
			String text=kind.getText();
			if(text.equals("")) text=chars.substring(pos, pos+length);
			return text;
		}
		
		//returns a LinePos object representing the line and column of this Token
		LinePos getLinePos(){
			int [] linePosArray = new int[lineNumbers.size()];
			for(int i=0; i<lineNumbers.size();i++)
				linePosArray[i] = lineNumbers.get(i);
			int line;
			//linePosArray will keep the starting position in a new line, 
			//binary search searches for the position of that token among the line positions and give the appropriate near value
			line=Arrays.binarySearch(linePosArray,pos);
			//Incase that element is not present in array, binary search algorithm of java designates -(insertionPoint)-1 value, hence to handle such cases :- line = -1*line-2
			if(line<0) line=line*-1-2;
			int lineP = lineNumbers.get(line);
			LinePos lp = new LinePos(line,pos-lineP);
			return lp;
		}

		@Override
		public String toString() {
			return "Token [kind=" + kind + ", pos=" + pos + ", length=" + length + " get text="+this.getText()+" "+this.getLinePos()+"]";
		}

		Token(Kind kind, int pos, int length) {
			this.kind = kind;
			this.pos = pos;
			this.length = length;
			//System.out.println("Adding token "+this.toString());
		}

		/** 
		 * Precondition:  kind = Kind.INT_LIT,  the text can be represented with a Java int.
		 * Note that the validity of the input should have been checked when the Token was created.
		 * So the exception should never be thrown.
		 * 
		 * @return  int value of this token, which should represent an INT_LIT
		 * @throws NumberFormatException
		 * @throws IllegalNumberException 
		 */
		public int intVal() throws NumberFormatException{
			int val=0;
			//try{
			val = Integer.parseInt(chars.substring(pos,pos+length));
			//}catch(NumberFormatException i)
			//{
				//throw new IllegalNumberException("Num Literal exceeds maximum int value");
			//	i.printStackTrace();
			//}
			return val;
		}

//		public boolean isKind(Kind eof) {
//			// TODO Auto-generated method stub
//			if(this.kind==eof) return true;
//			else return false;
//		}
		
		
		@Override
		  public int hashCode() {
		   final int prime = 31;
		   int result = 1;
		   result = prime * result + getOuterType().hashCode();
		   result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		   result = prime * result + length;
		   result = prime * result + pos;
		   return result;
		  }

		  @Override
		  public boolean equals(Object obj) {
		   if (this == obj) {
		    return true;
		   }
		   if (obj == null) {
		    return false;
		   }
		   if (!(obj instanceof Token)) {
		    return false;
		   }
		   Token other = (Token) obj;
		   if (!getOuterType().equals(other.getOuterType())) {
		    return false;
		   }
		   if (kind != other.kind) {
		    return false;
		   }
		   if (length != other.length) {
		    return false;
		   }
		   if (pos != other.pos) {
		    return false;
		   }
		   return true;
		  }

		 

		  private Scanner getOuterType() {
		   return Scanner.this;
		  }
	}

	 


	Scanner(String chars) {
		this.chars = chars;
		tokens = new ArrayList<Token>();
		lineNumbers=new ArrayList<Integer>();
		lineNumbers.add(0);
	}


	
	

	/**
	 * Initializes Scanner object by traversing chars and adding tokens to tokens list.
	 * 
	 * @return this scanner
	 * @throws IllegalCharException
	 * @throws IllegalNumberException
	 */
	public Scanner scan() throws IllegalCharException {
		int pos = 0; 
		int length = chars.length();
	    State state = State.START;
	    int startPos = 0;
	    int ch;
	    while (pos <= length) {
	        ch = pos < length ? chars.charAt(pos) : -1;
	        switch (state) {
	        case START: {
	            pos = skipWhiteSpace(pos);
	            ch = pos < length ? chars.charAt(pos) : -1;
	            //In case of empty string We just need to return EOF
	            if(length == 0) ch=-1;
	            startPos = pos;
	            switch (ch) {
	                case -1: {tokens.add(new Token(Kind.EOF, pos, 0)); pos++;}  break;
	                case '+': {tokens.add(new Token(Kind.PLUS, startPos, 1));pos++;} break;
	                case '*': {tokens.add(new Token(Kind.TIMES, startPos, 1));pos++;} break;
	                case '=': {state = State.AFTER_EQ;pos++;}break;
	                case '0': {tokens.add(new Token(Kind.INT_LIT,startPos, 1));pos++;}break;
	                case '|': {state = State.AFTER_OR;pos++;}break;
	                case '&': {tokens.add(new Token(Kind.AND,startPos,1));pos++;}break;
	                case '!': {state = State.AFTER_NOT;pos++;}break;
	                case '-': {state = State.AFTER_MINUS;pos++;}break;
	                case '<': {state = State.AFTER_LESS;pos++;}break;
	                case '>': {state = State.AFTER_GREATER;pos++;}break;
	                case '/': {state = State.AFTER_BACKSLASH;pos++;}break;
	                case '%': {tokens.add(new Token(Kind.MOD,startPos,1));pos++;}break;
	                case ';': {tokens.add(new Token(Kind.SEMI,startPos,1));pos++;}break;
	                case ',': {tokens.add(new Token(Kind.COMMA,startPos,1));pos++;}break;
	                case '(': {tokens.add(new Token(Kind.LPAREN,startPos,1));pos++;}break;
	                case ')': {tokens.add(new Token(Kind.RPAREN,startPos,1));pos++;}break;
	                case '{': {tokens.add(new Token(Kind.LBRACE,startPos,1));pos++;}break;
	                case '}': {tokens.add(new Token(Kind.RBRACE,startPos,1));pos++;}break;
	                default: {
	                    if (Character.isDigit(ch)) {state = State.IN_DIGIT;pos++;} 
	                    else if (Character.isJavaIdentifierStart(ch)) {
	                         state = State.IN_IDENT;pos++;
	                     } 
	                     else {
	                    	 throw new IllegalCharException("illegal char " +chars.charAt(pos)+" with ascii value "+ch +" at pos "+pos);
	                     }
	                  }
	            } // switch (ch)
	        } break;      // case START

            case IN_IDENT: {
	            	StringBuilder sb = new StringBuilder();
	            	 char cha = chars.charAt(startPos);
	            	 sb.append(cha);
		            	while(pos<length && Character.isJavaIdentifierPart(cha=chars.charAt(pos))){
		            			sb.append(cha);
		                      pos++;
		            	}
                		String key = sb.toString();
                		if(key.equals("integer")) tokens.add(new Token(Kind.KW_INTEGER,startPos,pos-startPos));
                		else if (key.equals("boolean")) tokens.add(new Token(Kind.KW_BOOLEAN,startPos,pos-startPos));
                		else if (key.equals("image")) tokens.add(new Token(Kind.KW_IMAGE,startPos,pos-startPos));
                		else if (key.equals("url")) tokens.add(new Token(Kind.KW_URL,startPos,pos-startPos));
                		else if (key.equals("file")) tokens.add(new Token(Kind.KW_FILE,startPos,pos-startPos));
                		else if (key.equals("frame")) tokens.add(new Token(Kind.KW_FRAME,startPos,pos-startPos));
                		else if (key.equals("while")) tokens.add(new Token(Kind.KW_WHILE,startPos,pos-startPos));
                		else if (key.equals("if")) tokens.add(new Token(Kind.KW_IF,startPos,pos-startPos));
                		else if (key.equals("sleep")) tokens.add(new Token(Kind.OP_SLEEP,startPos,pos-startPos));
                		else if (key.equals("screenheight")) tokens.add(new Token(Kind.KW_SCREENHEIGHT,startPos,pos-startPos));
                		else if (key.equals("screenwidth")) tokens.add(new Token(Kind.KW_SCREENWIDTH,startPos,pos-startPos));
                		else if (key.equals("gray")) tokens.add(new Token(Kind.OP_GRAY,startPos,pos-startPos));
                		else if (key.equals("convolve")) tokens.add(new Token(Kind.OP_CONVOLVE,startPos,pos-startPos));
                		else if (key.equals("blur")) tokens.add(new Token(Kind.OP_BLUR,startPos,pos-startPos));
                		else if (key.equals("scale")) tokens.add(new Token(Kind.KW_SCALE,startPos,pos-startPos));
                		else if (key.equals("width")) tokens.add(new Token(Kind.OP_WIDTH,startPos,pos-startPos));
                		else if (key.equals("height")) tokens.add(new Token(Kind.OP_HEIGHT,startPos,pos-startPos));
                		else if (key.equals("xloc")) tokens.add(new Token(Kind.KW_XLOC,startPos,pos-startPos));
                		else if (key.equals("yloc")) tokens.add(new Token(Kind.KW_YLOC,startPos,pos-startPos));
                		else if (key.equals("hide")) tokens.add(new Token(Kind.KW_HIDE,startPos,pos-startPos));
                		else if (key.equals("show")) tokens.add(new Token(Kind.KW_SHOW,startPos,pos-startPos));
                		else if (key.equals("move")) tokens.add(new Token(Kind.KW_MOVE,startPos,pos-startPos));
                		else if (key.equals("true")) tokens.add(new Token(Kind.KW_TRUE,startPos,pos-startPos));
                		else if (key.equals("false")) tokens.add(new Token(Kind.KW_FALSE,startPos,pos-startPos));
                		else tokens.add(new Token(Kind.IDENT, startPos, pos - startPos));
                        state = State.START;
            }break;

	            case AFTER_EQ: {
	            	if(ch == '='){
	            		tokens.add(new Token(Kind.EQUAL,startPos,2));
	            		pos++;
	            		state = State.START;
	            	}else{
	            		throw new IllegalCharException("= is not proceeded by another =");
	            	}
	            } break;
	            
	            case AFTER_OR:{
	            	if ((pos+1 < length) &&(ch == '-') && (chars.charAt(pos+1) == '>')){
	            		pos++;
	            		tokens.add(new Token(Kind.BARARROW,startPos,3));
	            		pos++;
	            		state = State.START;
	            	}else{
	            		tokens.add(new Token(Kind.OR,startPos,1));
	            		state = State.START;
	            	}
	            } break;
	            
	            case IN_DIGIT:{
	            	if(Character.isDigit(ch)) pos++;
	            	else {
	            		Token num_lit=new Token(Kind.INT_LIT, startPos,pos-startPos);
	            		num_lit.intVal();
	            		tokens.add(num_lit);
	            		state=State.START;
	            	}
	            	break;
	            }
	            
	            case AFTER_NOT:{
	            	if(ch == '='){
	            		tokens.add(new Token(Kind.NOTEQUAL,startPos,2));
	            		state=State.START;
	            		pos++;
	            	}else{
	            		tokens.add(new Token(Kind.NOT,startPos,1));
	            		state=State.START;
	            	}
	            	break;
	            }
	            
	            case AFTER_MINUS:{
	            	if(ch == '>'){
	            		tokens.add(new Token(Kind.ARROW,startPos,2));
	            		state=State.START;
	            		pos++;
	            	}else{
	            		tokens.add(new Token(Kind.MINUS,startPos,1));
	            		state=State.START;
	            	}
	            	break;
	            }
	            
	            case AFTER_LESS:{
	            	if(ch == '='){
	            		tokens.add(new Token(Kind.LE,startPos,2));
	            		state=State.START;
	            		pos++;
	            	}else if(ch == '-'){
	            		tokens.add(new Token(Kind.ASSIGN,startPos,2));
	            		state=State.START;
	            		pos++;
	            	}else {
	            		tokens.add(new Token(Kind.LT,startPos,1));
	            		state=State.START;
	            	}
	            	break;
	            }
	            
	            case AFTER_GREATER:{
	            	if(ch == '='){
	            		tokens.add(new Token(Kind.GE,startPos,2));
	            		state=State.START;
	            		pos++;
	            	}else {
	            		tokens.add(new Token(Kind.GT,startPos,1));
	            		state=State.START;
	            	}
	            	break;
	            }
	            
	            case AFTER_BACKSLASH:{
	            	if(ch == '*'){
	            		pos++;
	            		pos = this.skipWhiteSpace(pos);
	            		while((pos+1 < length) && !((chars.charAt(pos) == '*') && (chars.charAt(pos+1) == '/'))){
	            			pos++;
	            			pos = this.skipWhiteSpace(pos);
	            		}
	            		state=State.START;
	            		if(pos<length)pos++;
	            		if(pos >= length) break;
	            		//case where I could not apply check for proceeding condition and characters should be ignored eg:- /*a
	            			//if(!(chars.charAt(pos) == '*')&&(pos+1 == length)) pos++;
	            		//incrementing pos value by 2 as current position is * and next position will be after */ i.e ending braces
	            		pos++;
	            	}else{
	            		tokens.add(new Token(Kind.DIV,startPos,1));
	            		state=State.START;
	            	}
	            	break;
	            }
	            
	            default:  assert false;
	        }// switch(state)
	    } 
		
		//!!!
//		tokens.add(new Token(Kind.EOF,pos,0));
		return this;  
	}

	
	
	private int skipWhiteSpace(int pos) {
		int ch =  (pos < chars.length() ? chars.charAt(pos) : -1);
		while(Character.isWhitespace(ch)) {
			if(ch =='\n'){
			//line++;
			lineNumbers.add(pos+1);
			}
			pos++;
			ch = (pos < chars.length() ? chars.charAt(pos) : -1);
		}
		return pos;
	}



	final ArrayList<Token> tokens;
	final String chars;
	int tokenNum;
	final ArrayList<Integer> lineNumbers;
	int line=0;

	/*
	 * Return the next token in the token list and update the state so that
	 * the next call will return the Token..  
	 */
	public Token nextToken() {
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum++);
	}
	
	/*
	 * Return the next token in the token list without updating the state.
	 * (So the following call to next will return the same token.)
	 */
	public Token peek(){
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum);		
	}

	

	/**
	 * Returns a LinePos object containing the line and position in line of the 
	 * given token.  
	 * 
	 * Line numbers start counting at 0
	 * 
	 * @param t
	 * @return
	 */
	public LinePos getLinePos(Token t) {
		return t.getLinePos();
	}


}
