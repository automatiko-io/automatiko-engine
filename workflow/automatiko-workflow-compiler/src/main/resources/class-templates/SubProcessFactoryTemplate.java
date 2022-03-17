import java.util.Optional;

import io.automatiko.engine.api.workflow.ProcessInstance;

class Template {
    Object f = new io.automatiko.engine.workflow.process.core.node.SubProcessFactory<$Type$>() {
        public $Type$ bind(io.automatiko.engine.api.runtime.process.ProcessContext context) {
            return null;
        }
        public io.automatiko.engine.api.workflow.ProcessInstance<$Type$> createInstance($Type$ model) {
            return null;
        }
        public void unbind(io.automatiko.engine.api.runtime.process.ProcessContext context, $Type$ model) {

        }
        public void abortInstance(String instanceId) {
            
        }
        
        @SuppressWarnings("unchecked")
        public java.util.Optional<io.automatiko.engine.api.workflow.ProcessInstance<$Type$>> findInstance(String instanceId) {
            
        }
        
        @SuppressWarnings("unchecked")
        public java.util.Optional<io.automatiko.engine.api.workflow.ProcessInstance<$Type$>> findInstanceByStatus(String instanceId, int status) {
            
        }
        
        private void internalAbortInstance(java.util.Optional<? extends io.automatiko.engine.api.workflow.ProcessInstance<$Type$>> processInstance) {
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
