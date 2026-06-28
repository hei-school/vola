package zone.hei.telecom.vola.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment")
public class VolaPaymentController {

    @GetMapping
    public String displayPaymentPage() {
        return "payment";
    }

    @PostMapping("/initiate")
    public String handlePaymentInitiation(
            @RequestParam("amount") Double amount,
            @RequestParam(value = "scope", required = false) String scope,
            RedirectAttributes redirectAttributes) {
        
        try {
            String finalizedScope = (scope == null || scope.trim().isEmpty()) ? "Général" : scope.trim();

            System.out.println("==============================================");
            System.out.println("[VOLA BACKEND] Traitement d'un nouveau flux : ");
            System.out.println(" -> Montant reçu : " + amount + " MGA");
            System.out.println(" -> Scope identifié : " + finalizedScope);
            System.out.println("==============================================");

            redirectAttributes.addFlashAttribute("successMessage", "Flux initié avec succès !");

        } catch (Exception e) {
            System.err.println("[VOLA ERROR] Échec de l'initiation : " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors du traitement.");
        }

        return "redirect:/payment";
    }
}
