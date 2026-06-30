package fr.agroscan;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "agroscan.security.jwt-secret=test-jwt-secret-with-at-least-32-characters",
        "agroscan.admin.email=admin@example.test",
        "agroscan.admin.password=AdminTestPassword123!",
        "agroscan.admin.first-name=Admin",
        "agroscan.admin.last-name=Test"
})
@AutoConfigureMockMvc
@Testcontainers
class SystemCheckIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    MockMvc mockMvc;

    @Test
    void systemCheckReadsTheDatabase() throws Exception {
        mockMvc.perform(get("/api/system-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.message").value("Connexion PostgreSQL opérationnelle"));
    }

    @Test
    void registrationCreatesAUserAccountAndReturnsTokens() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "alice.martin@example.fr",
                                  "password": "MotDePasse123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.firstName").value("Alice"));
    }

    @Test
    void defaultAdminCanLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.test",
                                  "password": "AdminTestPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void authenticatedUserCanUpdateProfileAndPassword() throws Exception {
        String registrationResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Thomas",
                                  "lastName": "Bernard",
                                  "email": "thomas.bernard@example.fr",
                                  "password": "MotDePasse123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(registrationResponse, "$.accessToken");

        String profileResponse = mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Tom",
                                  "lastName": "Bernard",
                                  "email": "tom.bernard@example.fr"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.firstName").value("Tom"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String updatedAccessToken = JsonPath.read(profileResponse, "$.accessToken");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + updatedAccessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "MotDePasse123!",
                                  "newPassword": "NouveauMotDePasse123!"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void authenticatedUserCanCreateSearchAndViewScan() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.test",
                                  "password": "AdminTestPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginResponse, "$.accessToken");

        String createResponse = mockMvc.perform(post("/api/scans")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Feuille de tomate",
                                  "description": "Observation après arrosage",
                                  "imageBase64": "/9j/2Q==",
                                  "thumbnailBase64": "/9j/2Q==",
                                  "imageMediaType": "image/jpeg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Feuille de tomate"))
                .andReturn().getResponse().getContentAsString();
        Integer scanId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("name", "tomate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].thumbnailDataUrl").isNotEmpty());

        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/scans/{id}", scanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageDataUrl").value("data:image/jpeg;base64,/9j/2Q=="));

        String otherUserResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Autre",
                                  "lastName": "Utilisateur",
                                  "email": "autre.utilisateur@example.fr",
                                  "password": "MotDePasse123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String otherUserToken = JsonPath.read(otherUserResponse, "$.accessToken");

        mockMvc.perform(get("/api/scans/{id}", scanId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanSearchUpdateAndDeleteUsersWhileRegularUsersAreForbidden() throws Exception {
        String adminLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.test",
                                  "password": "AdminTestPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = JsonPath.read(adminLoginResponse, "$.accessToken");
        Integer adminId = JsonPath.read(adminLoginResponse, "$.user.id");

        String userRegistrationResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Claire",
                                  "lastName": "Durand",
                                  "email": "claire.admin-test@example.fr",
                                  "password": "MotDePasse123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String userToken = JsonPath.read(userRegistrationResponse, "$.accessToken");
        Integer userId = JsonPath.read(userRegistrationResponse, "$.user.id");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("search", "claire.admin-test")
                        .queryParam("sortBy", "role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("claire.admin-test@example.fr"));

        mockMvc.perform(patch("/api/admin/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Clara",
                                  "lastName": "Durand",
                                  "email": "clara.admin-test@example.fr",
                                  "role": "ADMIN",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Clara"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(patch("/api/admin/users/{id}", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Admin",
                                  "lastName": "AgroScan",
                                  "email": "admin@agroscan.fr",
                                  "role": "ADMIN",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADMIN_OPERATION_INVALID"));

        mockMvc.perform(delete("/api/admin/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
