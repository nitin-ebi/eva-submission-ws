package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import uk.ac.ebi.eva.submission.util.EnaDownloader;
import uk.ac.ebi.eva.submission.util.EnaUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.DESCRIPTION;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.TAXONOMY_ID;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.TITLE;

@SpringBootTest(classes = {EnaUtils.class, EnaDownloader.class})
@EnableRetry
public class EnaUtilsAndEnaDownloaderTest {
    private static final String projectAccession = "PRJEB12345";

    @Autowired
    private EnaUtils enaUtils;

    @Autowired
    private EnaDownloader enaDownloader;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void testGetProjectDetailsFromEna_withValidXml_shouldReturnParsedValues() {
        String xmlResponse = "<PROJECT_SET> " +
                "<PROJECT> " +
                "<TITLE>Test Project</TITLE> " +
                "<DESCRIPTION>Test Description</DESCRIPTION> " +
                "<SUBMISSION_PROJECT> " +
                "<ORGANISM> " +
                "<TAXON_ID>9606</TAXON_ID> " +
                "</ORGANISM> " +
                "</SUBMISSION_PROJECT> " +
                "</PROJECT> " +
                "</PROJECT_SET> ";

        when(restTemplate.getForObject(ArgumentMatchers.contains(projectAccession), eq(String.class)))
                .thenReturn(xmlResponse);

        Map<String, String> projectDetails = enaUtils.getProjectDetailsFromEna(projectAccession);
        assertEquals("Test Project", projectDetails.get(TITLE));
        assertEquals("Test Description", projectDetails.get(DESCRIPTION));
        assertEquals("9606", projectDetails.get(TAXONOMY_ID));
    }

    @Test
    void testGetProjectDetailsFromEna_withMissingNodes_shouldReturnEmptyStrings() {
        String xmlResponse = "<PROJECT_SET><PROJECT></PROJECT></PROJECT_SET>";

        when(restTemplate.getForObject(ArgumentMatchers.contains(projectAccession), eq(String.class)))
                .thenReturn(xmlResponse);

        Map<String, String> projectDetails = enaUtils.getProjectDetailsFromEna(projectAccession);
        assertEquals("", projectDetails.get(TITLE));
        assertEquals("", projectDetails.get(DESCRIPTION));
        assertEquals("", projectDetails.get(TAXONOMY_ID));
    }

    @Test
    void testGetProjectDetailsFromEna_withRestTemplateException_shouldReturnEmptyMap() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("API down"));

        Map<String, String> projectDetails = enaUtils.getProjectDetailsFromEna(projectAccession);

        assertEquals("", projectDetails.get(TITLE));
        assertEquals("", projectDetails.get(DESCRIPTION));
        assertEquals("", projectDetails.get(TAXONOMY_ID));
    }

    @Test
    void testDownloadXmlFromEnaRetriesOnFailure() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("ENA service down"));

        assertThrows(Exception.class, () -> enaDownloader.downloadXmlFromEna(projectAccession));

        verify(restTemplate, times(5)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void testDownloadXmlFromEnaSucceedsAfterRetries() throws Exception {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Temporary error"))
                .thenThrow(new RuntimeException("Temporary error again"))
                .thenReturn("<PROJECT_SET><PROJECT><TITLE>Test Project</TITLE></PROJECT></PROJECT_SET>");

        Document doc = enaDownloader.downloadXmlFromEna(projectAccession);

        String title = doc.getElementsByTagName("TITLE").item(0).getTextContent();
        assertEquals("Test Project", title);

        verify(restTemplate, times(3)).getForObject(anyString(), eq(String.class));
    }
}