package cop5556sp17;

import cop5556sp17.AST.ASTNode;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.Tuple;
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
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import java.util.ArrayList;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.LinePos;
import cop5556sp17.Scanner.Token;
import static cop5556sp17.AST.Type.TypeName.*;
import static cop5556sp17.Scanner.Kind.ARROW;
import static cop5556sp17.Scanner.Kind.KW_HIDE;
import static cop5556sp17.Scanner.Kind.KW_MOVE;
import static cop5556sp17.Scanner.Kind.KW_SHOW;
import static cop5556sp17.Scanner.Kind.KW_XLOC;
import static cop5556sp17.Scanner.Kind.KW_YLOC;
import static cop5556sp17.Scanner.Kind.OP_BLUR;
import static cop5556sp17.Scanner.Kind.OP_CONVOLVE;
import static cop5556sp17.Scanner.Kind.OP_GRAY;
import static cop5556sp17.Scanner.Kind.OP_HEIGHT;
import static cop5556sp17.Scanner.Kind.OP_WIDTH;
import static cop5556sp17.Scanner.Kind.*;

public class TypeCheckVisitor implements ASTVisitor {

	@SuppressWarnings("serial")
	public static class TypeCheckException extends Exception {
		public TypeCheckException(String message) {
			super(message);
		}
	}

	SymbolTable symtab = new SymbolTable();

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {

		binaryChain.getE0().visit(this, arg);
		binaryChain.getE1().visit(this, arg);
		TypeName chainT = binaryChain.getE0().getTn();
		ChainElem chainE = binaryChain.getE1();
		Kind kind = binaryChain.getArrow().kind;
		switch (kind) {
		case ARROW: {
			switch (chainT) {
			case URL:
			case FILE:
				if (chainE.getTn().isType(IMAGE))
					binaryChain.setTn(IMAGE);
				else
					throw new TypeCheckException("Exception occured : chain of FILE/URL but not IMAGE");
				break;
			case FRAME:
				if ((chainE instanceof FrameOpChain)
						&& (chainE.getFirstToken().kind.equals(KW_YLOC) || chainE.getFirstToken().kind.equals(KW_XLOC)))
					binaryChain.setTn(INTEGER);
				else if ((chainE instanceof FrameOpChain) && (chainE.getFirstToken().kind.equals(KW_MOVE)
						|| chainE.getFirstToken().kind.equals(KW_HIDE) || chainE.getFirstToken().kind.equals(KW_SHOW)))
					binaryChain.setTn(FRAME);
				else
					throw new TypeCheckException("Exception occured in FRAME binary chain");
				break;
			case IMAGE:
				if ((chainE instanceof ImageOpChain)
						&& (chainE.getFirstToken().kind.equals(OP_HEIGHT) || chainE.getFirstToken().kind.equals(OP_WIDTH)))
					binaryChain.setTn(INTEGER);
				else if (chainE.getTn().isType(FRAME))
					binaryChain.setTn(FRAME);
				else if (chainE.getTn().isType(FILE))
					binaryChain.setTn(NONE);
				else if ((chainE instanceof FilterOpChain) && (chainE.getFirstToken().kind.equals(OP_CONVOLVE)
						|| chainE.getFirstToken().kind.equals(OP_BLUR) || chainE.getFirstToken().kind.equals(OP_GRAY)))
					binaryChain.setTn(IMAGE);
				else if(chainE instanceof IdentChain && chainE.getTn().isType(IMAGE))
					binaryChain.setTn(IMAGE);
				else if ((chainE instanceof ImageOpChain) && (chainE.getFirstToken().kind.equals(KW_SCALE)))
					binaryChain.setTn(IMAGE);
				else if (chainE instanceof IdentChain)
					binaryChain.setTn(IMAGE);
				else
					throw new TypeCheckException("Exception occured in IMAGE binary chain");
			break;
			case INTEGER:
				if(chainE instanceof IdentChain && chainE.getTn().isType(INTEGER))
					binaryChain.setTn(INTEGER);
				break;
			default:
				throw new TypeCheckException("Exception occured default binary expression");
			}
		}
			break;
		case BARARROW:
			if (chainT.isType(IMAGE) && (chainE instanceof FilterOpChain) && (chainE.getFirstToken().kind.equals(OP_GRAY)
					|| chainE.getFirstToken().kind.equals(OP_BLUR) || chainE.getFirstToken().kind.equals(OP_CONVOLVE)))
				binaryChain.setTn(IMAGE);
			else
				throw new TypeCheckException("Exception in BARROW");
			break;
		default:
			throw new TypeCheckException("op illegal: " + kind.text);
		}
		return binaryChain;

	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {

		binaryExpression.getE0().visit(this, arg);
		binaryExpression.getE1().visit(this, arg);
		TypeName exp0 = binaryExpression.getE0().getTn();
		TypeName exp1 = binaryExpression.getE1().getTn();
		Kind op = binaryExpression.getOp().kind;
		switch (exp0) {
		case INTEGER:
			switch (exp1) {
			case INTEGER:
				switch (op) {
				case PLUS:
				case MINUS:
				case TIMES:
				case DIV:
				case MOD:
					binaryExpression.setTn(INTEGER);
					break;
				case LT:
				case GT:
				case LE:
				case AND:
				case OR:
				case GE:
				case EQUAL:
				case NOTEQUAL:
					binaryExpression.setTn(BOOLEAN);
					break;
				default:
					throw new TypeCheckException("Expression occured as E0, E1 type is not INTEGER");
				}
				break;
			case IMAGE:
				switch (op) {
				case TIMES:
				case DIV:
				case MOD:
					binaryExpression.setTn(IMAGE);
					break;
				default:
					throw new TypeCheckException("Expression occured as E1 not of IMAGE type");
				}
				break;
			default:
				throw new TypeCheckException("Expression occured as E1 not of IMAGE/INTEGER type");
			}
			break;
		case IMAGE:
			switch (exp1) {
			case IMAGE:
				switch (op) {
				case PLUS:
				case MINUS:
					binaryExpression.setTn(IMAGE);
					break;
				case EQUAL:
					
				case NOTEQUAL:
					binaryExpression.setTn(BOOLEAN);
					break;
				default:
					throw new TypeCheckException("Expression occured as E0 and E1 not of IMAGE type");
				}
				break;
				
			case INTEGER:
				if (op.equals(TIMES)||op.equals(MOD)||op.equals(DIV))
					binaryExpression.setTn(IMAGE);
				else
					throw new TypeCheckException("Expression occured as E1 not of INTEGER type");
				break;

			default:
				throw new TypeCheckException("Expression occured as E1 not of IMAGE/INTEGER type");
			}
			break;
			
		case BOOLEAN:
			if (exp1.isType(BOOLEAN)) {
				switch (op) {
				case LT:
				case GT:
				case LE:
				case AND:
				case OR:
				case GE:
				case EQUAL:
				case NOTEQUAL:
					binaryExpression.setTn(BOOLEAN);
					break;
				default:
					throw new TypeCheckException("Expression occured as E0, E1 not of BOOLEAN type");
				}
			} else
				throw new TypeCheckException("Expression occured as E1 not of BOOLEAN type");
			break;
		}
		
		if ((op.equals(NOTEQUAL) || op.equals(EQUAL)) && binaryExpression.tn == null) {
			if (exp0.isType(exp1)) {
					binaryExpression.setTn(BOOLEAN);
					return binaryExpression;
			} else
					throw new TypeCheckException("Expression occured as : E1.type not same as E2.type");
		}
		else if (binaryExpression.tn != null)
			return binaryExpression;
		
		else
			throw new TypeCheckException("Expression occured as op is not EQUAL/NOTEQUAL");

	}
	
	

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		try {
			symtab.enterScope();
			for (Dec dec : block.getDecs()) {
				Dec d = (Dec) dec.visit(this, arg);
				if (d != null)
					continue;
			}
			for (Statement st : block.getStatements()) {
				// visit all statements
				if (null != (Statement) st.visit(this, arg))
					continue;
			}
			symtab.leaveScope();
		} catch (Exception e) {
			String s = e.getMessage();
			throw new TypeCheckException("Exception occured in visit block " + s);
		}
		return block;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		booleanLitExpression.setTn(BOOLEAN);
		return booleanLitExpression;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		// visit
		filterOpChain.getArg().visit(this, arg);
		// condition check
		if (filterOpChain.getArg().getExprList().size() == 0)
			filterOpChain.setTn(TypeName.IMAGE);
		else
			throw new TypeCheckException("Exception occured in FrameOpChain " + filterOpChain.toString());
		return filterOpChain;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {

		frameOpChain.getArg().visit(this, arg);
		if (frameOpChain.firstToken.kind.equals(KW_XLOC) || frameOpChain.firstToken.kind.equals(KW_YLOC)) {
			if (frameOpChain.getArg().getExprList().size() == 0)
				frameOpChain.setTn(TypeName.INTEGER);
			else
				throw new TypeCheckException("Exception occured in FrameOpChain visit: " + frameOpChain.toString());
		} else if (frameOpChain.firstToken.kind.equals(KW_HIDE) || frameOpChain.firstToken.kind.equals(KW_SHOW)) {
			if (frameOpChain.getArg().getExprList().size() == 0)
				frameOpChain.setTn(TypeName.NONE);
			else
				throw new TypeCheckException("Exception occured in FrameOpChain visit: " + frameOpChain.toString());
		} else if (frameOpChain.firstToken.kind.equals(KW_MOVE)) {
			if (frameOpChain.getArg().getExprList().size() == 2)
				frameOpChain.setTn(TypeName.NONE);
			else
				throw new TypeCheckException("Exception occured in FrameOpChain visit: " + frameOpChain.toString());
		} else
			throw new TypeCheckException("Exception occured in FrameOpChain visit: " + frameOpChain.toString());
		return frameOpChain;

	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		// ident has been declared and is visible in the current scope
		Dec dec = symtab.lookup(identChain.getFirstToken().getText());
		if (dec != null) {
			dec.setTn(null);
			identChain.setTn(dec.getTn());
			identChain.setD(dec);
		}
		return identChain;

	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		String ident = identExpression.firstToken.getText();
		Dec dec = symtab.lookup(ident);
		if (null != dec) {
			dec.setTn(null);
			identExpression.setTn(dec.getTn());
			identExpression.setDec(dec);
		}
		return identExpression;
	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		ifStatement.getE().visit(this, arg);
		ifStatement.getB().visit(this, arg);
		if (ifStatement.getE().getTn().isType(BOOLEAN))
			return ifStatement;
		else
			throw new TypeCheckException("Excption occured in if statement visit " + ifStatement.toString());
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		intLitExpression.setTn(INTEGER);
		return intLitExpression;
	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this, arg);
		if (sleepStatement.getE().getTn().isType(INTEGER))
			return sleepStatement;
		else
			throw new TypeCheckException("Error occured in SleepStatement " + sleepStatement.toString());
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		whileStatement.getE().visit(this, arg);
		whileStatement.getB().visit(this, arg);
		if (whileStatement.getE().getTn().isType(BOOLEAN))
			return whileStatement;
		else
			throw new TypeCheckException("Exception occured in whileStatement visit " + whileStatement.toString());
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		// insert into symbol table and get the status
		if (symtab.insert(declaration.getIdent().getText(), declaration)){
			declaration.setTn(null);
			return declaration;
		}else
			throw new TypeCheckException(
					"Exception thrown in visit declaration while inserting into Symbol Table failed: "
							+ declaration.toString());
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		String message = "";
		try {
			for (ParamDec p : program.getParams()) {
				// visit all param in list
				p.visit(this, arg);
			}
			// Getting block from program and visit
			program.getB().visit(this, arg);
			return program;
		} catch (Exception e) {
			if (e.getMessage().isEmpty())
				message = "Error in Parser ";
			else
				message = e.getMessage();
			throw new TypeCheckException("Exception occured in visit program: " + message);
		}
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getVar().visit(this, arg);
		assignStatement.getE().visit(this, arg);
		// Checking identl value with expression type
		if (assignStatement.getVar().getDec().getTn().isType(assignStatement.getE().getTn()))
			return assignStatement;
		else
			throw new TypeCheckException(
					"Exception occured in AssignmentStatement visit " + assignStatement.toString());
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		Dec dec = symtab.lookup(identX.getText());
		if (null != dec) {
			dec.setTn(null);
			identX.setDec(dec);
		}
		return identX;
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		if (symtab.insert(paramDec.getIdent().getText(), paramDec)){
			paramDec.setTn(null);
			return paramDec;
		}
		throw new TypeCheckException("Symbol Table insertion failed: " + paramDec.toString());
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		try {
			constantExpression.setTn(INTEGER);
			return constantExpression;
		} catch (TypeCheckException e) {
			// e.printStackTrace();
			// throw new TypeCheckException("Exception thrown in visit constant
			// Expression");
			return null;
		}
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		imageOpChain.getArg().visit(this, arg);
		if (imageOpChain.firstToken.kind.equals(KW_SCALE)) {
			if (imageOpChain.getArg().getExprList().size() == 1)
				imageOpChain.setTn(IMAGE);
			else
				throw new TypeCheckException(
						"Exception occured while visiting image op chain " + imageOpChain.toString());
		} else if (imageOpChain.firstToken.kind.equals(OP_HEIGHT) || imageOpChain.firstToken.kind.equals(OP_WIDTH)) {
			if (imageOpChain.getArg().getExprList().size() == 0)
				imageOpChain.setTn(INTEGER);
			else
				throw new TypeCheckException(
						"Exception occured while visiting image op chain " + imageOpChain.toString());
		}
		return imageOpChain;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		// for all expression in List<Expression>: Expression.type = INTEGER
		for (Expression exp : tuple.getExprList()) {
			exp.visit(this, arg);
			if (exp.getTn() != INTEGER)
				throw new TypeCheckException("Invalid Condition in Tuple of expression " + exp.toString());
		}
		return tuple;
	}

}
