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
            "/transactions?date=%s-%s-%s", date.getYear(), date.getMonth(), date.getDayOfMonth());
    var httpRequest = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();

    try (var httpClient = newHttpClient()) {
      log.info("Fetching Orange transactions of {}", date);
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
