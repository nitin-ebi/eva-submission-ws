package uk.ac.ebi.eva.submission.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.controller.BaseController;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionDetails;
import uk.ac.ebi.eva.submission.entity.SubmissionProcessing;
import uk.ac.ebi.eva.submission.exception.RequiredFieldsMissingException;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStatus;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStep;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.model.SubmissionSummaryDto;
import uk.ac.ebi.eva.submission.model.SubmissionTrackingDetailsDto;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.SubmissionService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/v1/admin")
public class AdminController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final SubmissionService submissionService;

    public AdminController(SubmissionService submissionService, WebinTokenService webinTokenService,
                           LsriTokenService lsriTokenService) {
        super(webinTokenService, lsriTokenService);
        this.submissionService = submissionService;
    }

    @Operation(summary = "Given a submission id, this endpoint updates the status of the submission to the one provided",
            security = {@SecurityRequirement(name = "basicAuth")
            })
    @Parameters({
            @Parameter(name = "submissionId", description = "Id of the submission whose status needs to be updated",
                    required = true, in = ParameterIn.PATH),
            @Parameter(name = "status", description = "Desired status of the submission ",
                    required = true, in = ParameterIn.PATH)
    })
    @PutMapping("submission/{submissionId}/status/{status}")
    public ResponseEntity<?> markSubmissionStatus(@PathVariable("submissionId") String submissionId,
                                                  @PathVariable("status") SubmissionStatus status) {
        return new ResponseEntity<>("Cannot set submission status, use processing status instead", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Operation(summary = "This endpoint retrieves detail of submission including the metadata json",
            security = {@SecurityRequirement(name = "basicAuth")})
    @Parameters({
            @Parameter(name = "submissionId", description = "Id of the submission to fetch",
                    required = true, in = ParameterIn.PATH)
    })
    @GetMapping("submission/{submissionId}")
    public ResponseEntity<?> getSubmissionDetails(
            @PathVariable("submissionId") String submissionId) {
        try {
            SubmissionDetails submissionDetail = submissionService.getSubmissionDetail(submissionId);
            return new ResponseEntity<>(submissionDetail, HttpStatus.OK);
        } catch (SubmissionDoesNotExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "This endpoint retrieves all the submissions of a specific status present in the database",
            security = {@SecurityRequirement(name = "basicAuth")})
    @Parameters({
            @Parameter(name = "status", description = "Desired status of the submission ",
                    required = true, in = ParameterIn.PATH)
    })
    @GetMapping("submissions/status/{status}")
    public ResponseEntity<?> getSubmissionsbyStatus(@PathVariable("status") SubmissionStatus status) {
        List<Submission> submissions = submissionService.getSubmissionsByStatus(status);
        return new ResponseEntity<>(stripUserDetails(submissions), HttpStatus.OK);
    }

    @Operation(summary = "Given a submission id, this endpoint updates the processing status of the submission to the one provided",
            security = {@SecurityRequirement(name = "basicAuth")
            })
    @Parameters({
            @Parameter(name = "submissionId", description = "Id of the submission whose status needs to be updated",
                    required = true, in = ParameterIn.PATH),
            @Parameter(name = "step", description = "The processing step of the submission",
                    required = true, in = ParameterIn.PATH),
            @Parameter(name = "status", description = "The status of the processing step for this submission",
                    required = true, in = ParameterIn.PATH)
    })
    @PutMapping("submission-process/{submissionId}/{step}/{status}")
    public ResponseEntity<?> markSubmissionProcessStepAndStatus(@PathVariable("submissionId") String submissionId,
                                                                @PathVariable("step") SubmissionProcessingStep step,
                                                                @PathVariable("status") SubmissionProcessingStatus status) {
        try {
            SubmissionProcessing submissionProc = this.submissionService.markSubmissionProcessStepAndStatus(submissionId, step, status);
            return new ResponseEntity<>(submissionProc, HttpStatus.OK);
        } catch (SubmissionDoesNotExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "This endpoint retrieves all the submissions from the database with given step and status",
            security = {@SecurityRequirement(name = "basicAuth")})
    @Parameters({
            @Parameter(name = "step", description = "The processing step of the submission.",
                    required = true, in = ParameterIn.PATH),
            @Parameter(name = "status", description = "The status of the submission processing step.",
                    required = true, in = ParameterIn.PATH)
    })
    @GetMapping("submission-processes/{step}/{status}")
    public ResponseEntity<?> getSubmissionsProcessingByStepAndStatus(
            @PathVariable("step") SubmissionProcessingStep step,
            @PathVariable("status") SubmissionProcessingStatus status) {
        List<SubmissionProcessing> submissions = submissionService.getSubmissionsByProcessingStepAndStatus(step, status);
        return new ResponseEntity<>(submissions, HttpStatus.OK);
    }

    @Operation(summary = "This endpoint retrieves the submission id associated with an eload. " +
            "If there is no submission id for the eload, it will create one, link it to the eload and then retrieve the same",
            security = {@SecurityRequirement(name = "basicAuth")})
    @Parameters({@Parameter(name = "eload", description = "The eload for which we want to get the submission ID",
            required = true, in = ParameterIn.PATH),
            @Parameter(name = "source", description = "The source associated with the submission",
                    required = true, in = ParameterIn.QUERY)
    })
    @GetMapping("submission/{eload}/submissionId")
    public ResponseEntity<?> getSubmissionIdForEload(@PathVariable("eload") Integer eload,
                                                     @RequestParam(value = "source") String source) {
        String submissionId = submissionService.getOrGenerateSubmissionIdForEload(eload, source);
        return new ResponseEntity<>(Collections.singletonMap("submissionId", submissionId), HttpStatus.OK);
    }

    @Operation(summary = "This endpoint lists submissions with optional filters and return information about the submission, account, eload and processing step",
            security = {@SecurityRequirement(name = "basicAuth")})
    @Parameters({
            @Parameter(name = "submissionAccount", description = "Filter by submission account ID", in = ParameterIn.QUERY),
            @Parameter(name = "submissionStatus", description = "Filter by submission status (OPEN, UPLOADED, COMPLETED, TIMEOUT, FAILED, CANCELLED, PROCESSING)", in = ParameterIn.QUERY),
            @Parameter(name = "uploadedAfter", description = "Filter submissions with uploadedTime >= this date (ISO-8601)", in = ParameterIn.QUERY),
            @Parameter(name = "source", description = "Filter by submission source (email or eva-sub-cli)", in = ParameterIn.QUERY),
            @Parameter(name = "processingStep", description = "Filter by processing step (INGESTION, VALIDATION, BROKERING)", in = ParameterIn.QUERY),
            @Parameter(name = "processingStatus", description = "Filter by processing status (READY_FOR_PROCESSING, FAILURE, SUCCESS, RUNNING, ON_HOLD)", in = ParameterIn.QUERY),
            @Parameter(name = "submissionId", description = "Filter by submission ID", in = ParameterIn.QUERY),
            @Parameter(name = "eloadId", description = "Filter by ELOAD number", in = ParameterIn.QUERY)
    })
    @GetMapping("submissions")
    public ResponseEntity<?> getSubmissions(
            @RequestParam(required = false) String submissionAccount,
            @RequestParam(value = "submissionStatus", required = false) List<SubmissionStatus> submissionStatusList,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedAfter,
            @RequestParam(required = false) String source,
            @RequestParam(value = "processingStep", required = false) List<SubmissionProcessingStep> processingStepList,
            @RequestParam(value = "processingStatus", required = false) List<SubmissionProcessingStatus> processingStatusList,
            @RequestParam(required = false) String submissionId,
            @RequestParam(required = false) Integer eloadId,
            @PageableDefault(size = 1000, sort = "submissionId") Pageable pageable) {
        Page<SubmissionSummaryDto> result = submissionService.getSubmissionsSummary(
                submissionAccount, submissionStatusList, uploadedAfter, source, processingStepList, processingStatusList,
                submissionId, eloadId, pageable);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Operation(summary = "This endpoint stores the eload and the associated submission id.",
            security = {@SecurityRequirement(name = "basicAuth")})
    @PutMapping("submission/eload/submissionId")
    public ResponseEntity<?> storeSubmissionIdForEload(@RequestBody JsonNode body) {
        try {
            submissionService.storeSubmissionIdForEload(body);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RequiredFieldsMissingException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @Operation(summary = "Given a submission id, this endpoint updates the tracking details (release date, etc.)",
            security = {@SecurityRequirement(name = "basicAuth")
            })
    @Parameters({
            @Parameter(name = "submissionId", description = "Id of the submission to update",
                    required = true, in = ParameterIn.PATH)
    })
    @PutMapping("submission/{submissionId}/trackingDetails")
    public ResponseEntity<?> setTrackingDetails(
            @PathVariable("submissionId") String submissionId,
            @RequestBody SubmissionTrackingDetailsDto trackingDetails
    ) {
        try {
            return new ResponseEntity<>(submissionService.updateTrackingDetails(submissionId, trackingDetails),
                    HttpStatus.OK);
        } catch (SubmissionDoesNotExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "This endpoint marks the initiation of a submission submitted to EVA through FTP")
    @PostMapping("submission/initiate")
    public ResponseEntity<?> initiateSubmission() {
        Submission submission = this.submissionService.initiateSubmissionByEVA();
        logger.info("Admin Initiate Submission generated submission Id {}", submission.getSubmissionId());
        return new ResponseEntity<>(stripUserDetails(submission), HttpStatus.OK);
    }

}
