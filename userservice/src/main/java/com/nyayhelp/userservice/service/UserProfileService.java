package com.nyayhelp.userservice.service;

import com.nyayhelp.userservice.dto.LawyerVerificationRequest;
import com.nyayhelp.userservice.dto.NotificationRequest;
import com.nyayhelp.userservice.dto.UserProfileRequest;
import com.nyayhelp.userservice.model.UserProfile;
import com.nyayhelp.userservice.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private RestClient restClient;

    public String createProfile(UserProfileRequest request,
                                Authentication authentication) {

        String email = (String) authentication.getPrincipal();

        Long authUserId =
                (Long) authentication.getCredentials();

        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replace("ROLE_", "");

        if (repository.findByAuthUserId(authUserId).isPresent()) {
            throw new RuntimeException("Profile already exists");
        }

        UserProfile user = new UserProfile();

        user.setAuthUserId(authUserId);
        user.setEmail(email);
        user.setRole(role);

        user.setName(request.name);
        user.setLocation(request.location);
        user.setCategory(request.category);
        user.setExperience(request.experience);
        user.setFees(request.fees);

        user.setVerificationStatus("PENDING");

        repository.save(user);

        return "Profile Created Successfully";
    }

    public UserProfile getByAuthId(Long authId) {

        return repository.findByAuthUserId(authId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    public String uploadLawyerDocuments(
            LawyerVerificationRequest request,
            Authentication authentication
    ) {

        Long authUserId =
                (Long) authentication.getCredentials();

        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replace("ROLE_", "");

        if (!role.equals("LAWYER")) {
            throw new RuntimeException(
                    "Only lawyers can upload documents"
            );
        }

        UserProfile profile =
                repository.findByAuthUserId(authUserId)
                        .orElseThrow(() ->
                                new RuntimeException("Profile not found"));

        profile.setBarCouncilId(request.barCouncilId);
        profile.setDocuments(request.documents);

        profile.setVerificationStatus("PENDING");

        repository.save(profile);

        // SEND NOTIFICATION
        try {

            NotificationRequest notification =
                    new NotificationRequest();

            notification.setEmail(profile.getEmail());

            notification.setSubject(
                    "Lawyer Verification Submitted"
            );

            notification.setMessage(
                    "Your lawyer verification documents have been submitted successfully."
            );

            restClient.post()
                    .uri("http://localhost:8085/api/notifications/send")
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            System.out.println("Notification failed");

        }

        return "Documents Uploaded Successfully";
    }

    public List<UserProfile> getPendingLawyers() {

        return repository.findByRoleAndVerificationStatus(
                "LAWYER",
                "PENDING"
        );
    }

    public String approveLawyer(Long authUserId) {

        UserProfile profile =
                repository.findByAuthUserId(authUserId)
                        .orElseThrow(() ->
                                new RuntimeException("Lawyer not found"));

        if (!profile.getRole().equals("LAWYER")) {
            throw new RuntimeException("User is not lawyer");
        }

        profile.setVerificationStatus("APPROVED");

        repository.save(profile);

        // SEND NOTIFICATION
        try {

            NotificationRequest notification =
                    new NotificationRequest();

            notification.setEmail(profile.getEmail());

            notification.setSubject(
                    "Lawyer Verification Approved"
            );

            notification.setMessage(
                    "Congratulations! Your lawyer profile has been approved."
            );

            restClient.post()
                    .uri("http://localhost:8085/api/notifications/send")
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            System.out.println("Notification failed");

        }

        return "Lawyer Approved Successfully";
    }

    public String rejectLawyer(Long authUserId) {

        UserProfile profile =
                repository.findByAuthUserId(authUserId)
                        .orElseThrow(() ->
                                new RuntimeException("Lawyer not found"));

        if (!profile.getRole().equals("LAWYER")) {
            throw new RuntimeException("User is not lawyer");
        }

        profile.setVerificationStatus("REJECTED");

        repository.save(profile);

        // SEND NOTIFICATION
        try {

            NotificationRequest notification =
                    new NotificationRequest();

            notification.setEmail(profile.getEmail());

            notification.setSubject(
                    "Lawyer Verification Rejected"
            );

            notification.setMessage(
                    "Your lawyer verification was rejected. Please re-upload valid documents."
            );

            restClient.post()
                    .uri("http://localhost:8085/api/notifications/send")
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            System.out.println("Notification failed");

        }

        return "Lawyer Rejected Successfully";
    }
}