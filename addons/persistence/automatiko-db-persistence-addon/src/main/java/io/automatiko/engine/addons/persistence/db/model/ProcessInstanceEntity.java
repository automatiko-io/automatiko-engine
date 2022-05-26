package io.automatiko.engine.addons.persistence.db.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.automatiko.engine.api.Model;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@MappedSuperclass
public abstract class ProcessInstanceEntity extends PanacheEntityBase implements Model {

    @Id
    @Column(name = "ATK_ID")
    public String entityId;

    @Column(name = "ATK_NAME")
    @JsonIgnore
    public String name;

    @Column(name = "ATK_STATE")
    @JsonIgnore
    public Integer state;

    @Column(name = "ATK_PROCESS_ID")
    @JsonIgnore
    public String processId;

    @Column(name = "ATK_PROCESS_NAME")
    @JsonIgnore
    public String processName;

    @Column(name = "ATK_PROCESS_VERSION")
    @JsonIgnore
    public String processVersion;

    @Column(name = "ATK_START_DATE")
    @JsonIgnore
    public Date startDate;

    @Column(name = "ATK_END_DATE")
    @JsonIgnore
    public Date endtDate;

    @Column(name = "ATK_EXPIRED_AT_DATE")
    @JsonIgnore
    public Date expiredAtDate;

    @Column(name = "ATK_BUSINESS_KEY")
    @JsonIgnore
    public String businessKey;

    @Column(name = "ATK_CONTENT")
    @JsonIgnore
    @Lob
    public byte[] content;

    @Column(name = "ATK_VERSION")
    @JsonIgnore
    @Version
    public Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    public Set<String> tags = new HashSet<String>();

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [entityId=" + entityId + ", (" + businessKey + ")]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessInstanceEntity other = (ProcessInstanceEntity) obj;
        if (entityId == null) {
            if (other.entityId != null)
                return false;
        } else if (!entityId.equals(other.entityId))
            return false;
        return true;
    }
}
