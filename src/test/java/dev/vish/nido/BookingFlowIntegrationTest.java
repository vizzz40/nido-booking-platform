package dev.vish.nido;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersUsersPublishesAListingAndBlocksBookedDates() throws Exception {
        String hostToken = register(
                "Elena Host", "elena@example.com", "Secure123!", "HOST");
        String guestToken = register(
                "Marco Guest", "marco@example.com", "Secure123!", "GUEST");

        Map<String, Object> listing = Map.ofEntries(
                Map.entry("title", "Courtyard apartment"),
                Map.entry("city", "Padua"),
                Map.entry("country", "Italy"),
                Map.entry("description", "A quiet apartment close to the historic centre."),
                Map.entry("propertyType", "APARTMENT"),
                Map.entry("maxGuests", 3),
                Map.entry("bedrooms", 1),
                Map.entry("beds", 2),
                Map.entry("bathrooms", 1),
                Map.entry("nightlyPrice", 110),
                Map.entry("imageUrl", "/images/rome.svg"),
                Map.entry("amenities", List.of("Wi-Fi", "Kitchen"))
        );

        String listingJson = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listing)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Padua"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long listingId = objectMapper.readTree(listingJson).get("id").asLong();

        LocalDate checkIn = LocalDate.now().plusDays(12);
        LocalDate checkOut = checkIn.plusDays(3);
        Map<String, Object> booking = Map.of(
                "listingId", listingId,
                "checkIn", checkIn.toString(),
                "checkOut", checkOut.toString(),
                "guests", 2
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.totalPrice").value(369.6));

        mockMvc.perform(get("/api/listings")
                        .param("location", "Padua")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String register(String name, String email, String password, String role)
            throws Exception {
        Map<String, String> body = Map.of(
                "name", name,
                "email", email,
                "password", password,
                "role", role
        );
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
