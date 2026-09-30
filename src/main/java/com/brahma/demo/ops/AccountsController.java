package com.brahma.demo.ops;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AccountsController {
    private static final List<String> INDUSTRIES = List.of(
            "technology", "healthcare", "retail", "finance", "manufacturing");
    private final AccountsClient accounts;

    public AccountsController(AccountsClient accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "") String industry,
                        @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int offset, Model model) {
        if (offset < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid offset");
        model.addAttribute("page", accounts.list(industry, q, offset));
        model.addAttribute("industries", INDUSTRIES);
        model.addAttribute("industry", industry);
        model.addAttribute("q", q);
        return "accounts";
    }

    @GetMapping("/accounts/{id}")
    public String detail(@PathVariable String id, Model model) {
        try {
            model.addAttribute("account", accounts.get(id));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found", ex);
        }
        return "account";
    }
}
