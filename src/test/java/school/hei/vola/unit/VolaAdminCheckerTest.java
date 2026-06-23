package school.hei.vola.unit;

import org.junit.jupiter.api.Test;

class VolaAdminCheckerTest {
  @Test
  void admin_email_returns_true() {
    var checker =
        new school.hei.vola.endpoint.rest.security.VolaAdminChecker(
            "admin@hei.school, bob@hei.school");
    assert checker.isAdmin("admin@hei.school");
  }

  @Test
  void admin_email_case_insensitive() {
    var checker =
        new school.hei.vola.endpoint.rest.security.VolaAdminChecker(
            "admin@hei.school,bob@hei.school");
    assert checker.isAdmin("bob@Hei.School");
  }

  @Test
  void non_admin_email_returns_false() {
    var checker =
        new school.hei.vola.endpoint.rest.security.VolaAdminChecker(
            "admin@hei.school,bob@hei.school");
    assert !checker.isAdmin("other@hei.school");
  }

  @Test
  void null_email_returns_false() {
    var checker =
        new school.hei.vola.endpoint.rest.security.VolaAdminChecker(
            "admin@hei.school, valisoa@hei.school");
    assert !checker.isAdmin(null);
  }

  @Test
  void second_admin_also_matches() {
    var checker =
        new school.hei.vola.endpoint.rest.security.VolaAdminChecker(
            "admin@hei.school,valisoa@hei.school");
    assert checker.isAdmin("valisoa@hei.school");
  }
}
