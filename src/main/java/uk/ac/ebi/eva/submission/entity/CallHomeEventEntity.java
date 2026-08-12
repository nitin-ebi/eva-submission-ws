package uk.ac.ebi.eva.submission.entity;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(schema = "eva_submissions", name = "call_home_event")
public class CallHomeEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id")
    private String deploymentId;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "cli_version")
    private String cliVersion;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "runtime_seconds")
    private Integer runtimeSeconds;

    @Column(name = "executor")
    private String executor;

    @Column(name = "tasks")
    private String tasks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode rawPayload;

    public CallHomeEventEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCliVersion() {
        return cliVersion;
    }

    public void setCliVersion(String cliVersion) {
        this.cliVersion = cliVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRuntimeSeconds() {
        return runtimeSeconds;
    }

    public void setRuntimeSeconds(Integer runtimeSeconds) {
        this.runtimeSeconds = runtimeSeconds;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public JsonNode getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(JsonNode rawPayload) {
        this.rawPayload = rawPayload;
    }
}

