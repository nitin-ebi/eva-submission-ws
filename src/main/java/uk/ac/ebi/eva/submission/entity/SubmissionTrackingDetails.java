package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(schema = "eva_submissions", name = "submission_tracking_details")
public class SubmissionTrackingDetails {

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @OneToOne
    @PrimaryKeyJoinColumn(name = "submission_id", referencedColumnName = "submission_id")
    private Submission submission;

    @Column(name = "release_date", nullable = true)
    private LocalDate releaseDate;

    @Column(name = "project_accession", nullable = true)
    private String projectAccession;

    @ElementCollection
    @CollectionTable(joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "analysis_accessions", nullable = true)
    private List<String> analysisAccessions;

    @Column(name = "rt_link", nullable = true)
    private String rtLink;

    public SubmissionTrackingDetails() {
    }

    public SubmissionTrackingDetails(String submissionId) {
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

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public List<String> getAnalysisAccessions() {
        return analysisAccessions;
    }

    public void setAnalysisAccessions(List<String> analysisAccessions) {
        this.analysisAccessions = analysisAccessions;
    }

    public String getProjectAccession() {
        return projectAccession;
    }

    public void setProjectAccession(String projectAccession) {
        this.projectAccession = projectAccession;
    }

    public String getRtLink() {
        return rtLink;
    }

    public void setRtLink(String rtLink) {
        this.rtLink = rtLink;
    }
}
