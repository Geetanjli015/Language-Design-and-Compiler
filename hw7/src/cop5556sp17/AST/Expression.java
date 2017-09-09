package cop5556sp17.AST;

import cop5556sp17.Parser.SyntaxException;
import cop5556sp17.Scanner.Token;
import cop5556sp17.TypeCheckVisitor.TypeCheckException;
import cop5556sp17.AST.Type.TypeName;

public abstract class Expression extends ASTNode {
	
	public TypeName tn;
	
	public Dec dec;

	public Dec getDec() {
		return dec;
	}

	public void setDec(Dec dec) {
		this.dec = dec;
	}

	public TypeName getTn() {
		return tn;
	}

	public void setTn(TypeName tn) throws TypeCheckException{
		if(null!=tn) this.tn = tn;
		else
			try {
				this.tn = Type.getTypeName(firstToken);
			} catch (SyntaxException e) {
				e.printStackTrace();
				throw new TypeCheckException("Exception thrown while getting type from token");
			}
	}

	protected Expression(Token firstToken) {
		super(firstToken);
	}

	@Override
	abstract public Object visit(ASTVisitor v, Object arg) throws Exception;
	
}
