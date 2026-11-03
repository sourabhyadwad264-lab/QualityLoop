package com.qualityloop.controller;

import com.qualityloop.dto.ActionRequest;
import com.qualityloop.dto.DefectRequest;
import com.qualityloop.dto.StatusRequest;
import com.qualityloop.entity.AuditEntry;
import com.qualityloop.entity.Defect;
import com.qualityloop.entity.DefectStatus;
import com.qualityloop.service.DefectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/defects")
public class DefectController {

    private final DefectService service;

    public DefectController(DefectService service) { this.service = service; }

    @GetMapping
    public List<Defect> list(@RequestParam(required = false) DefectStatus status,
                             @RequestParam(required = false) String q) {
        return service.list(status, q);
    }

    @GetMapping("/{id}")
    public Defect get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Defect create(@Valid @RequestBody DefectRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public Defect update(@PathVariable Long id, @Valid @RequestBody DefectRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PatchMapping("/{id}/status")
    public Defect status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.changeStatus(id, r.status());
    }

    @PostMapping("/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public Defect addAction(@PathVariable Long id, @Valid @RequestBody ActionRequest r) {
        return service.addAction(id, r);
    }

    @PatchMapping("/{id}/actions/{actionId}/complete")
    public Defect complete(@PathVariable Long id, @PathVariable Long actionId) {
        return service.completeAction(id, actionId);
    }

    @DeleteMapping("/{id}/actions/{actionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAction(@PathVariable Long id, @PathVariable Long actionId) {
        service.deleteAction(id, actionId);
    }

    @GetMapping("/{id}/audit")
    public List<AuditEntry> audit(@PathVariable Long id) { return service.history(id); }
}
