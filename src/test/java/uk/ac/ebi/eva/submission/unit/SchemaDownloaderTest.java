package uk.ac.ebi.eva.submission.unit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.eva.submission.repository.CallHomeEventRepository;
import uk.ac.ebi.eva.submission.util.SchemaDownloader;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {SchemaDownloader.class, CacheConfig.class})
@EnableCaching
@EnableRetry
@Import(CacheAutoConfiguration.class)
class SchemaDownloaderTest {

    @Autowired
    private SchemaDownloader schemaDownloader;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private CallHomeEventRepository callHomeEventRepository;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("schemaCache").clear();

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"type\": \"object\"}");
    }

    @Test
    void testSchemaCachedAfterFirstCall() {
        String url = "https://raw.githubusercontent.com/some/schema.json";

        schemaDownloader.loadSchemaFromGitHub(url);
        schemaDownloader.loadSchemaFromGitHub(url);
        schemaDownloader.loadSchemaFromGitHub(url);

        verify(restTemplate, times(1)).getForObject(url, String.class);
    }

    @Test
    void testCacheEvictForcesRefetch() {
        String url = "https://raw.githubusercontent.com/some/schema.json";

        schemaDownloader.loadSchemaFromGitHub(url);
        schemaDownloader.loadSchemaFromGitHub(url);

        schemaDownloader.evictSchemaCache();

        schemaDownloader.loadSchemaFromGitHub(url);

        verify(restTemplate, times(2)).getForObject(url, String.class);
    }


    @Test
    void testDifferentUrlsAreCachedSeparately() {
        String url1 = "https://raw.githubusercontent.com/schema-v1.json";
        String url2 = "https://raw.githubusercontent.com/schema-v2.json";

        schemaDownloader.loadSchemaFromGitHub(url1);
        schemaDownloader.loadSchemaFromGitHub(url1);
        schemaDownloader.loadSchemaFromGitHub(url2);
        schemaDownloader.loadSchemaFromGitHub(url2);

        verify(restTemplate, times(1)).getForObject(url1, String.class);
        verify(restTemplate, times(1)).getForObject(url2, String.class);
    }

    @Test
    void testLoadSchemaFromGitHubRetriesOnFailure() {
        String url = "https://raw.githubusercontent.com/some/schema.json";
        when(restTemplate.getForObject(eq(url), eq(String.class))).thenThrow(new RuntimeException("Github down"));

        assertThrows(Exception.class, () -> schemaDownloader.loadSchemaFromGitHub(url));

        verify(restTemplate, times(5)).getForObject(eq(url), eq(String.class));
    }

    @Test
    void testGetLatestTagRetriesOnFailure() {
        String url = "https://raw.githubusercontent.com/some/schema.json";
        when(restTemplate.getForObject(eq(url), eq(JsonNode.class))).thenThrow(new RuntimeException("Github down"));

        assertThrows(Exception.class, () -> schemaDownloader.getLatestTag(url));

        verify(restTemplate, times(5)).getForObject(eq(url), eq(JsonNode.class));
    }
}