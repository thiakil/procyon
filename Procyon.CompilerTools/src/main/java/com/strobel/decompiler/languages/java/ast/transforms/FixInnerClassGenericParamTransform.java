package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.GenericParameter;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstType;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.EntityDeclaration;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.Roles;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeParameterDeclaration;

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
			//TODO inner types (including inner type of subclasses using inners of super)
			type.acceptVisitor(visitor, null);
		}
	}

	private static class Visitor extends ContextTrackingVisitor<Void>{

		protected Visitor(DecompilerContext context) {
			super(context);
		}

		@Override
		public Void visitTypeDeclaration(TypeDeclaration type, Void p) {
			type.getBaseType().acceptVisitor(this, null);
			for (AstType _interface : type.getInterfaces()){
				_interface.acceptVisitor(this, null);
			}
			for (TypeParameterDeclaration t : type.getTypeParameters()){
				t.getExtendsBound().acceptVisitor(this, null);
			}
			for (EntityDeclaration e : type.getChildrenByRole(Roles.TYPE_MEMBER)){
				if (e instanceof TypeDeclaration){
					visitTypeDeclaration((TypeDeclaration)e, null);
				}
			}
			return null;
		}

		@Override
		public Void visitSimpleType(SimpleType node, Void data) {
			super.visitSimpleType(node, data);

			TypeReference typeReference = node.toTypeReference();
			if (typeReference instanceof GenericParameter){
				return null;
			} else if (typeReference.isNested()/* && node.toTypeReference().getDeclaringType().equals(context.getCurrentType())*/){
				Identifier identifier = node.getIdentifierToken();
				String oldName = identifier.getName();
				if (!oldName.startsWith(typeReference.getDeclaringType().getSimpleName()))
					identifier.setName(typeReference.getDeclaringType().getSimpleName()+"."+oldName);
			}
			return null;
		}
	}
}
