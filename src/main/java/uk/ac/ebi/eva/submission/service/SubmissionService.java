package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.entity.SubmissionDetails;
import uk.ac.ebi.eva.submission.entity.SubmissionEload;
import uk.ac.ebi.eva.submission.entity.SubmissionProcessing;
import uk.ac.ebi.eva.submission.entity.SubmissionTrackingDetails;
import uk.ac.ebi.eva.submission.exception.MetadataFileInfoMismatchException;
import uk.ac.ebi.eva.submission.exception.RequiredFieldsMissingException;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStatus;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStep;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.model.SubmissionSummaryDto;
import uk.ac.ebi.eva.submission.model.SubmissionTrackingDetailsDto;
import uk.ac.ebi.eva.submission.repository.SubmissionAccountRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionDetailsRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionEloadRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionProcessingRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionTrackingDetailsRepository;
import uk.ac.ebi.eva.submission.util.BioSamplesUtils;
import uk.ac.ebi.eva.submission.util.EmailNotificationHelper;
import uk.ac.ebi.eva.submission.util.EnaUtils;
import uk.ac.ebi.eva.submission.util.MailSender;
import uk.ac.ebi.eva.submission.util.Utils;

import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.BIO_SAMPLE_ACCESSION;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.BIO_SAMPLE_OBJECT;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.DESCRIPTION;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.PROJECT;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.PROJECT_ACCESSION;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.SAMPLE;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.SCHEMA;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.TAXONOMY_ID;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.TITLE;
import static uk.ac.ebi.eva.submission.entity.SubmissionDetails.PROJECT_DESCRIPTION_LENGTH;
import static uk.ac.ebi.eva.submission.entity.SubmissionDetails.PROJECT_TITLE_LENGTH;

@Service
public class SubmissionService {
    private final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    private static final String METADATA_FILES_TAG = "files";
    private static final String METADATA_FILE_NAME = "fileName";
    private static final String METADATA_FILE_SIZE = "fileSize";
    private static final String GLOBUS_FILES_TAG = "DATA";
    private static final String GLOBUS_FILE_NAME = "name";
    private static final String GLOBUS_FILE_SIZE = "size";

    private static final Map<Pair<SubmissionProcessingStep, SubmissionProcessingStatus>, SubmissionStatus> STATUS_MAPPING =
            buildStatusMapping();

    private final SubmissionRepository submissionRepository;

    private final SubmissionAccountRepository submissionAccountRepository;

    private final SubmissionDetailsRepository submissionDetailsRepository;

    private final SubmissionProcessingRepository submissionProcessingRepository;

    private final SubmissionEloadRepository submissionEloadRepository;

    private final SubmissionTrackingDetailsRepository submissionTrackingDetailsRepository;

    private final GlobusDirectoryProvisioner globusDirectoryProvisioner;

    private final MailSender mailSender;

    @Value("${globus.uploadHttpDomain}")
    private String uploadHttpDomain;

    @Value("${eva.submission.account}")
    private String evaSubmissionAccount;

    private EmailNotificationHelper emailHelper;

    private EnaUtils enaUtils;

    private BioSamplesUtils bioSamplesUtils;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionAccountRepository submissionAccountRepository,
                             SubmissionDetailsRepository submissionDetailsRepository,
                             SubmissionProcessingRepository submissionProcessingRepository,
                             SubmissionEloadRepository submissionEloadRepository,
                             SubmissionTrackingDetailsRepository submissionTrackingDetailsRepository,
                             GlobusDirectoryProvisioner globusDirectoryProvisioner,
                             MailSender mailSender, EmailNotificationHelper emailHelper,
                             EnaUtils enaUtils, BioSamplesUtils bioSamplesUtils) {
        this.submissionRepository = submissionRepository;
        this.submissionAccountRepository = submissionAccountRepository;
        this.submissionDetailsRepository = submissionDetailsRepository;
        this.submissionProcessingRepository = submissionProcessingRepository;
        this.submissionEloadRepository = submissionEloadRepository;
        this.submissionTrackingDetailsRepository = submissionTrackingDetailsRepository;
        this.globusDirectoryProvisioner = globusDirectoryProvisioner;
        this.mailSender = mailSender;
        this.emailHelper = emailHelper;
        this.enaUtils = enaUtils;
        this.bioSamplesUtils = bioSamplesUtils;
    }

    private static Map<Pair<SubmissionProcessingStep, SubmissionProcessingStatus>, SubmissionStatus> buildStatusMapping() {
        Map<Pair<SubmissionProcessingStep, SubmissionProcessingStatus>, SubmissionStatus> statusMapping = new HashMap<>();

        // VALIDATION / READY_FOR_PROCESSING corresponds to UPLOADED initially, but on subsequent runs will correspond
        // to PROCESSING, hence the mapping here
        statusMapping.put(Pair.of(SubmissionProcessingStep.VALIDATION, SubmissionProcessingStatus.READY_FOR_PROCESSING), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.VALIDATION, SubmissionProcessingStatus.RUNNING), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.VALIDATION, SubmissionProcessingStatus.FAILURE), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.VALIDATION, SubmissionProcessingStatus.USER_FAILURE), SubmissionStatus.FAILED);
        statusMapping.put(Pair.of(SubmissionProcessingStep.VALIDATION, SubmissionProcessingStatus.SUCCESS), SubmissionStatus.PROCESSING);

        statusMapping.put(Pair.of(SubmissionProcessingStep.BROKERING, SubmissionProcessingStatus.READY_FOR_PROCESSING), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.BROKERING, SubmissionProcessingStatus.RUNNING), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.BROKERING, SubmissionProcessingStatus.FAILURE), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.BROKERING, SubmissionProcessingStatus.USER_FAILURE), SubmissionStatus.FAILED);
        statusMapping.put(Pair.of(SubmissionProcessingStep.BROKERING, SubmissionProcessingStatus.SUCCESS), SubmissionStatus.PROCESSING);

        statusMapping.put(Pair.of(SubmissionProcessingStep.INGESTION, SubmissionProcessingStatus.ON_HOLD), SubmissionStatus.ON_HOLD);
        statusMapping.put(Pair.of(SubmissionProcessingStep.INGESTION, SubmissionProcessingStatus.READY_FOR_PROCESSING), SubmissionStatus.ON_HOLD);
        statusMapping.put(Pair.of(SubmissionProcessingStep.INGESTION, SubmissionProcessingStatus.RUNNING), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.INGESTION, SubmissionProcessingStatus.FAILURE), SubmissionStatus.PROCESSING);
        statusMapping.put(Pair.of(SubmissionProcessingStep.INGESTION, SubmissionProcessingStatus.USER_FAILURE), SubmissionStatus.FAILED);
        statusMapping.put(Pair.of(SubmissionProcessingStep.INGESTION, SubmissionProcessingStatus.SUCCESS), SubmissionStatus.COMPLETED);

        return Collections.unmodifiableMap(statusMapping);
    }

    public Submission initiateSubmission(SubmissionAccount submissionAccount) {
        String submissionId = UUID.randomUUID().toString();
        String directoryToCreate = String.format("%s/%s", submissionAccount.getId(), submissionId);
        globusDirectoryProvisioner.createSubmissionDirectory(directoryToCreate);

        Optional<SubmissionAccount> optSubmissionAccount = submissionAccountRepository.findById(submissionAccount.getId());
        // if the user account is not present or if its primary email has changed, save/update the user account
        if (!optSubmissionAccount.isPresent() || !optSubmissionAccount.get().getPrimaryEmail().equals(submissionAccount.getPrimaryEmail())) {
            submissionAccountRepository.save(submissionAccount);
        }

        Submission submission = new Submission(submissionId);
        submission.setSubmissionAccount(submissionAccount);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submission.setUploadUrl(uploadHttpDomain + "/" + directoryToCreate);

        return submissionRepository.save(submission);
    }

    @Transactional
    public String getOrGenerateSubmissionIdForEload(Integer eload, String source) {
        SubmissionEload submissionEload = submissionEloadRepository.findByEload(eload);
        if (submissionEload != null) {
            return submissionEload.getSubmissionId();
        }

        String submissionId = UUID.randomUUID().toString();
        // get EVA submission account
        Optional<SubmissionAccount> optSubmissionAccount = submissionAccountRepository.findById(evaSubmissionAccount);
        // create submission with submission ID and EVA user
        Submission submission = new Submission(submissionId);
        submission.setSubmissionAccount(optSubmissionAccount.get());
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submissionRepository.save(submission);

        // Link Submission ID to Eload
        submissionEload = new SubmissionEload(submissionId, eload, source);
        submissionEloadRepository.save(submissionEload);

        return submissionId;
    }

    public void storeSubmissionIdForEload(JsonNode body) {
        String submissionId = body.path("submissionId").asText("");
        Integer eload = body.path("eload").asInt(-1);
        String source = body.path("source").asText("");
        if (submissionId.isEmpty() || source.isEmpty() || eload.equals(-1)) {
            throw new RequiredFieldsMissingException("Missing values for some of the required fields. " +
                    "submissionId: " + submissionId + ", eload: " + eload + ", source: " + source);
        }

        List<SubmissionEload> submissionEloadList = submissionEloadRepository.findBySubmissionIdOrEload(submissionId, eload);
        if (submissionEloadList.isEmpty()) {
            SubmissionEload submissionEload = new SubmissionEload(submissionId, eload, source);
            submissionEloadRepository.save(submissionEload);
        } else if (submissionEloadList.size() == 1) {
            SubmissionEload submissionEload = submissionEloadList.get(0);
            if (!submissionEload.getSubmissionId().equals(submissionId) || !submissionEload.getEload().equals(eload)) {
                throw new DataIntegrityViolationException("Can't store submissionId-eload (" + submissionId + " - " + eload + "). " +
                        "There already exists values pertaining to one of them. Existing values submissionId-eload ("
                        + submissionEload.getSubmissionId() + " - " + submissionEload.getEload() + ").");
            }
        } else {
            throw new DataIntegrityViolationException("Can't store submissionId-eload (" + submissionId + " - " + eload + "). " +
                    "There already exists values pertaining to them. " + submissionEloadList.stream()
                    .sorted(Comparator.comparing(se -> se.getEload()))
                    .map(se -> "(" + se.getSubmissionId() + " - " + se.getEload() + ")")
                    .collect(Collectors.joining(", ")));
        }
    }

    public void checkMetadataFileInfoMatchesWithUploadedFiles(SubmissionAccount submissionAccount, String submissionId, JsonNode metadataJson) {
        Map<String, Long> metadataFileInfo = new HashMap<>();
        if (metadataJson.get(METADATA_FILES_TAG) != null) {
            metadataFileInfo = StreamSupport.stream(metadataJson.get(METADATA_FILES_TAG).spliterator(), false)
                    .collect(Collectors.toMap(
                            dataNode -> dataNode.get(METADATA_FILE_NAME).asText(),
                            dataNode -> dataNode.get(METADATA_FILE_SIZE).asLong()
                    ));
        }
        if (metadataFileInfo.isEmpty()) {
            logger.error("Metadata json file for submission {} does not have any file info", submissionId);
            throw new MetadataFileInfoMismatchException("Metadata json file does not have any file info");
        }

        String directoryToList = String.format("%s/%s", submissionAccount.getId(), submissionId);
        String uploadedFilesInfo = globusDirectoryProvisioner.listSubmittedFiles(directoryToList);
        if (uploadedFilesInfo.isEmpty()) {
            logger.error("Failed to retrieve any file info from submission directory {} for submission {}", directoryToList, submissionId);
            throw new MetadataFileInfoMismatchException("Failed to retrieve any file info from submission directory.");
        } else {
            Map<String, Long> globusFileInfo = new HashMap<>();
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode globusFileInfoJson = (ObjectNode) mapper.readTree(uploadedFilesInfo);
                if (globusFileInfoJson.get(GLOBUS_FILES_TAG) != null) {
                    globusFileInfo = StreamSupport.stream(globusFileInfoJson.get(GLOBUS_FILES_TAG).spliterator(), false)
                            .collect(Collectors.toMap(
                                    dataNode -> dataNode.get(GLOBUS_FILE_NAME).asText(),
                                    dataNode -> dataNode.get(GLOBUS_FILE_SIZE).asLong()
                            ));
                }
            } catch (JsonProcessingException ex) {
                logger.error("Error parsing fileInfo from Submission Directory. Exception: {}", ex.getMessage(), ex);
                throw new MetadataFileInfoMismatchException("Error parsing fileInfo from Submission Directory");
            }

            List<String> missingFileList = new ArrayList<>();
            String fileSizeMismatchInfo = "";

            for (Map.Entry<String, Long> fileEntry : metadataFileInfo.entrySet()) {
                String fileName = Paths.get(fileEntry.getKey()).getFileName().toString();
                Long metadataFileSize = fileEntry.getValue();
                if (globusFileInfo.containsKey(fileName)) {
                    Long fileSizeInGlobus = globusFileInfo.get(fileName);
                    if (!metadataFileSize.equals(fileSizeInGlobus)) {
                        fileSizeMismatchInfo += fileName + ": metadata json file size (" + metadataFileSize + ") is not equal to uploaded file size (" + fileSizeInGlobus + ")\n";
                    }
                } else {
                    missingFileList.add(fileName);
                }
            }

            if (!missingFileList.isEmpty() || !fileSizeMismatchInfo.isEmpty()) {
                String missingFileMsg = missingFileList.isEmpty() ? "" : "There are some files mentioned in metadata json but not uploaded. Files : " + String.join(", ", missingFileList) + "\n";
                String fileSizeMismatchMsg = fileSizeMismatchInfo.isEmpty() ? "" : "There are some files mentioned in metadata json whose size does not match with the files uploaded.\n" + fileSizeMismatchInfo;
                logger.error("File Info Mismatch error in submission {}. \n {}", submissionId, missingFileMsg + fileSizeMismatchMsg);
                throw new MetadataFileInfoMismatchException(missingFileMsg + fileSizeMismatchMsg);
            }
        }
    }

    public String getVersionFromMetadataJson(JsonNode metadataJson) {
        String version = null;

        String schema = metadataJson.path(SCHEMA).asText(null);
        if (schema != null) {
            try {
                version = Utils.extractVersionFromSchemaUrl(schema);
            } catch (IllegalArgumentException ex) {
                logger.error("Version not present in schema url: {}", schema);
            }
        }

        return version;
    }

    public Map<String, String> checkAllRequiredParametersProvided(JsonNode metadataJson) {
        String projectAccession;
        String projectTitle;
        String projectDescription;
        String projectTaxonomy;

        ObjectNode projectNode = (ObjectNode) metadataJson.path(PROJECT);
        projectAccession = projectNode.path(PROJECT_ACCESSION).asText("");

        if (Strings.isEmpty(projectAccession)) {
            projectTitle = projectNode.path(TITLE).asText("");
            projectDescription = projectNode.path(DESCRIPTION).asText("");
            projectTaxonomy = projectNode.path(TAXONOMY_ID).asText("");
        } else {
            Map<String, String> projectDetails = enaUtils.getProjectDetailsFromEna(projectAccession);
            projectTitle = projectDetails.get(TITLE);
            projectDescription = projectDetails.get(DESCRIPTION);
            projectTaxonomy = projectDetails.get(TAXONOMY_ID);
        }

        // trim project title and project description to the specified length
        projectTitle = projectTitle.substring(0, Math.min(projectTitle.length(), PROJECT_TITLE_LENGTH));
        projectDescription = projectDescription.substring(0, Math.min(projectDescription.length(),
                PROJECT_DESCRIPTION_LENGTH));

        // check all required parameters are present, raise exception if anything is missing
        if (Strings.isEmpty(projectTitle) || Strings.isEmpty(projectDescription)) {
            List<String> missingParameters = new ArrayList<>();
            if (Strings.isEmpty(projectTitle)) {
                missingParameters.add("project title");
            }
            if (Strings.isEmpty(projectDescription)) {
                missingParameters.add("project description");
            }

            if (Strings.isEmpty(projectAccession)) {
                throw new RequiredFieldsMissingException("Some of the required parameters are missing from the metadata. " +
                        "Missing parameters: " + missingParameters);
            } else {
                throw new RequiredFieldsMissingException("Could not retrieve some of the required parameters from ENA " +
                        "for the project " + projectAccession + ". Missing parameters: " + missingParameters);
            }
        }

        Map<String, String> projectDetails = new HashMap<>();
        projectDetails.put(TITLE, projectTitle);
        projectDetails.put(DESCRIPTION, projectDescription);
        projectDetails.put(TAXONOMY_ID, projectTaxonomy);

        return projectDetails;
    }

    public boolean isHumanDataInSubmission(JsonNode metadataJson, String projectTaxonomy) {
        if (!Strings.isEmpty(projectTaxonomy)) {
            return "9606".equals(projectTaxonomy);
        }

        JsonNode sampleArray = metadataJson.get(SAMPLE);
        if (sampleArray == null || !sampleArray.isArray()) {
            return false;
        }

        int bioSamplesApiQueriedCount = 0;
        for (JsonNode sampleNode : sampleArray) {
            // samples defined in JSON : check all, no limit
            JsonNode bioSampleObject = sampleNode.path(BIO_SAMPLE_OBJECT);
            if (!bioSampleObject.isMissingNode()) {
                if ("9606".equals(bioSampleObject.path(TAXONOMY_ID).asText(null))) {
                    return true;
                }
                String organism = bioSampleObject.path("characteristics").path("organism")
                        .path(0).path("text").asText(null);
                if ("Homo sapiens".equalsIgnoreCase(organism)) {
                    return true;
                }
                continue;
            }

            // Pre-registered sample: query BioSamples API, limited to 5 calls
            if (bioSamplesApiQueriedCount >= 5) {
                continue;
            }
            String accession = sampleNode.path(BIO_SAMPLE_ACCESSION).asText(null);
            if (accession != null && !accession.isEmpty()) {
                if ("9606".equals(bioSamplesUtils.getTaxIdFromBioSamples(accession))) {
                    return true;
                }
                bioSamplesApiQueriedCount++;
            }
        }
        return false;
    }

    @Transactional
    public Submission uploadMetadataJsonAndMarkUploaded(String submissionId, String projectTitle,
                                                        String projectDescription, JsonNode metadataJson) {
        SubmissionDetails submissionDetails = new SubmissionDetails(submissionId);
        submissionDetails.setProjectTitle(projectTitle);
        submissionDetails.setProjectDescription(projectDescription);
        submissionDetails.setMetadataJson(metadataJson);
        submissionDetailsRepository.save(submissionDetails);

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        submission.setStatus(SubmissionStatus.UPLOADED.toString());
        submission.setUploadedTime(LocalDateTime.now());

        SubmissionProcessing submissionProc = new SubmissionProcessing(submissionId);
        submissionProc.setStep(SubmissionProcessingStep.VALIDATION.toString());
        submissionProc.setStatus(SubmissionProcessingStatus.READY_FOR_PROCESSING.toString());
        submissionProcessingRepository.save(submissionProc);

        return submissionRepository.save(submission);
    }

    public String getSubmissionStatus(String submissionId) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        if (submission == null) {
            throw new SubmissionDoesNotExistException(submissionId);
        }

        return submission.getStatus();
    }

    private void setSubmissionStatus(String submissionId,
                                     SubmissionProcessingStep step,
                                     SubmissionProcessingStatus processingStatus) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        if (submission == null) {
            throw new SubmissionDoesNotExistException(submissionId);
        }

        SubmissionStatus overallStatus = null;
        if (processingStatus == SubmissionProcessingStatus.CANCELLED) {
            overallStatus = SubmissionStatus.CANCELLED;
        } else {
            Pair<SubmissionProcessingStep, SubmissionProcessingStatus> key = Pair.of(step, processingStatus);
            if (STATUS_MAPPING.containsKey(key)) {
                overallStatus = STATUS_MAPPING.get(Pair.of(step, processingStatus));
            } else {
                logger.warn("{} - {} is not a supported combination, not updating overall status. Current status: {}",
                        step, processingStatus, submission.getStatus());
            }
        }

        if (overallStatus != null) {
            submission.setStatus(overallStatus.toString());
        }
        if (overallStatus == SubmissionStatus.COMPLETED) {
            submission.setCompletionTime(LocalDateTime.now());
        }

        submissionRepository.save(submission);
    }

    public boolean checkUserHasAccessToSubmission(SubmissionAccount account, String submissionId) {
        Optional<Submission> optSubmission = submissionRepository.findById(submissionId);
        if (optSubmission.isPresent()) {
            SubmissionAccount submissionAccount = optSubmission.get().getSubmissionAccount();
            return submissionAccount.getId().equals(account.getId());
        } else {
            throw new SubmissionDoesNotExistException(submissionId);
        }
    }

    public void sendMailNotificationToUserForStatusUpdate(SubmissionAccount submissionAccount, String submissionId,
                                                          String projectTitle, SubmissionStatus submissionStatus,
                                                          boolean needConsentStatement, boolean deprecatedVersion, boolean success) {
        String sendTo = submissionAccount.getPrimaryEmail();
        List<String> sendCC = submissionAccount.getSecondaryEmails();
        String subject = emailHelper.getSubjectForSubmissionStatusUpdate(submissionStatus, success);
        String body = emailHelper.getTextForSubmissionStatusUpdate(submissionAccount, submissionId, projectTitle,
                submissionStatus, needConsentStatement, deprecatedVersion, success);
        mailSender.sendEmail(emailHelper.getEvaHelpdeskEmail(), sendTo, sendCC, subject, body);
    }

    public void sendMailNotificationToEVAHelpdeskForSubmissionUploaded(SubmissionAccount submissionAccount,
                                                                       String submissionId, String projectTitle) {
        String subject = String.format("New Submission Uploaded. Submission Id - (%s)", submissionId);
        String body = emailHelper.getTextForEVAHelpdeskSubmissionUploaded(submissionAccount, submissionId, projectTitle);
        mailSender.sendEmail(emailHelper.getEvaHelpdeskEmail(), subject, body);
    }

    public List<Submission> getSubmissionsByStatus(SubmissionStatus status) {
        return submissionRepository.findByStatus(status.toString());
    }

    public List<SubmissionProcessing> getSubmissionsByProcessingStepAndStatus(SubmissionProcessingStep step,
                                                                              SubmissionProcessingStatus status) {
        return submissionProcessingRepository.findByStepAndStatus(step.toString(), status.toString());

    }

    @Transactional
    public SubmissionProcessing markSubmissionProcessStepAndStatus(String submissionId,
                                                                   SubmissionProcessingStep step,
                                                                   SubmissionProcessingStatus status) {
        Optional<Submission> submission = submissionRepository.findById(submissionId);
        if (!submission.isPresent()) {
            throw new SubmissionDoesNotExistException(submissionId);
        }

        SubmissionProcessing submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId);
        if (submissionProc == null) {
            submissionProc = new SubmissionProcessing(submissionId);
        }

        submissionProc.setStep(step.toString());
        submissionProc.setStatus(status.toString());
        setSubmissionStatus(submissionId, step, status);

        return submissionProcessingRepository.save(submissionProc);
    }

    public SubmissionDetails getSubmissionDetail(String submissionId) {
        return submissionDetailsRepository.findBySubmissionId(submissionId);
    }

    public Page<SubmissionSummaryDto> getSubmissionsSummary(String submissionAccount, List<SubmissionStatus> submissionStatusList,
                                                            LocalDate uploadedAfter, String source,
                                                            List<SubmissionProcessingStep> processingStepList,
                                                            List<SubmissionProcessingStatus> processingStatusList,
                                                            String submissionId, Integer eloadId, Pageable pageable) {
        List<String> statusNames = toNames(submissionStatusList);
        List<String> stepNames = toNames(processingStepList);
        List<String> processingStatusNames = toNames(processingStatusList);

        return submissionRepository.findSubmissionSummaries(
                submissionAccount,
                !statusNames.isEmpty(), statusNames,
                uploadedAfter, source,
                !stepNames.isEmpty(), stepNames,
                !processingStatusNames.isEmpty(), processingStatusNames,
                submissionId, eloadId,
                pageable
        ).map(p -> new SubmissionSummaryDto(
                p.getSubmissionId(), p.getSubmissionStatus(), p.getUploadedTime(), p.getAccountId(),
                p.getEloadSource(), p.getEloadId(),
                p.getProcessingStep(), p.getProcessingStatus(), p.getProjectTitle(),
                p.getReleaseDate(), p.getProjectAccession(), parseAnalysisAccessions(p.getAnalysisAccessions()),
                parseUri(p.getRtLink())
        ));
    }

    private static List<String> toNames(List<? extends Enum<?>> enumList) {
        if (enumList == null || enumList.isEmpty()) {
            return Collections.emptyList();
        }
        return enumList.stream().map(Enum::name).collect(Collectors.toList());
    }

    private List<String> parseAnalysisAccessions(String analysisAccessions) {
        if (analysisAccessions == null || analysisAccessions.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(analysisAccessions.split(","))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private URI parseUri(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return URI.create(value);
    }

    public SubmissionTrackingDetails updateTrackingDetails(String submissionId, SubmissionTrackingDetailsDto trackingDetails) {
        Optional<Submission> submission = submissionRepository.findById(submissionId);
        if (!submission.isPresent()) {
            throw new SubmissionDoesNotExistException(submissionId);
        }

        SubmissionTrackingDetails submissionTrackingDetails =
                submissionTrackingDetailsRepository.findBySubmissionId(submissionId);
        if (submissionTrackingDetails == null) {
            submissionTrackingDetails = new SubmissionTrackingDetails(submissionId);
        }

        if (trackingDetails.getReleaseDate() != null) {
            submissionTrackingDetails.setReleaseDate(trackingDetails.getReleaseDate());
        }
        if (trackingDetails.getProjectAccession() != null) {
            submissionTrackingDetails.setProjectAccession(trackingDetails.getProjectAccession());
        }
        if (trackingDetails.getAnalysisAccessions() != null) {
            submissionTrackingDetails.setAnalysisAccessions(trackingDetails.getAnalysisAccessions());
        }
        if (trackingDetails.getRtLink() != null) {
            submissionTrackingDetails.setRtLink(trackingDetails.getRtLink().toString());
        }

        return submissionTrackingDetailsRepository.save(submissionTrackingDetails);
    }

}
