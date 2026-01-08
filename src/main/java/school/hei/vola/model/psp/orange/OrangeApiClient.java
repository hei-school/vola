package school.hei.vola.model.psp.orange;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.net.http.HttpClient.newHttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class OrangeApiClient {
  private final String baseUrl;
  public static final ObjectMapper om = new ObjectMapper();

  static {
    om.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public OrangeDailyTransactions transactionsOf(LocalDate date) {
    var path =
        String.format(
            "/transactions?date=%s-%s-%s",
            date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    var uri = baseUrl + path;
    log.info("Building Orange API request with baseUrl: {}, path: {}", baseUrl, path);
    log.info("Full URI: {}", uri);

    var httpRequest = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();

    try (var httpClient = newHttpClient()) {
      log.info("Fetching Orange transactions: {}", uri);
      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      var httpStatus = response.statusCode();
      log.info("HTTP status from Orange: {}", httpStatus);
      log.info("Response body: {}", response.body());

      if (httpStatus / 100 != 2) {
        log.error(
            "Non-2xx response from Orange API: status={}, body={}", httpStatus, response.body());
        throw new OrangeApiException("Response from Orange was not 2xx: " + response);
      }

      var dailyTransactions = om.readValue(response.body(), OrangeDailyTransactions.class);
      log.info("Successfully parsed {} transactions", dailyTransactions.getTransactions().size());
      return dailyTransactions;
    } catch (IOException | InterruptedException e) {
      log.error("Error calling Orange API for date {}: {}", date, e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
