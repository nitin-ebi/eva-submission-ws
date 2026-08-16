package uk.ac.ebi.eva.submission.controller.submissionws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.controller.BaseController;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.exception.MetadataFileInfoMismatchException;
import uk.ac.ebi.eva.submission.exception.RequiredFieldsMissingException;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.exception.UnsupportedVersionException;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.SubmissionService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;
import uk.ac.ebi.eva.submission.util.Utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/v1")
public class SubmissionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    public static final String PROJECT = "project";
    public static final String PROJECT_ACCESSION = "projectAccession";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String TAXONOMY_ID = "taxId";
    public static final String ANALYSIS = "analysis";
    public static final String SCHEMA = "$schema";
    public static final String SAMPLE = "sample";
    public static final String BIO_SAMPLE_ACCESSION = "bioSampleAccession";
    public static final String BIO_SAMPLE_OBJECT = "bioSampleObject";

    public static final String DEPRECATED_VERSION = "v0.5.0";
    public static final String UNSUPPORTED_VERSION = "v0.4.13";

    private final SubmissionService submissionService;
    private final WebinTokenService webinTokenService;
    private final LsriTokenService lsriTokenService;

    public SubmissionController(SubmissionService submissionService, WebinTokenService webinTokenService,
                                LsriTokenService lsriTokenService) {
        super(webinTokenService, lsriTokenService);
        this.submissionService = submissionService;
        this.webinTokenService = webinTokenService;
        this.lsriTokenService = lsriTokenService;
    }

    @Operation(summary = "This endpoint authenticates a user with LSRI")
    @Parameters({
            @Parameter(name = "deviceCode", description = "device code for authentication",
                    required = true, in = ParameterIn.QUERY),
            @Parameter(name = "expiresIn", description = "device code expiration time in seconds",
                    required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("submission/auth/lsri")
    public ResponseEntity<?> authenticateLSRI(@RequestParam("deviceCode") String deviceCode,
                                              @RequestParam("expiresIn") int codeExpirationTimeInSeconds) {
        String token = lsriTokenService.pollForToken(deviceCode, codeExpirationTimeInSeconds);
        if (Objects.nonNull(token)) {
            return new ResponseEntity<>(token, HttpStatus.OK);
        }
        return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    @Operation(summary = "This endpoint marks the initiation of a submission. It will do the necessary prep work " +
            "for receiving the submission files")
    @Parameters({
            @Parameter(name = "Authorization", description = "Token (bearerToken) for authenticating the user",
                    required = true, in = ParameterIn.HEADER)
    })
    @PostMapping("submission/initiate")
    public ResponseEntity<?> initiateSubmission(@RequestHeader("Authorization") String bearerToken) {
        logger.info("Initiate submission endpoint called");
        SubmissionAccount submissionAccount = this.getSubmissionAccount(bearerToken);
        if (Objects.isNull(submissionAccount)) {
            logger.warn("Initiate submission failed: authentication unsuccessful");
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        logger.info("Initiate submission: authenticated user {}", submissionAccount.getId());
        Submission submission = this.submissionService.initiateSubmission(submissionAccount);
        logger.info("Initiate submission: submission Id {}", submission.getSubmissionId());
        return new ResponseEntity<>(stripUserDetails(submission), HttpStatus.OK);
    }

    @Operation(summary = "Given a submission id, this endpoint will mark the submission as uploaded")
    @Parameters({
            @Parameter(name = "Authorization", description = "Token (bearerToken) for authenticating the user",
                    required = true, in = ParameterIn.HEADER),
            @Parameter(name = "submissionId", description = "Id of the submission whose status needs to be retrieved",
                    required = true, in = ParameterIn.PATH)
    })
    @PutMapping("submission/{submissionId}/uploaded")
    public ResponseEntity<?> markSubmissionUploaded(@RequestHeader("Authorization") String bearerToken,
                                                    @PathVariable("submissionId") String submissionId,
                                                    @RequestBody JsonNode metadataJson) {
        logger.info("Mark submission uploaded endpoint called for submissionId: {}", submissionId);
        SubmissionAccount submissionAccount = this.getSubmissionAccount(bearerToken);
        if (Objects.isNull(submissionAccount)) {
            logger.warn("Mark submission uploaded failed for submissionId {}: authentication unsuccessful", submissionId);
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        if (!submissionService.checkUserHasAccessToSubmission(submissionAccount, submissionId)) {
            logger.warn("Mark submission uploaded failed for submissionId {}: user {} does not have access to this submission",
                    submissionId, submissionAccount.getId());
            return new ResponseEntity<>("Unauthorized: Account " + submissionAccount.getId() +
                    " does not have access to submissionId " + submissionId, HttpStatus.UNAUTHORIZED);
        }
        logger.info("Mark submission uploaded: authenticated user {} for submissionId {}", submissionAccount.getId(), submissionId);

        String submissionStatus = submissionService.getSubmissionStatus(submissionId);
        if (!Objects.equals(submissionStatus, SubmissionStatus.OPEN.toString())) {
            return new ResponseEntity<>(
                    "Submission " + submissionId + " is not in status " + SubmissionStatus.OPEN +
                            ". It cannot be marked as " + SubmissionStatus.UPLOADED +
                            ". Current Status: " + submissionStatus,
                    HttpStatus.BAD_REQUEST);
        }
        try {
            // check if the User is using a deprecated version of eva-sub-cli
            boolean deprecatedVersion = true;
            String version = submissionService.getVersionFromMetadataJson(metadataJson);
            if (version != null && Utils.compareVersions(version, DEPRECATED_VERSION) > 0) {
                deprecatedVersion = false;
            } else {
                if (version == null || version.isEmpty()) {
                    version = "< " + UNSUPPORTED_VERSION;
                }
                throw new UnsupportedVersionException(version);
            }
            // check if there is a difference between the files uploaded and files mentioned in metadata
            submissionService.checkMetadataFileInfoMatchesWithUploadedFiles(submissionAccount, submissionId, metadataJson);

            // check if all the required parameters are present/provided
            Map<String, String> projectDetails = submissionService.checkAllRequiredParametersProvided(metadataJson);

            String projectTitle = projectDetails.get(TITLE);
            String projectDescription = projectDetails.get(DESCRIPTION);
            String projectTaxonomy = projectDetails.get(TAXONOMY_ID);

            // save submission details along with metadata
            Submission submission = this.submissionService.uploadMetadataJsonAndMarkUploaded(submissionId,
                    projectTitle, projectDescription, metadataJson);

            // check if consent statement is required
            ArrayNode analysisNode = (ArrayNode) metadataJson.get(ANALYSIS);
            boolean isHuman = submissionService.isHumanDataInSubmission(metadataJson, projectTaxonomy);
            boolean needConsentStatement = checkConsentStatementIsNeededForTheSubmission(isHuman, analysisNode);

            // send notification to user
            submissionService.sendMailNotificationToUserForStatusUpdate(submissionAccount, submissionId, projectTitle,
                    SubmissionStatus.UPLOADED, needConsentStatement, deprecatedVersion, true);
            // send notification to EVA HelpDesk
            submissionService.sendMailNotificationToEVAHelpdeskForSubmissionUploaded(submissionAccount, submissionId,
                    projectTitle);

            return new ResponseEntity<>(stripUserDetails(submission), HttpStatus.OK);
        } catch (RequiredFieldsMissingException | MetadataFileInfoMismatchException | UnsupportedVersionException ex) {
            logger.error("Error occurred while processing the submission.", ex);
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error occurred while processing the submission.", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Given a submission id, this endpoint retrieves the current status of the submission")
    @Parameters({
            @Parameter(name = "submissionId", description = "Id of the submission whose status needs to be retrieved",
                    required = true, in = ParameterIn.PATH)
    })
    @GetMapping("submission/{submissionId}/status")
    public ResponseEntity<?> getSubmissionStatus(@PathVariable("submissionId") String submissionId) {
        try {
            String submissionStatus = submissionService.getSubmissionStatus(submissionId);
            return new ResponseEntity<>(submissionStatus, HttpStatus.OK);
        } catch (SubmissionDoesNotExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    private boolean checkConsentStatementIsNeededForTheSubmission(boolean isHuman, ArrayNode analysisNode) {
        if (!isHuman) {
            return false;
        }
        Set<String> evidenceTypes = new HashSet<>();
        for (JsonNode node : analysisNode) {
            String evidenceType = node.path("evidenceType").asText(null);
            if (evidenceType != null) {
                evidenceTypes.add(evidenceType);
            }
        }
        return evidenceTypes.isEmpty() || evidenceTypes.contains("genotype");
    }
}
