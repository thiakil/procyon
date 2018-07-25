package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstType;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;

/**
 * Top level type declarations can't actually see their inner types by simple name, prepend the outer simple name
 */
public class FixInnerClassGenericParamTransform implements IAstTransform {

	private final DecompilerContext _context;

	public FixInnerClassGenericParamTransform(final DecompilerContext context) {
		_context = VerifyArgument.notNull(context, "context");
	}

	@Override
	public void run(AstNode compilationUnit) {
		Visitor visitor = new Visitor(_context);
		for (TypeDeclaration type : ((CompilationUnit)compilationUnit).getTypes()){
			type.getBaseType().acceptVisitor(visitor, null);
			for (AstType _interface : type.getInterfaces()){
				_interface.acceptVisitor(visitor, null);
			}
		}
	}

	private static class Visitor extends ContextTrackingVisitor<Void>{

		protected Visitor(DecompilerContext context) {
			super(context);
		}

		@Override
		public Void visitSimpleType(SimpleType node, Void data) {
			super.visitSimpleType(node, data);

			if (node.toTypeReference().isNested() && node.toTypeReference().getDeclaringType().equals(context.getCurrentType())){
				Identifier identifier = node.getIdentifierToken();
				String oldName = identifier.getName();
				identifier.setName(context.getCurrentType().getSimpleName()+"."+oldName);
			}
			return null;
		}
	}
}
