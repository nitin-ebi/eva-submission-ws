package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.lang.NonNull;

import java.util.Objects;


@Entity
@Table(schema = "eva_submissions", name = "submission_eload")
public class SubmissionEload {

    @Id
    @NonNull
    @Column(nullable = false, name = "submission_id")
    private String submissionId;
    @NonNull
    @Column(nullable = false, unique = true)
    private Integer eload;
    @NonNull
    @Column(nullable = false)
    private String source;

    public SubmissionEload() {

    }

    public SubmissionEload(String submissionId, Integer eload, String source) {
        this.submissionId = submissionId;
        this.eload = eload;
        this.source = source;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(@NonNull String submissionId) {
        this.submissionId = submissionId;
    }

    @NonNull
    public Integer getEload() {
        return eload;
    }

    public void setEload(@NonNull Integer eload) {
        this.eload = eload;
    }

    @NonNull
    public String getSource() {
        return source;
    }

    public void setSource(@NonNull String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionEload)) return false;
        SubmissionEload that = (SubmissionEload) o;
        return getSubmissionId().equals(that.getSubmissionId()) && getEload().equals(that.getEload());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubmissionId() + getEload());
    }

    @Override
    public String toString() {
        return "SubmissionEload{" +
                "submissionId='" + submissionId + '\'' +
                ", eload=" + eload +
                ", source='" + source + '\'' +
                '}';
    }
}