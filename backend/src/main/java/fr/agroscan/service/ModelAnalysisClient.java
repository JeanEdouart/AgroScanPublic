package fr.agroscan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ModelAnalysisClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelAnalysisClient(
            @Value("${agroscan.analysis.model-api-url}") String modelApiUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(modelApiUrl).build();
    }

    public ScanAnalysisResult analyze(String imageBase64, String imageMediaType) {
        ModelAnalysisResponse response;
        try {
            response = restClient.post()
                    .uri("/analyze")
                    .body(new ModelAnalysisRequest(imageBase64, imageMediaType))
                    .retrieve()
                    .body(ModelAnalysisResponse.class);
        } catch (RestClientException exception) {
            throw new AnalysisUnavailableException("Le service d'analyse est indisponible", exception);
        }
        if (response == null || response.plant() == null || response.disease() == null) {
            throw new AnalysisUnavailableException("Le service d'analyse a renvoye une reponse invalide");
        }
        return new ScanAnalysisResult(
                response.plant(),
                response.disease(),
                response.healthy(),
                response.confidence(),
                toJson(response),
                null
        );
    }

    private String toJson(ModelAnalysisResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new AnalysisUnavailableException("La reponse d'analyse est invalide", exception);
        }
    }

    record ModelAnalysisRequest(String imageBase64, String imageMediaType) {
    }

    record ModelAnalysisResponse(
            String plant,
            String disease,
            Boolean healthy,
            Double confidence,
            Double plantConfidence,
            Double diseaseConfidence
    ) {
    }
}
