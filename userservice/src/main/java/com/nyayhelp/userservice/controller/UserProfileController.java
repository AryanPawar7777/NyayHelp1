package com.nyayhelp.userservice.controller;

import com.nyayhelp.userservice.dto.LawyerVerificationRequest;
import com.nyayhelp.userservice.dto.UserProfileRequest;
import com.nyayhelp.userservice.model.UserProfile;
import com.nyayhelp.userservice.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    @Autowired
    private UserProfileService service;

    @PostMapping("/create")
    public String create(@RequestBody UserProfileRequest request, Authentication authentication) {
        return service.createProfile(request, authentication);
    }

    @GetMapping("/by-auth/{authId}")
    public UserProfile getByAuthId(@PathVariable Long authId) {
        return service.getByAuthId(authId);
    }


    @PostMapping("/lawyer/documents")
public String uploadDocuments(@RequestBody LawyerVerificationRequest request,
                              Authentication authentication) {
    return service.uploadLawyerDocuments(request, authentication);
}

@GetMapping("/admin/lawyers/pending")
public List<UserProfile> pendingLawyers() {
    return service.getPendingLawyers();
}

@PutMapping("/admin/lawyers/{authUserId}/approve")
public String approveLawyer(@PathVariable Long authUserId) {
    return service.approveLawyer(authUserId);
}

@PutMapping("/admin/lawyers/{authUserId}/reject")
public String rejectLawyer(@PathVariable Long authUserId) {
    return service.rejectLawyer(authUserId);
}
}