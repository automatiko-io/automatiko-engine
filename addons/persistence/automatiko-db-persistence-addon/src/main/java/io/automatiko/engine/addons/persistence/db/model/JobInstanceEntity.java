package io.automatiko.engine.addons.persistence.db.model;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "ATK_JOB_INSTANCE")
public class JobInstanceEntity extends PanacheEntityBase {

    public enum JobStatus {
        SCHEDULED,
        TAKEN,
        FAILED
    }

    @Id
    @Column(name = "ATK_JOB_ID")
    public String id;

    @Column(name = "ATK_INSTANCE_ID")
    public String ownerInstanceId;

    @Column(name = "ATK_DEFINITION_ID")
    public String ownerDefinitionId;

    @Column(name = "ATK_TRIGGER_TYPE")
    public String triggerType;

    @Column(name = "ATK_JOB_STATUS")
    public JobStatus status;

    @Column(name = "ATK_JOB_EXPIRATION")
    public LocalDateTime expirationTime;

    @Column(name = "ATK_JOB_LIMIT")
    public Integer limit;

    @Column(name = "ATK_JOB_INTERVAL")
    public Long repeatInterval;

    @Column(name = "ATK_EXPRESSION")
    public String expression;

    public JobInstanceEntity() {
    }

    public JobInstanceEntity(String id, String triggerType, String ownerDefinitionId, String ownerInstanceId,
            JobStatus status, LocalDateTime expirationTime, Integer limit, Long repeatInterval, String expression) {
        this.id = id;
        this.triggerType = triggerType;
        this.ownerDefinitionId = ownerDefinitionId;
        this.ownerInstanceId = ownerInstanceId;
        this.status = status;
        this.expirationTime = expirationTime;
        this.limit = limit;
        this.repeatInterval = repeatInterval;
        this.expression = expression;
    }

    public JobInstanceEntity(String id, String ownerDefinitionId,
            JobStatus status, LocalDateTime expirationTime, Integer limit, Long repeatInterval, String expression) {
        this.id = id;
        this.ownerDefinitionId = ownerDefinitionId;
        this.ownerInstanceId = null;
        this.status = status;
        this.expirationTime = expirationTime;
        this.limit = limit;
        this.repeatInterval = repeatInterval;
        this.expression = expression;
    }

    public static JobInstanceEntity acquireJob(String id) {
        return findById(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public static List<JobInstanceEntity> loadJobs(LocalDateTime expirationBefore) {
        return list("status = ?1 and expirationTime < ?2", JobStatus.SCHEDULED, expirationBefore);
    }
}
