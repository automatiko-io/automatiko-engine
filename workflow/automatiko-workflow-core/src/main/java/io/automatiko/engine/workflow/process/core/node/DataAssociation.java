
package io.automatiko.engine.workflow.process.core.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataAssociation implements Serializable {

    private static final long serialVersionUID = 5L;

    private List<String> sources;
    private String target;
    private List<Assignment> assignments;
    private Transformation transformation;

    public DataAssociation(List<String> sources, String target, List<Assignment> assignments,
            Transformation transformation) {
        this.sources = sources;
        this.target = target;
        this.assignments = assignments;
        this.transformation = transformation;
    }

    public DataAssociation(final String source, String target, List<Assignment> assignments,
            Transformation transformation) {
        this.sources = new ArrayList<String>();
        this.sources.add(source);
        this.target = target;
        this.assignments = assignments;
        this.transformation = transformation;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
    }

}
