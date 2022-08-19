import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;

import io.automatiko.engine.workflow.builder.BuilderContext;

public class LambdaParser {

    public static final void main(String[] args) throws FileNotFoundException {

        LambdaParser parser = new LambdaParser();

        Map<String, List<String>> data = parser.parseLambdas(
                "/Users/mswiderski/Development/workspaces/automatiko-dev/workflow-as-code/src/main/java/io/automatiko/tests/MyWorkflows.java");

        System.out.println(data);
    }

    public Map<String, List<String>> parseLambdas(String sourceFile) throws FileNotFoundException {

        Map<String, List<String>> data = new LinkedHashMap<>();

        CompilationUnit unit = StaticJavaParser.parse(new File(sourceFile));

        unit.findAll(MethodDeclaration.class, md -> {
            if (md.isPublic() && md.getType().toString().equals("WorkflowBuilder")) {
                return true;
            }

            return false;
        }).forEach(md -> {
            String methodName = md.getNameAsString();
            List<String> lambdas = new ArrayList<>();

            md.findAll(LambdaExpr.class).forEach(l -> lambdas.add(l.getBody().toString()));
            BuilderContext.addMethodData(methodName, lambdas);
        });

        return data;
    }
}
