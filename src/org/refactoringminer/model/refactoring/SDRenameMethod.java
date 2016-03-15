package org.refactoringminer.model.refactoring;

import org.refactoringminer.model.SDMethod;

import gr.uom.java.xmi.diff.RefactoringType;

public class SDRenameMethod extends SDRefactoring {

    private final SDMethod methodBefore;
    private final SDMethod methodAfter;
    
    public SDRenameMethod(SDMethod methodBefore, SDMethod methodAfter) {
        super(RefactoringType.RENAME_METHOD, methodAfter);
        this.methodBefore = methodBefore;
        this.methodAfter = methodAfter;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(methodBefore.getVerboseSimpleName());
        sb.append(" renamed to ");
        sb.append(methodAfter.getVerboseSimpleName());
        sb.append(" in class ");
        sb.append(methodAfter.container().fullName());
        return sb.toString();
    }
}