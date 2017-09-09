package cop5556sp17;

import cop5556sp17.Scanner.Kind;
import static cop5556sp17.Scanner.Kind.*;

import java.util.ArrayList;
import java.util.List;

import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTNode;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.WhileStatement;

public class Parser {

	/**
	 * Exception to be thrown if a syntax error is detected in the input. You
	 * will want to provide a useful error message.
	 *
	 */
	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		public SyntaxException(String message) {
			super(message);
		}
	}

	/**
	 * Useful during development to ensure unimplemented routines are not
	 * accidentally called during development. Delete it when the Parser is
	 * finished.
	 *
	 */
	@SuppressWarnings("serial")
	public static class UnimplementedFeatureException extends RuntimeException {
		public UnimplementedFeatureException() {
			super();
		}
	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	/**
	 * parse the input using tokens from the scanner. Check for EOF (i.e. no
	 * trailing junk) when finished
	 * 
	 * @throws SyntaxException
	 */
	Program parse() throws SyntaxException {
		Program prog = program();
		matchEOF();
		return prog;
	}

	Expression expression() throws SyntaxException {
		//public Expr expr()  //expr ::= term (( + | - ) term  )* 
		//{    
			Expression exp1 = null;
	        Expression exp2 = null;
	        Token firstToken = t;

		try {
			ArrayList<Kind> expression = new ArrayList<Kind>();
			expression.add(LT);
			expression.add(LE);
			expression.add(GT);
			expression.add(GE);
			expression.add(EQUAL);
			expression.add(NOTEQUAL);
			exp1 = term();
			while (expression.contains(t.kind)) {
				Token op = t;
				consume();
				exp2 =term();
				exp1 = new BinaryExpression(firstToken, exp1, op, exp2);
			}
		} catch (Exception e) {
			throw new SyntaxException("Exception thrown in expression"+" found "+t.kind);
		}
		return exp1;
	}

	Expression term() throws SyntaxException {
		Expression t1 = null;
		Expression t2 = null;
		Token firstToken = t;
		try {
			ArrayList<Kind> term = new ArrayList<Kind>();
			term.add(PLUS);
			term.add(MINUS);
			term.add(OR);
			t1 = elem();
			while (term.contains(t.kind)) {
				Token op = t;
				consume();
				t2 = elem();
				t1 = new BinaryExpression(firstToken,t1,op,t2);
			}
			return t1;
		} catch (Exception e) {
			throw new SyntaxException("Exception thrown in term found "+t.kind);
		}
	}

	Expression elem() throws SyntaxException {
		Expression e1 = null;
		Expression e2 = null;
		Token firstToken = t;

		try {
			ArrayList<Kind> elem = new ArrayList<Kind>();
			elem.add(TIMES);
			elem.add(DIV);
			elem.add(AND);
			elem.add(MOD);
			e1 = factor();
			while (elem.contains(t.kind)) {
				Token op = t;
				consume();
				e2 = factor();
				e1 = new BinaryExpression(firstToken, e1, op, e2);
			}
		} catch (Exception e) {
			throw new SyntaxException("Exception thrown in elem found "+t.kind);
		}
		return e1;
	}

	Expression factor() throws SyntaxException {
		Kind kind = t.kind;
		Expression exp = null;
		Token firstToken = t;
		switch (kind) {
		case IDENT: {
			exp = new IdentExpression(firstToken);
			consume();
		}
			break;
		case INT_LIT: {
			exp = new IntLitExpression(firstToken);
			consume();
		}
			break;
		case KW_TRUE:
		case KW_FALSE: {
			exp = new BooleanLitExpression(firstToken);
			consume();
		}
			break;
		case KW_SCREENWIDTH:
		case KW_SCREENHEIGHT: {
			exp = new ConstantExpression(firstToken);
			consume();
		}
			break;
		case LPAREN: {
			consume();
			exp = expression();
			match(RPAREN);
		}
			break;
		default:
			// you will want to provide a more useful error message
			throw new SyntaxException("illegal factor");
		}
		return exp;
	}

	Block block() throws SyntaxException {
		Token firstToken = t;
		Block bl = null;
		ArrayList<Dec> listDec = new ArrayList<Dec>();
		ArrayList<Statement> listStmt = new ArrayList<Statement>();
		ArrayList<Kind> dec = new ArrayList<Kind>();
		ArrayList<Kind> statement = new ArrayList<Kind>();
		try {
			dec.add(KW_INTEGER);
			dec.add(KW_IMAGE);
			dec.add(KW_BOOLEAN);
			dec.add(KW_FRAME);
			statement.add(OP_SLEEP);
			statement.add(KW_WHILE);
			statement.add(KW_IF);
			statement.add(KW_SCALE);
			statement.add(IDENT);
			statement.add(OP_BLUR);
			statement.add(OP_GRAY);
			statement.add(OP_CONVOLVE);
			statement.add(KW_SHOW);
			statement.add(KW_HIDE);
			statement.add(KW_MOVE);
			statement.add(KW_XLOC);
			statement.add(KW_YLOC);
			statement.add(OP_WIDTH);
			statement.add(OP_HEIGHT);
			match(LBRACE);
			while (dec.contains(t.kind) || statement.contains(t.kind)) {
				if (dec.contains(t.kind)) {
					listDec.add(dec());
				} else if (statement.contains(t.kind)) {
					listStmt.add(statement());
				} else {
					throw new SyntaxException("Exception thrown in block found "+t.kind);
				}
			}
			bl = new Block(firstToken,listDec,listStmt);
			match(RBRACE);
		} catch (Exception e) {
			throw new SyntaxException("Exception thrown in block found "+t.kind);
		}
		return bl;
	}

	Program program() throws SyntaxException {
			ArrayList<Kind> param = new ArrayList<Kind>();
			Program p = null;
			Block bl = null;
			ArrayList<ParamDec> apd = new ArrayList<ParamDec>();
			Token firstToken = t;
			param.add(KW_URL);
			param.add(KW_INTEGER);
			param.add(KW_FILE);
			param.add(KW_BOOLEAN);
			if(t.kind.equals(IDENT))
			{
				consume();
				if(t.kind.equals(LBRACE)){
					bl=block();
				}else if(param.contains(t.kind)){
					apd.add(paramDec());
					while(t.kind.equals(COMMA)){
						consume();
						apd.add(paramDec());
					}
					bl=block();
				}
				else{
					throw new SyntaxException("Exception thrown in program "+t.kind);
				}
				p=new Program(firstToken,apd,bl);
			}
			else{
				throw new SyntaxException("Exception thrown in program "+t.kind);
			}
			return p;
	}

	ParamDec paramDec() throws SyntaxException {
		ParamDec pdcl = null;
		Token firstToken = t;
		ArrayList<Kind> param = new ArrayList<Kind>();
		param.add(KW_URL);
		param.add(KW_INTEGER);
		param.add(KW_FILE);
		param.add(KW_BOOLEAN);
		if(param.contains(t.kind)){
			consume();
			Token temp = t;
			match(IDENT);
			pdcl=new ParamDec(firstToken,temp);
		}else
			throw new SyntaxException("Exception thrown in paramDec found "+t.kind);
		return pdcl;
	}

	Dec dec() throws SyntaxException {
		// TODO
		ArrayList<Kind> dec = new ArrayList<Kind>();
		Dec dc = null;
		Token firstToken = t;
		dec.add(KW_INTEGER);
		dec.add(KW_IMAGE);
		dec.add(KW_BOOLEAN);
		dec.add(KW_FRAME);
		if (dec.contains(t.kind)) {
			consume();
			Token temp = t;
			match(IDENT);
			dc=new Dec(firstToken,temp);
		} else {
			throw new SyntaxException("Exception thrown in dec "+t.kind);
		}
		return dc;
	}

	Statement statement() throws SyntaxException {
		Statement stmt = null;
		Expression exp =null;
		Block bl = null;
		IdentLValue idv =null;
		Token firstToken = t;
		ArrayList<Kind> statement = new ArrayList<Kind>();
		statement.add(OP_SLEEP);
		statement.add(KW_WHILE);
		statement.add(KW_IF);
		statement.add(KW_SCALE);
		statement.add(IDENT);
		statement.add(OP_BLUR);
		statement.add(OP_GRAY);
		statement.add(OP_CONVOLVE);
		statement.add(KW_SHOW);
		statement.add(KW_HIDE);
		statement.add(KW_MOVE);
		statement.add(KW_XLOC);
		statement.add(KW_YLOC);
		statement.add(OP_WIDTH);
		statement.add(OP_HEIGHT);
		// TODO
		if (statement.contains(t.kind)) {
			switch (t.kind) {

			case KW_WHILE:
				consume();
				match(LPAREN);
				exp =expression();
				match(RPAREN);
				bl = block();
				stmt = new WhileStatement(firstToken,exp,bl);
				break;
			case OP_SLEEP:
				consume();
				exp = expression();
				stmt = new SleepStatement(firstToken,exp);
				match(SEMI);
				break;
			case IDENT:
				if (scanner.peek().kind.equals(ASSIGN)) {
					idv = new IdentLValue(firstToken);
					consume();
					consume();
					exp = expression();
					match(SEMI);
					stmt= new AssignmentStatement(firstToken,idv,exp);
				} else if (scanner.peek().kind.equals(ARROW) || scanner.peek().kind.equals(BARARROW)) {
					stmt = chain();
					match(SEMI);
				} else {
					throw new SyntaxException("Illegal statement " + t.getLinePos().toString());
				}
				break;
			case KW_IF:
				consume();
				match(LPAREN);
				exp =expression();
				match(RPAREN);
				bl =block();
				stmt = new IfStatement(firstToken,exp,bl);
				break;
			default: {
				stmt = chain();
				match(SEMI);
			}

			}
		} else {
			throw new SyntaxException("Illegal Parameter statement" + t.getLinePos().toString());
		}
		return stmt;
	}

	Chain chain() throws SyntaxException {
		// TODO
			Chain ch = null;
			ChainElem ce1 = null;
			ChainElem ce2 = null;
			Token firstToken = t;
				ArrayList<Kind> arrOp = new ArrayList<Kind>();
				arrOp.add(ARROW);
				arrOp.add(BARARROW);
				ArrayList<Kind> chainEle = new ArrayList<Kind>();
				chainEle.add(IDENT);chainEle.add(OP_BLUR);chainEle.add(OP_GRAY); chainEle.add(OP_CONVOLVE);
				chainEle.add(KW_SHOW);chainEle.add(KW_HIDE);chainEle.add(KW_MOVE);chainEle.add(KW_XLOC);
				chainEle.add(KW_YLOC);chainEle.add(OP_WIDTH);chainEle.add(OP_HEIGHT);chainEle.add(KW_SCALE);
				if (chainEle.contains(t.kind)) {
					ce1=chainElem();
					Token tk = t;
					if(arrOp.contains(t.kind)){
						consume();
					}
					ce2 = chainElem();
					ch = new BinaryChain(firstToken,ce1,tk,ce2);
					while(arrOp.contains(t.kind)){
						Token temp = t;
						if(arrOp.contains(t.kind)){
							consume();
						}
						ce1 = chainElem();
						ch = new BinaryChain(firstToken,ch,temp,ce1);
					}
				} else {
					throw new SyntaxException("Exception thrown in chain "+t.kind);
				}
				return ch;
	}

	

	ChainElem chainElem() throws SyntaxException {
		ArrayList<Kind> chainEle = new ArrayList<Kind>();
		ChainElem cem = null;
		Tuple tpl = null;
		Token firstToken = t;
		List<Kind> l1 = new ArrayList<Kind>();
		l1.add(OP_BLUR);l1.add(OP_GRAY); l1.add(OP_CONVOLVE);
		
		List<Kind> l2 = new ArrayList<Kind>();
		l2.add(KW_SHOW);l2.add(KW_HIDE);l2.add(KW_MOVE);l2.add(KW_XLOC);
		l2.add(KW_YLOC);
		
		List<Kind> l3 = new ArrayList<Kind>();
		l3.add(OP_WIDTH);l3.add(OP_HEIGHT);l3.add(KW_SCALE);
		
//		chainEle.add(IDENT);chainEle.add(OP_BLUR);chainEle.add(OP_GRAY); chainEle.add(OP_CONVOLVE);
//		chainEle.add(KW_SHOW);chainEle.add(KW_HIDE);chainEle.add(KW_MOVE);chainEle.add(KW_XLOC);
//		chainEle.add(KW_YLOC);chainEle.add(OP_WIDTH);chainEle.add(OP_HEIGHT);chainEle.add(KW_SCALE);
//			if(chainEle.contains(t.kind))
		//	{
				if(t.kind.equals(IDENT))
				{
					cem = new IdentChain(firstToken);
				consume();
				}
				else if(l3.contains(t.kind))
				{
					consume();
					tpl = arg();
					cem = new ImageOpChain(firstToken,tpl);
				}
				else if(l1.contains(t.kind))
				{
					consume();
					tpl = arg();
					cem = new FilterOpChain(firstToken,tpl);
				}
				else if(l2.contains(t.kind))
				{
					consume();
					tpl = arg();
					cem = new FrameOpChain(firstToken,tpl);
				}
				
			else{
				throw new SyntaxException("Exception thrown in chainElem found "+t.kind);
			}
		return cem;
	}

	Tuple arg() throws SyntaxException {
		//TODO
		Tuple tup = null;
		List<Expression> le = new ArrayList<Expression>();
		Token firstToken = t;
		ArrayList<Kind> argum = new ArrayList<Kind>();
		argum.add(ARROW);
		argum.add(BARARROW);
		argum.add(SEMI);
		argum.add(EOF);
		
				if(t.kind.equals(LPAREN)){
					consume();
						le.add(expression());  
					while(t.kind.equals(COMMA)){
						consume();
						le.add(expression());
					}
					match(RPAREN);
				}
				tup = new Tuple(firstToken,le);
				return tup;
	}

	/**
	 * Checks whether the current token is the EOF token. If not, a
	 * SyntaxException is thrown.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.kind.equals(EOF)) {
			return t;
		}
		throw new SyntaxException("expected EOF");
	}

	/**
	 * Checks if the current token has the given kind. If so, the current token
	 * is consumed and returned. If not, a SyntaxException is thrown.
	 * 
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		if (t.kind.equals(kind)) {
			return consume();
		}
		throw new SyntaxException("saw " + t.kind + "expected " + kind);
	}

	/**
	 * Checks if the current token has one of the given kinds. If so, the
	 * current token is consumed and returned. If not, a SyntaxException is
	 * thrown.
	 * 
	 * * Precondition: for all given kinds, kind != EOF
	 * 
	 * @param kinds
	 *            list of kinds, matches any one
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind... kinds) throws SyntaxException {
		// TODO. Optional but handy
		return null; // replace this statement
	}

	/**
	 * Gets the next token and returns the consumed token.
	 * 
	 * Precondition: t.kind != EOF
	 * 
	 * @return
	 * 
	 */
	private Token consume() throws SyntaxException {
		Token tmp = t;
		t = scanner.nextToken();
		return tmp;
	}

}
