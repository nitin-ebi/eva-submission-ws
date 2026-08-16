package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(schema = "eva_submissions", name = "submission")
public class Submission {

    public Submission() {

    }

    public Submission(String submissionId) {
        this.submissionId = submissionId;
    }

    @Id
    @NonNull
    @Column(nullable = false, name = "submission_id")
    private String submissionId;

    @NonNull
    @Column(nullable = false)
    private String status;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "submission_account_id", nullable = false, referencedColumnName = "id")
    private SubmissionAccount submissionAccount;

    @Column(nullable = false)
    private LocalDateTime initiationTime;

    @Column
    private LocalDateTime uploadedTime;

    @Column
    private LocalDateTime completionTime;

    @Column
    private String uploadUrl;

    public String getSubmissionId() {
        return submissionId;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    @NonNull
    public SubmissionAccount getSubmissionAccount() {
        return submissionAccount;
    }

    public void setSubmissionAccount(@NonNull SubmissionAccount submissionAccount) {
        this.submissionAccount = submissionAccount;
    }

    public LocalDateTime getInitiationTime() {
        return initiationTime;
    }

    public void setInitiationTime(LocalDateTime initiationTime) {
        this.initiationTime = initiationTime;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Submission)) return false;
        Submission that = (Submission) o;
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
                ", status=" + status +
                ", initiationTime=" + initiationTime +
                ", uploadedTime=" + uploadedTime +
                ", completionTime=" + completionTime +
                '}';
    }
}
