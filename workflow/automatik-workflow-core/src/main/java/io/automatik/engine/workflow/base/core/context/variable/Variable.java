
package io.automatik.engine.workflow.base.core.context.variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.SourceVersion;

import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.base.core.ValueObject;
import io.automatik.engine.workflow.base.core.datatype.impl.type.UndefinedDataType;

/**
 * Default implementation of a variable.
 * 
 */
public class Variable implements ValueObject, Serializable, io.automatik.engine.api.workflow.Variable {

    private static final long serialVersionUID = 510l;

    public static final String VARIABLE_TAGS = "tags";

    public static final String READONLY_TAG = "readonly";
    public static final String REQUIRED_TAG = "required";
    public static final String INTERNAL_TAG = "internal";
    public static final String NOT_NULL_TAG = "notnull";
    public static final String INPUT_TAG = "input";
    public static final String OUTPUT_TAG = "output";
    public static final String BUSINESS_RELEVANT = "business-relevant";
    public static final String TRACKED = "tracked";
    public static final String AUTO_INITIALIZED = "auto-initialized";
    public static final String BUSINESS_KEY = "business-key";
    public static final String INITIATOR = "initiator";

    public static final String DEFAULT_VALUE = "value";

    private String id;
    private String name;
    private String sanitizedName;
    private DataType type;
    private Object value;
    private Map<String, Object> metaData = new HashMap<String, Object>();

    private List<String> tags = new ArrayList<>();

    public Variable() {
        this.type = UndefinedDataType.getInstance();
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
        this.sanitizedName = sanitizeIdentifier(name);
    }

    public String getSanitizedName() {
        return sanitizedName;
    }

    /**
     * Return a valid unique Java identifier based on the given @param name. It
     * consider valid characters and reserved words. In case the input is valid it
     * is returned itself otherwise a valid identifier is generated prefixing v$
     * based on the @param input excluding invalid characters.
     * 
     * @param name the input
     * @return the output valid Java identifier
     */
    private static String sanitizeIdentifier(String name) {
        return Optional.ofNullable(name).filter(SourceVersion::isName).orElseGet(() -> {
            String identifier = StringUtils.extractFirstIdentifier(name, 0);
            return Optional.ofNullable(identifier).filter(s -> !StringUtils.isEmpty(s)).filter(SourceVersion::isName)
                    // prepend v$ in front of the variable name to prevent clashing with reserved
                    // keywords
                    .orElseGet(() -> String.format("v$%s", identifier));
        });
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DataType getType() {
        return this.type;
    }

    public void setType(final DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }
        this.type = type;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(final Object value) {
        if (this.type.verifyDataType(value)) {
            this.value = value;
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append("Value <");
            sb.append(value);
            sb.append("> is not valid for datatype: ");
            sb.append(this.type);
            throw new IllegalArgumentException(sb.toString());
        }
    }

    public void setMetaData(String name, Object value) {
        this.metaData.put(name, value);

        if (VARIABLE_TAGS.equals(name) && value != null) {
            tags = Arrays.asList(value.toString().split(","));
        }
    }

    public Object getMetaData(String name) {
        return this.metaData.get(name);
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public String toString() {
        return this.name;
    }

    public List<String> getTags() {
        if (tags.isEmpty() && this.metaData.containsKey(VARIABLE_TAGS)) {
            tags = Arrays.asList(metaData.get(VARIABLE_TAGS).toString().split(","));

        }
        return tags;
    }

    public boolean hasTag(String tagName) {
        return getTags().contains(tagName);
    }

    public boolean matchByIdOrName(String nameOrId) {
        return ((id != null && id.equals(nameOrId)) || name.equals(nameOrId));
    }
}
