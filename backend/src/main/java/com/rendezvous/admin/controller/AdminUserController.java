package com.rendezvous.admin.controller;

import com.rendezvous.admin.dto.AdminUserResponse;
import com.rendezvous.admin.service.AdminUserService;
import com.rendezvous.common.web.PageResponse;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/users")
    public PageResponse<AdminUserResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean blocked,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(query, blocked, pageable);
    }

    @PutMapping("/api/admin/users/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable UUID id) {
        service.block(id);
    }

    @DeleteMapping("/api/admin/users/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable UUID id) {
        service.unblock(id);
    }
}
