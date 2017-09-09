package cop5556sp17;



import java.util.HashMap;
import java.util.ListIterator;
import java.util.Stack;

import cop5556sp17.TypeCheckVisitor.TypeCheckException;
import cop5556sp17.AST.Dec;


public class SymbolTable {
	
	//TODO  add fields
	Stack<Integer> scope_stack;
	int current_scope,next_scope;
	HashMap<String,HashMap<Integer,Dec>> hashMp = new HashMap<>();
	/** 
	 * to be called when block entered
	 */
	public void enterScope(){
		current_scope=next_scope++;
		scope_stack.push(current_scope);
	}
	
	
	/**
	 * leaves scope
	 */
	public void leaveScope(){
		scope_stack.pop();
		current_scope=scope_stack.peek();
	}
	
	public boolean insert(String ident, Dec dec) throws TypeCheckException{
		//TODO:  IMPLEMENT THIS
		if(!hashMp.containsKey(ident)){
			HashMap<Integer,Dec> newEntry = new HashMap<Integer,Dec>();
			newEntry.put(current_scope, dec);
			hashMp.put(ident, newEntry);
		}else {
		HashMap<Integer,Dec> newEntry = hashMp.get(ident);
		if(newEntry.containsKey(current_scope)) throw new TypeCheckException("Duplicate ident");
		newEntry.put(current_scope, dec);
		hashMp.put(ident, newEntry);
		}
		
		if(hashMp.containsKey(ident)) return true;
		return false;
	}
	
	public Dec lookup(String ident){
		HashMap<Integer,Dec> getIdentScopes = hashMp.get(ident);
		ListIterator<Integer> stackIterat = scope_stack.listIterator(scope_stack.size());
		while(stackIterat.hasPrevious()){
			Integer scpeLevel = stackIterat.previous();
			if(getIdentScopes.get(scpeLevel)!=null){
				return getIdentScopes.get(scpeLevel);
			}
		}
		return null;
	}
		
	public SymbolTable() {
		current_scope=0;
		next_scope=current_scope+1;
		scope_stack = new Stack<Integer>();
		scope_stack.push(0);
	}


	@Override
	public String toString() {
		return "SymbolTable [scope_stack=" + scope_stack + ", current_scope=" + current_scope + ", next_scope="
				+ next_scope + ", hashMp=" + hashMp + "]";
	}
	
	


}
