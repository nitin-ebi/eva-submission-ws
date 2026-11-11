package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.util.List;
import java.util.Objects;

@Entity
@IdClass(SubmittedAnalysis.SubmittedAnalysisId.class)
@Table(schema = "eva_submissions", name = "submitted_analysis")
public class SubmittedAnalysis {

    public SubmittedAnalysis() {
    }

    public SubmittedAnalysis(String submissionId, String analysisAlias) {
        this.submissionId = submissionId;
        this.analysisAlias = analysisAlias;
    }

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @Id
    @Column(name = "analysis_alias")
    private String analysisAlias;

    @Column(name = "analysis_accession")
    private String analysisAccession;

    @Column(name = "aggregation_type")
    private String aggregationType;

    @Column(name = "assembly_accession")
    private String assemblyAccession;

    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinColumns({
            @JoinColumn(name = "submission_id", referencedColumnName = "submission_id",
                    insertable = false, updatable = false),
            @JoinColumn(name = "analysis_alias", referencedColumnName = "analysis_alias",
                    insertable = false, updatable = false)
    })
    private List<SubmittedVcf> vcfs;

    @ManyToMany
    @JoinTable(name = "submitted_analysis_submitted_sample",
            joinColumns = {
                    @JoinColumn(name = "analysis_submission_id", referencedColumnName = "submission_id"),
                    @JoinColumn(name = "analysis_alias", referencedColumnName = "analysis_alias")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "sample_submission_id", referencedColumnName = "submission_id"),
                    @JoinColumn(name = "sample_name", referencedColumnName = "sample_name")
            })
    private List<SubmittedSample> samples;

    public static final class SubmittedAnalysisId {

        private String submissionId;

        private String analysisAlias;

        public SubmittedAnalysisId() {
        }

        public SubmittedAnalysisId(String submissionId, String analysisAlias) {
            this.submissionId = submissionId;
            this.analysisAlias = analysisAlias;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            SubmittedAnalysisId that = (SubmittedAnalysisId) object;
            return Objects.equals(submissionId, that.submissionId)
                    && Objects.equals(analysisAlias, that.analysisAlias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(submissionId, analysisAlias);
        }
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getAnalysisAlias() {
        return analysisAlias;
    }

    public String getAnalysisAccession() {
        return analysisAccession;
    }

    public void setAnalysisAccession(String analysisAccession) {
        this.analysisAccession = analysisAccession;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getAssemblyAccession() {
        return assemblyAccession;
    }

    public void setAssemblyAccession(String assemblyAccession) {
        this.assemblyAccession = assemblyAccession;
    }

    public List<SubmittedVcf> getVcfs() {
        return vcfs;
    }

    public void setVcfs(List<SubmittedVcf> vcfs) {
        this.vcfs = vcfs;
    }

    public List<SubmittedSample> getSamples() {
        return samples;
    }

    public void setSamples(List<SubmittedSample> samples) {
        this.samples = samples;
    }
}
