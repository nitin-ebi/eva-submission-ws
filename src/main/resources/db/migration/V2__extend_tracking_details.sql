ALTER TABLE eva_submissions.submission_tracking_details
    ADD COLUMN project_title VARCHAR(255),
    ADD COLUMN project_taxonomies VARCHAR(255),
    ADD COLUMN metadata_json_file VARCHAR(255);

CREATE TABLE eva_submissions.submitted_analysis (
    submission_id VARCHAR(255) NOT NULL REFERENCES eva_submissions.submission(submission_id),
    analysis_alias VARCHAR(255) NOT NULL,
    analysis_accession VARCHAR(255),
    aggregation_type VARCHAR(255),
    assembly_accession VARCHAR(255),
    PRIMARY KEY (submission_id, analysis_alias)
);

CREATE TABLE eva_submissions.submitted_sample (
    submission_id VARCHAR(255) NOT NULL,
    sample_name VARCHAR(255) NOT NULL,
    sample_accession VARCHAR(255),
    PRIMARY KEY (submission_id, sample_name),
    CONSTRAINT fk_submitted_sample_submission FOREIGN KEY (submission_id)
        REFERENCES eva_submissions.submission(submission_id)
);

CREATE TABLE eva_submissions.submitted_vcf (
    submission_id VARCHAR(255) NOT NULL,
    analysis_alias VARCHAR(255) NOT NULL,
    submitted_file VARCHAR(255) NOT NULL,
    brokered_file VARCHAR(255),
    csi_file VARCHAR(255),
    brokered_file_md5 VARCHAR(255),
    csi_file_md5 VARCHAR(255),
    PRIMARY KEY (submission_id, analysis_alias, submitted_file),
    CONSTRAINT fk_submitted_vcf_analysis FOREIGN KEY (submission_id, analysis_alias)
        REFERENCES eva_submissions.submitted_analysis(submission_id, analysis_alias)
);

CREATE TABLE eva_submissions.submission_submitted_analysis (
    submission_id VARCHAR(255) NOT NULL,
    analysis_alias VARCHAR(255) NOT NULL,
    PRIMARY KEY (submission_id, analysis_alias),
    CONSTRAINT fk_submission_submitted_analysis_submission FOREIGN KEY (submission_id)
        REFERENCES eva_submissions.submission_tracking_details(submission_id),
    CONSTRAINT fk_submission_submitted_analysis_analysis FOREIGN KEY (submission_id, analysis_alias)
        REFERENCES eva_submissions.submitted_analysis(submission_id, analysis_alias)
);

CREATE TABLE eva_submissions.submitted_analysis_submitted_sample (
    analysis_submission_id VARCHAR(255) NOT NULL,
    analysis_alias VARCHAR(255) NOT NULL,
    sample_submission_id VARCHAR(255) NOT NULL,
    sample_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (analysis_submission_id, analysis_alias, sample_submission_id, sample_name),
    CONSTRAINT fk_analysis_sample_analysis FOREIGN KEY (analysis_submission_id, analysis_alias)
        REFERENCES eva_submissions.submitted_analysis(submission_id, analysis_alias),
    CONSTRAINT fk_analysis_sample_sample FOREIGN KEY (sample_submission_id, sample_name)
        REFERENCES eva_submissions.submitted_sample(submission_id, sample_name),
    CONSTRAINT chk_analysis_sample_submission_match
        CHECK (analysis_submission_id = sample_submission_id)
);

CREATE TABLE eva_submissions.submitted_analysis_submitted_vcf (
    submission_id VARCHAR(255) NOT NULL,
    analysis_alias VARCHAR(255) NOT NULL,
    submitted_file VARCHAR(255) NOT NULL,
    PRIMARY KEY (submission_id, analysis_alias, submitted_file),
    CONSTRAINT fk_analysis_vcf_analysis FOREIGN KEY (submission_id, analysis_alias)
        REFERENCES eva_submissions.submitted_analysis(submission_id, analysis_alias),
    CONSTRAINT fk_analysis_vcf_vcf FOREIGN KEY (submission_id, analysis_alias, submitted_file)
        REFERENCES eva_submissions.submitted_vcf(submission_id, analysis_alias, submitted_file)
);

INSERT INTO eva_submissions.submitted_analysis (submission_id, analysis_alias, analysis_accession)
SELECT submission_id, analysis_accessions, analysis_accessions
FROM eva_submissions.submission_tracking_details_analysis_accessions
WHERE analysis_accessions IS NOT NULL
ON CONFLICT (submission_id, analysis_alias) DO NOTHING;

INSERT INTO eva_submissions.submission_submitted_analysis (submission_id, analysis_alias)
SELECT submission_id, analysis_accessions
FROM eva_submissions.submission_tracking_details_analysis_accessions
WHERE analysis_accessions IS NOT NULL
ON CONFLICT (submission_id, analysis_alias) DO NOTHING;


DROP TABLE eva_submissions.submitted_analysis_submitted_vcf;
DROP TABLE eva_submissions.submission_submitted_analysis;
