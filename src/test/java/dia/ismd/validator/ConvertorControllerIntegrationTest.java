package dia.ismd.validator;

import dia.ismd.common.exceptions.JsonExportException;
import dia.ismd.validator.convertor.ConvertorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApplicationModuleTest()
@ContextConfiguration(classes = ConvertorControllerIntegrationTest.TestConfig.class)
class ConvertorControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ConvertorService convertorService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(convertorService);
    }

    @Configuration
    @Import(ConvertorController.class)
    static class TestConfig {
        @Bean
        public ConvertorService convertorService() {
            return mock(ConvertorService.class);
        }
    }

    private static final String ARCHI_XML_CONTENT = """
            <model xmlns="http://www.opengroup.org/xsd/archimate/3.0/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://www.opengroup.org/xsd/archimate/3.0/ http://www.opengroup.org/xsd/archimate/3.1/archimate3_Diagram.xsd"
            identifier="id-1a2b89ddbd634df6a46af504ebebfea8">
            " +
            "<name xml:lang="cs">DEMO Šablona pro popis dat Archi v1</name>
            " +
            "<properties>
            " +
            "<property propertyDefinitionRef="propid-2">
            " +
            "<value xml:lang="cs"/>
            " +
            "</property>
            " +
            "<property propertyDefinitionRef="propid-15">
            " +
            "<value xml:lang="cs">https://data.dia.gov.cz</value>
            " +
            "</property>
            " +
            "</properties>" +
            "<folder></folder></archimate:model>
            """;

    private static final String JSON_OUTPUT = "{\"result\":\"success\"}";
    private static final String TTL_OUTPUT = "@prefix : <http://example.org/> .\n:subject :predicate :object .";

    @Test
    void testSuccessfulArchiXmlToJsonConversion() throws Exception {
        // Reset the mock before each test to ensure clean state
        reset(convertorService);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        // Configure mock service behavior
        doNothing().when(convertorService).parseArchiFromString(anyString());
        doNothing().when(convertorService).convertArchi();
        when(convertorService.exportArchiToJson()).thenReturn(JSON_OUTPUT);

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file)
                        .param("output", "json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string(JSON_OUTPUT));

        // Verify service interactions
        verify(convertorService).parseArchiFromString(anyString());
        verify(convertorService).convertArchi();
        verify(convertorService).exportArchiToJson();
    }

    @Test
    void testSuccessfulArchiXmlToTurtleConversion() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        // Configure mock service behavior
        doNothing().when(convertorService).parseArchiFromString(anyString());
        doNothing().when(convertorService).convertArchi();
        when(convertorService.exportArchiToTurtle()).thenReturn(TTL_OUTPUT);

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file)
                        .param("output", "ttl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().string(TTL_OUTPUT));

        // Verify service interactions
        verify(convertorService).parseArchiFromString(anyString());
        verify(convertorService).convertArchi();
        verify(convertorService).exportArchiToTurtle();
    }

    @Test
    void testEmptyFileUpload() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xml",
                "application/xml",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOversizedFileUpload() throws Exception {
        // Arrange - Create a file larger than 5MB
        byte[] oversizedContent = new byte[5_242_881]; // 5MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.xml",
                "application/xml",
                oversizedContent
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void testUnsupportedFileFormat() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "PDF content".getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testUnsupportedOutputFormat() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        doNothing().when(convertorService).parseArchiFromString(anyString());
        doNothing().when(convertorService).convertArchi();

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file)
                        .param("output", "csv"))  // Unsupported format
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string("Nepodporovaný výstupní formát: csv"));
    }

    @Test
    void testServiceExceptionHandling() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        // Configure service to throw exception
        doThrow(new RuntimeException("Service processing error"))
                .when(convertorService).parseArchiFromString(anyString());

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Service processing error"));
    }

    @Test
    void testJsonExportException() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        doNothing().when(convertorService).parseArchiFromString(anyString());
        doNothing().when(convertorService).convertArchi();
        when(convertorService.exportArchiToJson())
                .thenThrow(new JsonExportException("Error exporting to JSON"));

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file)
                        .param("output", "json"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error exporting to JSON"));
    }

    @Test
    void testTurtleExportException() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        doNothing().when(convertorService).parseArchiFromString(anyString());
        doNothing().when(convertorService).convertArchi();
        when(convertorService.exportArchiToTurtle())
                .thenThrow(new JsonExportException("Error exporting to Turtle"));

        // Act & Assert
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file)
                        .param("output", "ttl"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error exporting to Turtle"));
    }

    @Test
    void testDefaultOutputFormat() throws Exception {
        // Arrange - Test that JSON is used when no output format is specified
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xml",
                "application/xml",
                ARCHI_XML_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        doNothing().when(convertorService).parseArchiFromString(anyString());
        doNothing().when(convertorService).convertArchi();
        when(convertorService.exportArchiToJson()).thenReturn(JSON_OUTPUT);

        // Act & Assert - Don't specify output param, should default to JSON
        mockMvc.perform(multipart("/api/prevodnik/prevod")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string(JSON_OUTPUT));

        verify(convertorService).exportArchiToJson();
    }
}
