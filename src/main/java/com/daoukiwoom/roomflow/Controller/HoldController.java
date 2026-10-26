package com.daoukiwoom.roomflow.Controller;

import com.daoukiwoom.roomflow.DTO.Request.AcquireHoldRequest;
import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import com.daoukiwoom.roomflow.DTO.Response.HoldResponse;
import com.daoukiwoom.roomflow.DTO.Response.HoldSettingsResponse;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.Interface.HoldService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/holds")
public class HoldController {

    @Autowired
    private HoldService holdService;

    @GetMapping("/settings")
    public APIResponse<HoldSettingsResponse> settings() {
        APIResponse<HoldSettingsResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(holdService.settings());
        return apiResponse;
    }

    @PostMapping
    public APIResponse<HoldResponse> acquire(
            @Valid @RequestBody AcquireHoldRequest request,
            @AuthenticationPrincipal AuthUser actor
    ) {
        APIResponse<HoldResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(holdService.acquire(request, actor));
        return apiResponse;
    }

    @PostMapping("/{id}/heartbeat")
    public APIResponse<HoldResponse> heartbeat(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthUser actor
    ) {
        APIResponse<HoldResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(holdService.heartbeat(id, actor));
        return apiResponse;
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> release(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthUser actor
    ) {
        holdService.release(id, actor);
        return new APIResponse<>();
    }
}
