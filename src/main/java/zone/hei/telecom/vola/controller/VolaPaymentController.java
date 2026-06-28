package zone.hei.telecom.vola.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zone.hei.telecom.vola.model.Payment;
import zone.hei.telecom.vola.service.PaymentService;

@Controller
@AllArgsConstructor
public class VolaPaymentController {
    private final PaymentService paymentService;

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ROLE_STUDENT', 'ROLE_ADMIN')")
    public String showPaymentPage(
            @RequestParam(name = "studentRef", required = false) String studentRef,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            Model model) {
        
        Page<Payment> paymentPage = paymentService.getAllPayments(studentRef, page, pageSize);
        model.addAttribute("payments", paymentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paymentPage.getTotalPages());
        return "payment";
    }

    @PostMapping("/payments/process")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public String processPayment(
            @RequestParam("studentRef") String studentRef,
            @RequestParam("amount") Double amount) {
        
        paymentService.savePayment(studentRef, amount);
        return "redirect:/payments?success=true";
    }
}
