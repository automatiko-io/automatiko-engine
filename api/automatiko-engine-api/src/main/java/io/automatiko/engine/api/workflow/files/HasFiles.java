package io.automatiko.engine.api.workflow.files;

/**
 * Indicates that a java object contains files. Type of the file is declared as parametrized type
 *
 * @param <T> type of file used. Can be single file or collection of files
 */
public interface HasFiles<T> {

    /**
     * Returns file or collection of files included in the object
     * 
     * @return files
     */
    T files();

    /**
     * Called at the time when file or collection of files where augmented by storage mechanism
     * 
     * @param augmented file or collection of files augmented
     */
    void augmentFiles(T augmented);
}
