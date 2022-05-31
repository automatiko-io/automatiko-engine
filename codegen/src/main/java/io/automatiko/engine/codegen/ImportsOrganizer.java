package io.automatiko.engine.codegen;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ImportsOrganizer extends VoidVisitorAdapter<Object> {

    private Set<String> referencedTypes;

    private static Logger log = LoggerFactory.getLogger(ImportsOrganizer.class);

    public static void organize(CompilationUnit unit) {
        if ("false".equalsIgnoreCase(System.getProperty("io.automtiko.codegen.imports.remove"))) {
            return;
        }

        ImportsOrganizer i = new ImportsOrganizer();

        i.visit(unit, new Object());
    }

    public void visit(ClassOrInterfaceType n, Object ctx) {
        if (n.getScope().isPresent()) {
            referencedTypes.add(n.getScope().toString() + "." + n.getName());
            referencedTypes.add(n.getScope().toString());
        } else {
            referencedTypes.add(n.getNameAsString());
        }
        super.visit(n, ctx);
    }

    public void visit(final ClassOrInterfaceDeclaration n, Object ctx) {

        n.getAnnotations().forEach(a -> {
            referencedTypes.add(a.getNameAsString());

            a.findAll(AnnotationExpr.class).forEach(ca -> referencedTypes.add(ca.getNameAsString()));
        });

        super.visit(n, ctx);
    }

    public void visit(final MethodDeclaration n, Object ctx) {

        n.getAnnotations().forEach(a -> {
            referencedTypes.add(a.getNameAsString());

            a.findAll(AnnotationExpr.class).forEach(ca -> referencedTypes.add(ca.getNameAsString()));
        });
        referencedTypes.add(n.getTypeAsString());

        super.visit(n, ctx);
    }

    public void visit(final Parameter n, Object ctx) {

        n.getAnnotations().forEach(a -> {
            referencedTypes.add(a.getNameAsString());
        });
        referencedTypes.add(n.getTypeAsString());

        super.visit(n, ctx);
    }

    public void visit(NameExpr n, Object ctx) {
        referencedTypes.add(n.toString());
        super.visit(n, ctx);
    }

    public void visit(CompilationUnit n, Object ctx) {
        String pDeclaration = n.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("null");
        referencedTypes = new HashSet<String>();
        super.visit(n, ctx);
        log.debug("Removing unused import for {}",
                n.getTypes().stream().map(t -> t.getNameAsString()).collect(Collectors.joining(",")));
        List<ImportDeclaration> imports = n.getImports();
        if (imports != null && !referencedTypes.isEmpty()) {
            List<ImportDeclaration> cleanImportList = new LinkedList<ImportDeclaration>(
                    imports);
            Iterator<ImportDeclaration> itImport = cleanImportList.iterator();
            while (itImport.hasNext()) {
                ImportDeclaration id = itImport.next();
                if (id.isAsterisk()) {
                    // avoid removing asterisk imports
                    continue;
                }

                String importName = id.getName().toString();

                if (importName.startsWith(pDeclaration)) {
                    itImport.remove();
                    log.debug("\t Removed {}", importName);
                } else if (!id.isStatic() && !id.isAsterisk()) {
                    boolean found = false;

                    Iterator<String> it = referencedTypes.iterator();
                    while (it.hasNext() && !found) {
                        String name = it.next();
                        int dotIndex = name.indexOf(".");
                        if (dotIndex != -1) {
                            name = name.substring(dotIndex);
                        } else {
                            name = "." + name;
                        }
                        found = importName.endsWith(name);
                    }
                    if (!found) {
                        itImport.remove();
                        log.debug("\t Removed {}", importName);
                    }
                }
            }
            n.setImports(NodeList.nodeList(cleanImportList));
        }
    }

}
