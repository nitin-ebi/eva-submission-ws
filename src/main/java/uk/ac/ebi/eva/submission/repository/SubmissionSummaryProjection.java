package uk.ac.ebi.eva.submission.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SubmissionSummaryProjection {
    String getSubmissionId();

    String getSubmissionStatus();

    LocalDateTime getUploadedTime();

    String getAccountId();

    String getEloadSource();

    Integer getEloadId();

    String getProcessingStep();

    String getProcessingStatus();

    String getProjectTitle();

    LocalDate getReleaseDate();

    String getProjectAccession();

    String getAnalysisAccessions();

    String getRtLink();
}
