package cop5556sp17;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTVisitor;
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
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import static cop5556sp17.AST.Type.TypeName.FRAME;
import static cop5556sp17.AST.Type.TypeName.IMAGE;
import static cop5556sp17.AST.Type.TypeName.URL;
import static cop5556sp17.Scanner.Kind.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;
	private int slotIter = 0;
	private int slot = 0;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		for (ParamDec dec : params)
			dec.visit(this, mv);
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
		// TODO visit the local variables
		// I plan to get the list of dec and then visiting local variable by
		// extracting required fields
		for (Dec dec : program.getB().getDecs()) {
			mv.visitLocalVariable(dec.getIdent().getText(), dec.getTn().getJVMTypeDesc(), null, startRun, endRun,
					dec.getSlot());
		}
		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method

		cw.visitEnd();// end of class

		// generate classfile and return it
		return cw.toByteArray();
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getE().visit(this, arg);
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getTn());
		assignStatement.getVar().visit(this, arg);
		return null;
	}


	
	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		//visit e0 first
		binaryChain.getE0().visit(this, true);
		
		//check whether files needs to be downloaded from url or file and then visit method
		
		if(binaryChain.getE0().getTn().getJVMTypeDesc().equals("Ljava/net/URL;")){
			
				mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageIO", "readFromURL", PLPRuntimeImageIO.readFromURLSig,false);
		}else if(binaryChain.getE0().getTn().getJVMTypeDesc().equals("Ljava/io/File;")){
			
				mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageIO", "readFromFile", PLPRuntimeImageIO.readFromFileDesc,false);
		}
		mv.visitInsn(DUP);
		binaryChain.getE1().visit(this, false);
		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		binaryExpression.getE0().visit(this, arg);
		binaryExpression.getE1().visit(this, arg);
		Label startExp = new Label();
		Label endExp = new Label();
		//Now consider all the ops cases
		if(binaryExpression.getOp().kind==TIMES){
			if(binaryExpression.getTn() == IMAGE) {
				if(binaryExpression.getE0().getTn() == TypeName.INTEGER)
					mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"mul", PLPRuntimeImageOps.mulSig, false);
			} else {
				mv.visitInsn(IMUL);
			}
		}else if(binaryExpression.getOp().kind==OR){
			mv.visitInsn(IOR);
		}else if(binaryExpression.getOp().kind==MINUS){
			if(binaryExpression.getTn() == IMAGE) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"sub", PLPRuntimeImageOps.subSig, false);
			} else {
				mv.visitInsn(ISUB);
			}
		}else if(binaryExpression.getOp().kind==DIV){
			if(binaryExpression.getTn() == IMAGE) {
				if(binaryExpression.getE0().getTn() == TypeName.INTEGER)
					mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"div", PLPRuntimeImageOps.divSig, false);
			} else {
				mv.visitInsn(IDIV);
			}
		}else if(binaryExpression.getOp().kind==MOD){
			if(binaryExpression.getTn() == IMAGE) {
				if(binaryExpression.getE0().getTn() == TypeName.INTEGER)
					mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"mod", PLPRuntimeImageOps.modSig, false);
			} else {
				mv.visitInsn(IREM);
			}
		}else if(binaryExpression.getOp().kind==PLUS){
			if(binaryExpression.getTn() == IMAGE) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"add", PLPRuntimeImageOps.addSig, false);
			} else {
				mv.visitInsn(IADD);
			}
		}else if(binaryExpression.getOp().kind==GE){
			mv.visitJumpInsn(IF_ICMPGE, startExp);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, endExp);
			mv.visitLabel(startExp);
			mv.visitLdcInsn(true);
			mv.visitLabel(endExp);
		}else if(binaryExpression.getOp().kind==AND){
			mv.visitInsn(IAND);
		}else if(binaryExpression.getOp().kind==LT){
			mv.visitJumpInsn(IF_ICMPLT, startExp);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, endExp);
			mv.visitLabel(startExp);
			mv.visitLdcInsn(true);
			mv.visitLabel(endExp);
		}else if(binaryExpression.getOp().kind==LE){
			mv.visitJumpInsn(IF_ICMPLE, startExp);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, endExp);
			mv.visitLabel(startExp);
			mv.visitLdcInsn(true);
			mv.visitLabel(endExp);
		}else if(binaryExpression.getOp().kind==NOTEQUAL){
			if (binaryExpression.getE0().getTn().isType(TypeName.INTEGER, TypeName.BOOLEAN)) {
				mv.visitJumpInsn(IF_ICMPNE, startExp);
			}
			else {
				mv.visitJumpInsn(IF_ACMPNE, startExp);
			}
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, endExp);
			mv.visitLabel(startExp);
			mv.visitLdcInsn(true);
			mv.visitLabel(endExp);
		}else if(binaryExpression.getOp().kind==GT){
			mv.visitJumpInsn(IF_ICMPGT, startExp);
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, endExp);
			mv.visitLabel(startExp);
			mv.visitLdcInsn(true);
			mv.visitLabel(endExp);
		}else if(binaryExpression.getOp().kind==EQUAL){
			if (binaryExpression.getE0().getTn().isType(TypeName.INTEGER, TypeName.BOOLEAN)) {
				mv.visitJumpInsn(IF_ICMPEQ, startExp);
			}
			else {
				mv.visitJumpInsn(IF_ACMPEQ, startExp);
			}
			mv.visitLdcInsn(false);
			mv.visitJumpInsn(GOTO, endExp);
			mv.visitLabel(startExp);
			mv.visitLdcInsn(true);
			mv.visitLabel(endExp);
		}

		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		Label startBlock = new Label();
		Label endBlock = new Label();
		mv.visitLabel(startBlock);
		// Now Visiting dec
		for (Dec d : block.getDecs())
			d.visit(this, arg);
		// Now checking statements of the block
		for (Statement statemt : block.getStatements()) {
			if (statemt instanceof AssignmentStatement)
				if (((AssignmentStatement) statemt).getVar().getDec() instanceof ParamDec)
					mv.visitVarInsn(ALOAD, 0);
			// visitng statements
			statemt.visit(this, arg);
			
			if(statemt instanceof BinaryChain){
				mv.visitInsn(POP);
			}
		}
		
		
		mv.visitLabel(endBlock);
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		// TODO Implement this
		mv.visitLdcInsn(booleanLitExpression.getValue());
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
//		assert false : "not yet implemented";
	
	if(constantExpression.getFirstToken().kind.equals(KW_SCREENWIDTH)){
		mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFrame","getScreenWidth",PLPRuntimeFrame.getScreenWidthSig ,false);
	}else if (constantExpression.getFirstToken().kind.equals(KW_SCREENHEIGHT)){
		mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFrame","getScreenHeight",PLPRuntimeFrame.getScreenHeightSig ,false);
	}
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		// TODO Implement this
		declaration.setSlot(slot);
		slot = slot + 1;
		
		if((declaration.getTn().getJVMTypeDesc().equals("Lcop5556sp17/PLPRuntimeFrame;"))||(declaration.getTn().getJVMTypeDesc().equals("Ljava/awt/image/BufferedImage;"))){
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, declaration.getSlot());
		}
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
			mv.visitInsn(POP);
			mv.visitInsn(ACONST_NULL);
		
		switch(filterOpChain.firstToken.kind){
		case OP_GRAY :
			mv.visitInsn(POP);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
		break;
		case OP_BLUR :
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
			break;
		case OP_CONVOLVE:
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig, false);
			break;
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		frameOpChain.getArg().visit(this, arg);
		switch(frameOpChain.getFirstToken().kind){
		case KW_YLOC:
			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "getYVal", PLPRuntimeFrame.getYValDesc,false);
			break;
		case KW_HIDE :
			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "hideImage", PLPRuntimeFrame.hideImageDesc,false);
			break;
		case KW_XLOC:
			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "getXVal", PLPRuntimeFrame.getXValDesc,false);
			break;
		case KW_MOVE:
			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "moveFrame", PLPRuntimeFrame.moveFrameDesc,false);
			break;
		case KW_SHOW :
			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "showImage", PLPRuntimeFrame.showImageDesc,false);
			break;
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		boolean left = (boolean)arg;
		Dec dec = identChain.getD();
		String fType = dec.getTn().getJVMTypeDesc();
		if(left) {
			visitIdentChainLeft(identChain, dec, fType);
		} else {
			visitIdentChainRight(identChain, dec, fType);
		}		return null;
	}

	private void visitIdentChainRight(IdentChain identChain, Dec dec, String fType) {
			if(!(dec instanceof ParamDec)){
				//not a instance of param dec
				if(dec.getTn() == TypeName.INTEGER)
					mv.visitVarInsn(ISTORE, dec.getSlot());
				else if(identChain.getD().getTn() == TypeName.FRAME) {
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, dec.getSlot());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName,"createOrSetFrame", PLPRuntimeFrame.createOrSetFrameSig, false);
					mv.visitInsn(DUP);
					mv.visitVarInsn(ASTORE, dec.getSlot());
				}
				else if(dec.getTn() == TypeName.FILE)  {
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, dec.getSlot());
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), dec.getTn().getJVMTypeDesc());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className,"write", PLPRuntimeImageIO.writeImageDesc, false);
					//mv.visitVarInsn(ASTORE, dec.getSlot());
				} 
				else if(dec.getTn() == TypeName.IMAGE) {
					//mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"copyImage", PLPRuntimeImageOps.copyImageSig, false);
					mv.visitVarInsn(ASTORE, dec.getSlot());
				}
			
			}else if (dec instanceof ParamDec) {
				//instance of param dec
			if (dec.getTn() == TypeName.FILE) {
				mv.visitInsn(POP);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), dec.getTn().getJVMTypeDesc());
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write", PLPRuntimeImageIO.writeImageDesc, false);
			} else {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitInsn(SWAP);
				mv.visitFieldInsn(PUTFIELD, className, identChain.getD().getIdent().getText(), fType);
			}
		}
	}

	private void visitIdentChainLeft(IdentChain identChain, Dec dec, String fType) {
		if(dec instanceof ParamDec) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, identChain.getD().getIdent().getText(), fType);
		}
		else {
			if(identChain.getTn() == TypeName.INTEGER)
				mv.visitVarInsn(ILOAD, dec.getSlot());
			else 
				mv.visitVarInsn(ALOAD, dec.getSlot());
		}
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		
		Dec dec = identExpression.getDec();
		if (!(dec instanceof ParamDec)) {
			if(identExpression.getTn() == TypeName.BOOLEAN || identExpression.getTn() == TypeName.INTEGER)
				mv.visitVarInsn(ILOAD, dec.getSlot());
			else
				mv.visitVarInsn(ALOAD, dec.getSlot());
		}else{
			mv.visitVarInsn(ALOAD, 0);
			String type = "";
			if(dec.getTn() == TypeName.INTEGER)
				mv.visitFieldInsn(GETFIELD, className, identExpression.getFirstToken().getText(), "I");
			else if(dec.getTn() == TypeName.BOOLEAN)
				mv.visitFieldInsn(GETFIELD, className, identExpression.getFirstToken().getText(), "Z");
		}
		 
		return null;
	}

	
	
	
	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		String type = "";
		Dec d = identX.getDec();
		if (d instanceof ParamDec) {
			mv.visitFieldInsn(PUTFIELD, className, identX.getFirstToken().getText(), d.getTn().getJVMTypeDesc());
		} else {
			if(identX.getDec().getTn() == TypeName.IMAGE) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"copyImage", PLPRuntimeImageOps.copyImageSig, false);
				mv.visitVarInsn(ASTORE, d.getSlot());
			}else if(identX.getDec().getTn() == TypeName.INTEGER || identX.getDec().getTn() == TypeName.BOOLEAN)
				mv.visitVarInsn(ISTORE, d.getSlot());
			else {
				mv.visitVarInsn(ASTORE, d.getSlot());
			}
		}
		return null;

	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
//		 Expression
//         IFEQ AFTER
//     Block
//		AFTER …
		ifStatement.getE().visit(this, arg);
		Label check = new Label();
		mv.visitJumpInsn(IFEQ, check);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(check);
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		imageOpChain.getArg().visit(this, arg);
		switch(imageOpChain.getFirstToken().kind){
		case OP_WIDTH:
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage", "getWidth", PLPRuntimeImageOps.getWidthSig, false);
			break;
		case KW_SCALE:
			mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "scale", PLPRuntimeImageOps.scaleSig, false);
			break;
		case OP_HEIGHT:
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage", "getHeight", PLPRuntimeImageOps.getHeightSig, false);
			break;
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		mv.visitLdcInsn(intLitExpression.value);
		return null;
	}
	
	
	

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		//instance variable in class, initialized with values from command line arguments
		String type = paramDec.getTn().getJVMTypeDesc();
		FieldVisitor visit = cw.visitField(ACC_PUBLIC, paramDec.getIdent().getText(), type, null, null);
		visit.visitEnd();
		// setting slot
		paramDec.setSlot(slot++);
		mv.visitVarInsn(ALOAD, 0);
		//case switch:
		switch(paramDec.getTn()){
		case BOOLEAN :
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(slotIter++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), "Z");
			break;
		case INTEGER: 
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(slotIter++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), "I");
			break;
		case URL :
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(slotIter++);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "getURL", PLPRuntimeImageIO.getURLSig, false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), paramDec.getTn().getJVMTypeDesc());
			break;
		case FILE :
			mv.visitTypeInsn(NEW, "java/io/File");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(slotIter++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), paramDec.getTn().getJVMTypeDesc());
			break;
		default:
			break;
		
		}
			return null;

	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		//	SleepStatement ∷= Expression
		Expression sleepExp = sleepStatement.getE();
		sleepExp.visit(this, arg);
		//To change the integer expression to a long  with “I2L”
		mv.visitInsn(I2L);
       //invoke java/lang/Thread/sleep.  
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread","sleep", "(J)V",false);
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		//list of expressions
		for(Expression e : tuple.getExprList()){
			//now visit each expressions
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		
//		WhileStatement ∷= Expression Block
//	              goto GUARD
//	   BODY     Block
//	   GUARD  Expression
//	                  IFNE  BODY
		
		Label guard = new Label();
		mv.visitJumpInsn(GOTO, guard);
		Label Wbody = new Label();
		mv.visitLabel(Wbody);
		//visit block
		whileStatement.getB().visit(this, arg);
		mv.visitLabel(guard);
		whileStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFNE, Wbody);
		return null;
	}

}
