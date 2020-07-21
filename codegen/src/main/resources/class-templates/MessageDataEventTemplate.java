package $Package$;


import io.automatik.engine.services.event.AbstractProcessDataEvent;

public class $TypeName$ extends AbstractProcessDataEvent<$Type$> {
    
    private String automatikStartFromNode;
    
    public $TypeName$() {
        super(null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null);
    }
    
    public $TypeName$(String source, 
                           $Type$ body,
                           String automatikProcessinstanceId,
                           String automatikParentProcessinstanceId,
                           String automatikRootProcessinstanceId,
                           String automatikProcessId,
                           String automatikRootProcessId,
                           String automatikProcessinstanceState) {
        super(source,
              body,
              automatikProcessinstanceId,
              automatikParentProcessinstanceId,
              automatikRootProcessinstanceId,
              automatikProcessId,
              automatikRootProcessId,
              automatikProcessinstanceState,
              null);
    }

    
    public void setAutomatikStartFromNode(String automatikStartFromNode) {
        this.automatikStartFromNode = automatikStartFromNode;
    }
    
    public String getAutomatikStartFromNode() {
        return this.automatikStartFromNode;
    }
}
