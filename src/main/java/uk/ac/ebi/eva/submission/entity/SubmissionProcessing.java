package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Audited(targetAuditMode = NOT_AUDITED)
@Table(schema = "eva_submissions", name = "submission_processing_status")
public class SubmissionProcessing {

    @Id
    @NonNull
    @Column(nullable = false, name = "submission_id")
    private String submissionId;
    
    @OneToOne
    @JoinColumn(name = "submission_id", referencedColumnName = "submission_id", insertable = false, updatable = false)
    private Submission submission;

    @NonNull
    @Column(nullable = false)
    private String step;

    @NonNull
    @Column(nullable = false)
    private String status;

    @NonNull
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 5")
    private Integer priority = 5;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastUpdateTime;

    public SubmissionProcessing() {

    }

    public SubmissionProcessing(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    @NonNull
    public String getStep() {
        return step;
    }

    public void setStep(@NonNull String step) {
        this.step = step;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    @NonNull
    public Integer getPriority() {
        return priority;
    }

    public void setPriority(@NonNull Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionProcessing)) return false;
        SubmissionProcessing that = (SubmissionProcessing) o;
        return getSubmissionId().equals(that.getSubmissionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubmissionId());
    }

    @Override
    public String toString() {
        return "Submission{" +
                "submissionId='" + submissionId + '\'' +
                ", step=" + step +
                ", status=" + status +
                ", priority=" + priority +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}
