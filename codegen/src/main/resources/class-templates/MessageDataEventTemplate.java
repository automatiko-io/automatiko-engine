package $Package$;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.automatiko.engine.api.event.AbstractDataEvent;

public class $TypeName$ extends AbstractDataEvent<$Type$> {
    
    public $TypeName$() {
        super(null, null, null);
    }
    
    public $TypeName$(String source, $Type$ body) {
        super(null, source, body);
    }
    
    public $TypeName$(String type, String source, $Type$ body) {
        super(type, source, body);
    }
    
    public $TypeName$(String specversion, String id, String source, String type, String subject, String time, $Type$  data) {
        super(specversion, id, source, type, subject, time, data);
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
