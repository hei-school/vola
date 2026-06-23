package school.hei.vola.endpoint.http;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import school.hei.vola.repository.ApplicationRepository;

@Controller
@AllArgsConstructor
public class VolaController {

  private final ApplicationRepository applicationRepository;

  @GetMapping("/payments")
  public String exportPage(Model model) {
    model.addAttribute("applications", applicationRepository.findAll());
    return "payments";
  }
}
