package school.hei.vola.model.psp.orange;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.net.http.HttpClient.newHttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrangeApiClient {
  private final String baseUrl;
  private final ObjectMapper om;

  public OrangeApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.om = new ObjectMapper();
    om.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public OrangeDailyTransactions transactionsOf(Instant instant) {
    var path = "/transactions?date=" + instant;
    var httpRequest = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();

    try (var httpClient = newHttpClient()) {
      log.info("Fetching Orange transactions of {}", instant);
      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      var httpStatus = response.statusCode();
      log.info("Https status from Orange was: {}", httpStatus);
      if (httpStatus / 100 != 2) {
        throw new OrangeApiException("Response from Orange was not 2xx: " + response);
      }

      return om.readValue(response.body(), OrangeDailyTransactions.class);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
