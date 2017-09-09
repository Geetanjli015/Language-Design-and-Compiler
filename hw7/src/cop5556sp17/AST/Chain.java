package cop5556sp17.AST;

import cop5556sp17.Scanner.Token;
import cop5556sp17.TypeCheckVisitor.TypeCheckException;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.Parser.SyntaxException;


public abstract class Chain extends Statement {
	public TypeName tn;
	
	public void setTn(TypeName tn) throws TypeCheckException{
		if(null!=tn) this.tn = tn;
		else
			try {
				this.tn = Type.getTypeName(firstToken);
			} catch (SyntaxException e) {
				e.printStackTrace();
				throw new TypeCheckException("Exception thrown while getting type from token of chain");
			}
	}
	
	public TypeName getTn() {
		return tn;
	}
	
	
	public Chain(Token firstToken) {
		super(firstToken);
	}

}
