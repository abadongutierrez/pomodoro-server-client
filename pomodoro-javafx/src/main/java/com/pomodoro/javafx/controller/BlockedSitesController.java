package com.pomodoro.javafx.controller;

import com.pomodoro.javafx.service.BlockedSitesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blocked-sites")
public class BlockedSitesController {

    private final BlockedSitesService blockedSitesService;

    public BlockedSitesController(BlockedSitesService blockedSitesService) {
        this.blockedSitesService = blockedSitesService;
    }

    @GetMapping
    public List<String> getSites() {
        return blockedSitesService.getSites();
    }

    @PutMapping
    public List<String> putSites(@RequestBody List<String> sites) {
        return blockedSitesService.setSites(sites);
    }
}
