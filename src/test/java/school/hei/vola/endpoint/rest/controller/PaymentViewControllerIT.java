package school.hei.vola.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import school.hei.vola.conf.FacadeIT;

class PaymentViewControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void unauthenticated_request_to_payments_shows_login_page() {
    var response = restTemplate.getForEntity("/payments", String.class);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("Please sign in"));
  }

  @Test
  void authenticated_user_can_access_payments() {
    var sessionCookie = fetchSessionCookie();
    var csrfToken = fetchCsrfToken(sessionCookie);

    var loginResponse = login(sessionCookie, csrfToken, "admin@cute.dev");
    assertEquals(302, loginResponse.getStatusCodeValue());
    assertEquals(
        "/payments", Objects.requireNonNull(loginResponse.getHeaders().getLocation()).getPath());

    var authCookie = extractSessionId(loginResponse);
    var paymentsHeaders = new HttpHeaders();
    paymentsHeaders.add(HttpHeaders.COOKIE, authCookie);
    var paymentsResponse =
        restTemplate.exchange(
            "/payments", HttpMethod.GET, new HttpEntity<>(paymentsHeaders), String.class);

    assertEquals(200, paymentsResponse.getStatusCodeValue());
  }

  @Test
  void unknown_user_cannot_login() {
    var sessionCookie = fetchSessionCookie();
    var csrfToken = fetchCsrfToken(sessionCookie);

    var loginResponse = login(sessionCookie, csrfToken, "nobody@user.guest");
    assertEquals(302, loginResponse.getStatusCodeValue());
    assertTrue(
        Objects.requireNonNull(loginResponse.getHeaders().getLocation())
            .getPath()
            .contains("/login"));
  }

  @Test
  void static_resources_are_accessible_without_authentication() {
    var cssResponse = restTemplate.getForEntity("/style/history.css", String.class);
    assertEquals(200, cssResponse.getStatusCodeValue());
  }

  @Test
  void api_endpoint_with_invalid_adminKey_returns_401() {
    var response =
        restTemplate.getForEntity(
            "/payments/export/csv?adminKey=wrong&applicationName=test", String.class);
    assertEquals(401, response.getStatusCodeValue());
  }

  private String fetchSessionCookie() {
    var loginPage = restTemplate.getForEntity("/login", String.class);
    var setCookie = loginPage.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    return extractSessionId(loginPage);
  }

  private String fetchCsrfToken(String sessionCookie) {
    var headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, sessionCookie);
    var loginPage =
        restTemplate.exchange("/login", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    var token = extractCsrfToken(loginPage.getBody());
    assertNotNull(token);
    return token;
  }

  private ResponseEntity<String> login(String sessionCookie, String csrfToken, String email) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add(HttpHeaders.COOKIE, sessionCookie);

    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("email", email);
    formData.add("password", "test-password");
    formData.add("_csrf", csrfToken);

    return restTemplate.postForEntity("/login", new HttpEntity<>(formData, headers), String.class);
  }

  private static String extractSessionId(ResponseEntity<?> response) {
    var cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    return cookie != null ? cookie.split(";")[0] : null;
  }

  private static String extractCsrfToken(String html) {
    var pattern = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
    var matcher = pattern.matcher(html);
    return matcher.find() ? matcher.group(1) : null;
  }
}
