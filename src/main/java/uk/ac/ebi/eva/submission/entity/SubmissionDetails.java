package uk.ac.ebi.eva.submission.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "eva_submissions", name = "submission_details")
public class SubmissionDetails {
    public static final int PROJECT_TITLE_LENGTH = 500;
    public static final int PROJECT_DESCRIPTION_LENGTH = 5000;

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @OneToOne
    @PrimaryKeyJoinColumn(name = "submission_id", referencedColumnName = "submission_id")
    private Submission submission;

    @Column(nullable = false, name = "project_title", length = PROJECT_TITLE_LENGTH)
    private String projectTitle;

    @Column(nullable = false, name = "project_description", length = PROJECT_DESCRIPTION_LENGTH)
    private String projectDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "metadata_json", nullable = false)
    private JsonNode metadataJson;

    public SubmissionDetails() {
    }

    public SubmissionDetails(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public JsonNode getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(JsonNode metadataJson) {
        this.metadataJson = metadataJson;
    }
}
