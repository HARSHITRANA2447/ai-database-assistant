package com.aidb.controller;

import com.aidb.repository.QueryHistoryRepository;
import com.aidb.service.SchemaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final QueryHistoryRepository historyRepo;
    private final SchemaService schemaService;

    public PageController(QueryHistoryRepository historyRepo, SchemaService schemaService) {
        this.historyRepo = historyRepo;
        this.schemaService = schemaService;
    }

    @GetMapping("/")
    public String home() { return "redirect:/chat"; }

    @GetMapping("/chat")
    public String chat(Model model, Authentication auth) {
        if (auth != null) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("queryCount", historyRepo.countByUsername(auth.getName()));
        }
        return "chat";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("totalQueries", historyRepo.count());
        model.addAttribute("recentQueries", historyRepo.findAllByOrderByTimestampDesc()
            .stream().limit(20).toList());
        model.addAttribute("schemaInfo", schemaService.getDetailedSchema());
        return "admin";
    }

    @GetMapping("/login")
    public String login() { return "login"; }
}
