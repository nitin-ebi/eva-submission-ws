package uk.ac.ebi.eva.submission.util;


import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Pattern;

@Component
public class EnaDownloader {
    private static final String ENA_BASE_URL = "https://www.ebi.ac.uk/ena/browser/api/xml/";
    private static final Pattern ENA_ACCESSION = Pattern.compile("^[A-Z]{1,6}[0-9]{1,9}$");
    private final RestTemplate restTemplate;

    public EnaDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public Document downloadXmlFromEna(String projectAccession) throws Exception {
        if (!ENA_ACCESSION.matcher(projectAccession).matches()) {
            throw new IllegalArgumentException("Invalid ENA accession format: " + projectAccession);
        }
        String responseBody = restTemplate.getForObject(ENA_BASE_URL + projectAccession, String.class);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(responseBody)));
    }
}
