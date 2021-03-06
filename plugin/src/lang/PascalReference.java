package com.siberika.idea.pascal.lang;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.siberika.idea.pascal.lang.parser.NamespaceRec;
import com.siberika.idea.pascal.lang.psi.PascalNamedElement;
import com.siberika.idea.pascal.lang.psi.impl.PasField;
import com.siberika.idea.pascal.lang.references.PasReferenceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Date: 3/13/13
 * Author: George Bakhtadze
 */
public class PascalReference extends PsiPolyVariantReferenceBase<PascalNamedElement> {
        //PsiReferenceBase<PascalNamedElement> {
    private static final Logger LOG = Logger.getInstance(PascalReference.class);
    private final String key;

    public PascalReference(@NotNull PsiElement element, TextRange textRange) {
        super((PascalNamedElement) element, textRange);
        key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        final ResolveCache resolveCache = ResolveCache.getInstance(myElement.getProject());
        //return resolveCache.resolveWithCaching(this, Resolver.INSTANCE, true, incompleteCode, myElement.getContainingFile());
        return Resolver.INSTANCE.resolve(this, incompleteCode);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length > 0 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        Project project = myElement.getProject();
        List<LookupElement> variants = new ArrayList<LookupElement>();

        /*List<PascalNamedElement> properties = PascalParserUtil.findTypes(project);
        for (final PascalNamedElement property : properties) {
            if (property.getName().length() > 0) {
                variants.add(LookupElementBuilder.create(property).
                        withIcon(PascalIcons.GENERAL).
                        withTypeText(property.getContainingFile().getName())
                );
            }
        }*/
        return variants.toArray();
    }

    private static class Resolver implements ResolveCache.PolyVariantResolver<PascalReference> {
        public static final Resolver INSTANCE = new Resolver();

        @NotNull
        @Override
        public ResolveResult[] resolve(@NotNull PascalReference pascalReference, boolean incompleteCode) {
            if (PasField.DUMMY_IDENTIFIER.equals(pascalReference.key)) {
                return new ResolveResult[0];
            }
            final Collection<PasField> references = PasReferenceUtil.resolveExpr(null, NamespaceRec.fromElement(pascalReference.getElement()), PasField.TYPES_ALL, true, 0);
            // return only first reference
            for (PasField el : references) {
                if (el != null) {
                    PsiElement element = el.target != null ? el.target : el.getElement();
                    if (element != null) {
                        return PsiElementResolveResult.createResults(new PsiElement[]{element});
                    }
                } else {
                    LOG.info("ERROR: null resolved for " + this);
                }
            }
            return PsiElementResolveResult.createResults(new PsiElement[] {});
        }
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        final ResolveResult[] results = multiResolve(false);
        for (ResolveResult result : results) {
            if (getElement().getManager().areElementsEquivalent(getNamedElement(result.getElement()), getNamedElement(element))) {
                return true;
            }
        }
        return false;
    }

    private PsiElement getNamedElement(PsiElement element) {
        if (element instanceof PsiNameIdentifierOwner) {
            return ((PsiNameIdentifierOwner) element).getNameIdentifier();
        }
        return element;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PascalReference that = (PascalReference) o;

        if (!getElement().equals(that.getElement())) return false;
        if (!key.equals(that.key)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + getElement().hashCode();
        return result;
    }
}
