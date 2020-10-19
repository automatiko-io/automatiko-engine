package io.automatik.engine.api.workflow.cases;

public class CaseFileItemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 6025186042413286274L;

    private String fileItemName;

    public CaseFileItemNotFoundException(String fileItemName) {
        super("Case file item '" + fileItemName + " not found");
        this.fileItemName = fileItemName;
    }

    public String getFileItemName() {
        return this.fileItemName;
    }

}
