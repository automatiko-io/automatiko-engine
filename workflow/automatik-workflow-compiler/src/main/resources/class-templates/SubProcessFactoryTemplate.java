import io.automatik.engine.api.workflow.ProcessInstance;

class Template {
    Object f = new io.automatik.engine.workflow.process.core.node.SubProcessFactory<$Type$>() {
        public $Type$ bind(io.automatik.engine.api.runtime.process.ProcessContext kcontext) {
            return null;
        }
        public io.automatik.engine.api.workflow.ProcessInstance<$Type$> createInstance($Type$ model) {
            return null;
        }
        public void unbind(io.automatik.engine.api.runtime.process.ProcessContext kcontext, $Type$ model) {

        }
        public void abortInstance(String instanceId) {
            
        }
        
        private void internalAbortInstance(java.util.Optional<? extends io.automatik.engine.api.workflow.ProcessInstance<$Type$>> processInstance) {
            processInstance.ifPresent(pi -> {

                try {
                    pi.abort();
                } catch (IllegalArgumentException e) {
                    // ignore it as this might be thrown in case of canceling already aborted instance
                }
            });
        }
    };
}
