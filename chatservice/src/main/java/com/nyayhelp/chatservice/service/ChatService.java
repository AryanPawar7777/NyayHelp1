package com.nyayhelp.chatservice.service;

import com.nyayhelp.chatservice.dto.CaseResponse;
import com.nyayhelp.chatservice.dto.ChatResponse;
import com.nyayhelp.chatservice.dto.MessageRequest;
import com.nyayhelp.chatservice.dto.NotificationRequest;
import com.nyayhelp.chatservice.dto.UserProfileResponse;
import com.nyayhelp.chatservice.model.Message;
import com.nyayhelp.chatservice.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private MessageRepository repository;

    @Autowired
    private RestClient restClient;

    public String sendMessage(
            MessageRequest request,
            Authentication authentication
    ) {

        Long senderId =
                (Long) authentication.getDetails();

        String senderEmail =
                (String) authentication.getPrincipal();

        String senderRole = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replace("ROLE_", "");

        // FETCH CASE
        CaseResponse caseData =
                restClient.get()
                        .uri("http://localhost:8083/api/cases/" + request.caseId)
                        .retrieve()
                        .body(CaseResponse.class);

        if (caseData == null ||
                !"ASSIGNED".equals(caseData.status)) {

            throw new RuntimeException(
                    "Chat not allowed. Case not assigned yet."
            );
        }

        // CHECK AUTHORIZATION
        if (!senderId.equals(caseData.clientId)
                &&
                !senderId.equals(caseData.lawyerId)) {

            throw new RuntimeException(
                    "Unauthorized to chat in this case."
            );
        }

        // SAVE MESSAGE
        Message msg = new Message();

        msg.setCaseId(request.caseId);

        msg.setSenderId(senderId);

        msg.setSenderRole(senderRole);

        msg.setContent(request.content);

        msg.setTimestamp(LocalDateTime.now());

        repository.save(msg);

        // SEND NOTIFICATION
        try {

            NotificationRequest notification =
                    new NotificationRequest();

            // TEMP DUMMY EMAIL
            notification.setEmail(
                    "receiver@gmail.com"
            );

            notification.setSubject(
                    "New Message Received"
            );

            notification.setMessage(
                    "You received a new message regarding your legal case on NyayHelp."
            );

            restClient.post()
                    .uri("http://localhost:8085/api/notifications/send")
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            System.out.println(
                    "Notification Failed"
            );
        }

        return "Message Sent";
    }

    public List<ChatResponse> getMessages(
            Long caseId
    ) {

        List<Message> messages =
                repository.findByCaseIdOrderByTimestampAsc(caseId);

        return messages.stream().map(msg -> {

            ChatResponse res =
                    new ChatResponse();

            try {

                UserProfileResponse user =
                        restClient.get()
                                .uri("http://localhost:8082/api/users/by-auth/" + msg.getSenderId())
                                .retrieve()
                                .body(UserProfileResponse.class);

                if (user != null) {

                    res.name = user.name;

                    res.role = user.role;

                } else {

                    res.name = "Unknown";

                    res.role = "UNKNOWN";
                }

            } catch (Exception e) {

                res.name = "Unknown";

                res.role = "UNKNOWN";
            }

            res.message = msg.getContent();

            res.time = msg.getTimestamp().toString();

            return res;

        }).toList();
    }
}