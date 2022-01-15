package $Package$;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.automatiko.engine.services.event.AbstractProcessDataEvent;

public class $TypeName$ extends AbstractProcessDataEvent<$Type$> {
    
    public $TypeName$() {
        super(null, null);
    }
    
    public $TypeName$(String source, $Type$ body) {
        super(source, body);
    }
    
    public $TypeName$(String type, String source, $Type$ body) {
        super(type, source, body);
    }
    
    public $TypeName$(String specversion, String id, String source, String type, String time, $Type$  data) {
        super(specversion, id, source, type, time, data);
    }
    
    @Override
    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    @JsonAnySetter
    public void addExtension(String name, Object value) {
        this.extensions.put(name, value);
    }
}
