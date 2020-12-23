package io.automatiko.engine.api.workflow.cases;

public class CaseFileItemDuplicatedException extends RuntimeException {

    private static final long serialVersionUID = 6025186042413286274L;

    private String fileItemName;

    public CaseFileItemDuplicatedException(String fileItemName) {
        super("Case file item '" + fileItemName + " already exists");
        this.fileItemName = fileItemName;
    }

    public String getFileItemName() {
        return this.fileItemName;
    }

}
