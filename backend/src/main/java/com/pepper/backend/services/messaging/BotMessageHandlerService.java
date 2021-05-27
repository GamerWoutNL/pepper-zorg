package com.pepper.backend.services.messaging;

import com.pepper.backend.controllers.BotCommunicationController;
import com.pepper.backend.model.*;
import com.pepper.backend.model.database.Response;
import com.pepper.backend.model.messaging.Message;
import com.pepper.backend.model.messaging.Person;
import com.pepper.backend.model.messaging.Sender;
import com.pepper.backend.model.messaging.Task;
import com.pepper.backend.services.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class BotMessageHandlerService {

    private static final Logger LOG = LoggerFactory.getLogger(BotMessageHandlerService.class);

    @Value("${encryption.enabled}")
    private boolean encryptionEnabled;

    @Value("${encryption.password}")
    private String encryptionPassword;

    private final BotCommunicationController botCommunicationController;
    private final DatabaseService databaseService;
    private final MessageParserService messageParser;
    private final MessageEncryptorService messageEncryptor;

    public BotMessageHandlerService(BotCommunicationController botCommunicationController, DatabaseService databaseService, MessageParserService messageParser, MessageEncryptorService messageEncryptor) {
        this.botCommunicationController = botCommunicationController;
        this.databaseService = databaseService;
        this.messageParser = messageParser;
        this.messageEncryptor = messageEncryptor;
    }

    public void send(Person person, String personId, Task task, String taskId, String data) {
        String message = this.messageParser.stringify(Sender.PLATFORM, "1", person, personId, task, taskId, data);

        if (this.encryptionEnabled) {
            try {
                message = this.messageEncryptor.encrypt(message, this.encryptionPassword);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        if (this.botCommunicationController.publish(message)) {
            LOG.info("Send message to bot: " + message);
        } else {
            LOG.error("Failed to send message to bot");
        }
    }

    public void handle(String message) {
        if (this.encryptionEnabled) {
            try {
                message = this.messageEncryptor.decrypt(message, this.encryptionPassword);
            } catch (Exception e) {
                LOG.error("Failed to decrypt message: " + message);
                return;
            }
        }

        Message botMessage;
        try {
            botMessage = this.messageParser.parse(message);
        } catch (Exception e) {
            LOG.error("Failed to parse message: " + message);
            return;
        }

        if (botMessage.getSender() != Sender.BOT) {
            return;
        }

        this.handleBotMessage(botMessage);
    }

    public void handleBotMessage(Message message) {

        switch (message.getTask()) {
            case FEEDBACK_STATUS -> {
                LOG.info("New feedback status: " + message.getData());
                Response response = this.databaseService.saveFeedback(Feedback.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .status(message.getData())
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.FEEDBACK_ID, response.getId(), response.getId());
                }
            }
            case FEEDBACK_EXPLANATION -> {
                LOG.info("New feedback explanation: " + message.getData());
                Response response = this.databaseService.saveFeedback(Feedback.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .explanation(message.getData())
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.FEEDBACK_ID, response.getId(), response.getId());
                }
            }
            case FEEDBACK_TIMESTAMP -> {
                LOG.info("New feedback timestamp: " + message.getData());
                Response response = this.databaseService.saveFeedback(Feedback.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .timestamp(LocalDateTime.ofEpochSecond(Long.parseLong(message.getData()), 0, ZoneOffset.UTC))
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.FEEDBACK_ID, response.getId(), response.getId());
                }
            }
            case MEAL_ORDER_MEAL_ID -> {
                LOG.info("New meal order meal: " + message.getData());
                Response response = this.databaseService.saveMealOrder(MealOrder.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .mealId(message.getData())
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.MEAL_ORDER_ID, response.getId(), response.getId());
                }
            }
            case MEAL_ORDER_TIMESTAMP -> {
                LOG.info("New meal order timestamp: " + message.getData());
                Response response = this.databaseService.saveMealOrder(MealOrder.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .timestamp(LocalDateTime.ofEpochSecond(Long.parseLong(message.getData()), 0, ZoneOffset.UTC))
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.MEAL_ORDER_ID, response.getId(), response.getId());
                }
            }
            case QUESTION_ID -> {
                LOG.info("New question ids request: " + message.getData());

                if (message.getPersonId().equals("-1")) {
                    break;
                }

                Set<String> ids = this.databaseService.findUnansweredQuestionIds(message.getPersonId());
                this.sendIds(Person.PATIENT, message.getPersonId(), Task.QUESTION_ID, message.getTaskId(), ids);
            }
            case QUESTION -> {
                LOG.info("New question request: " + message.getData());

                if (message.getTaskId().equals("-1")) {
                    break;
                }

                this.sendQuestion(this.databaseService.findQuestion(message.getTaskId()));
            }
            case ANSWER_QUESTION_ID -> {
                LOG.info("New answer question id: " + message.getData());
                Response response = this.databaseService.saveAnswer(Answer.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .questionId(message.getData())
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.ANSWER_ID, response.getId(), response.getId());
                }
            }
            case ANSWER_TEXT -> {
                LOG.info("New answer text: " + message.getData());
                Response response = this.databaseService.saveAnswer(Answer.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .text(message.getData())
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.ANSWER_ID, response.getId(), response.getId());
                }
            }
            case ANSWER_TIMESTAMP -> {
                LOG.info("New answer timestamp: " + message.getData());
                Response response = this.databaseService.saveAnswer(Answer.builder()
                        .id(message.getTaskId())
                        .patientId(message.getPersonId())
                        .timestamp(LocalDateTime.ofEpochSecond(Long.parseLong(message.getData()), 0, ZoneOffset.UTC))
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, message.getPersonId(), Task.ANSWER_ID, response.getId(), response.getId());
                }
            }
            case REMINDER -> {
                LOG.info("New reminder: " + message.getData());
            }
            case PATIENT -> {
                LOG.info("Get patient request");

                if (message.getPerson() != Person.PATIENT) {
                    break;
                }

                if (message.getPersonId().equals("-1")) {
                    break;
                }

                this.sendPatient(this.databaseService.findPatient(message.getPersonId()));
            }
            case PATIENT_ID -> {
                LOG.info("Get patients id request");

                if (!message.getPersonId().equals("-1")) {
                    break;
                }

                Set<String> ids = this.databaseService.findPatientIds(LocalDate.ofEpochDay(Long.parseLong(message.getData())));
                this.sendIds(Person.PATIENT, "-1", Task.PATIENT_ID, message.getTaskId(), ids);
            }
            case PATIENT_NAME -> {
                LOG.info("New patient name: " + message.getData());
                Response response = this.databaseService.savePatient(Patient.builder()
                        .id(message.getPersonId())
                        .name(message.getData())
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, response.getId(), Task.PATIENT_ID, message.getTaskId(), response.getId());
                }
            }
            case PATIENT_BIRTHDATE -> {
                LOG.info("New patient birthdate: " + message.getData());
                Response response = this.databaseService.savePatient(Patient.builder()
                        .id(message.getPersonId())
                        .birthdate(LocalDate.ofEpochDay(Long.parseLong(message.getData())))
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, response.getId(), Task.PATIENT_ID, message.getTaskId(), response.getId());
                }
            }
            case PATIENT_ALLERGIES -> {
                LOG.info("New patient allergies: " + message.getData());
                Response response = this.databaseService.savePatient(Patient.builder()
                        .id(message.getPersonId())
                        .allergies(new HashSet<>(Arrays.asList(Allergy.valueOf(message.getData()))))
                        .build());

                if (response.isNew()) {
                    this.sendId(Person.PATIENT, response.getId(), Task.PATIENT_ID, message.getTaskId(), response.getId());
                }
            }
            default -> {
                LOG.error("Unknown command: " + message.getTask());
            }
        }

    }

    public void sendQuestion(Question question) {
        if (question == null) {
            return;
        }

        this.send(Person.PATIENT, question.getPatientId(), Task.QUESTION_TEXT, question.getId(), question.getText());
        this.send(Person.PATIENT, question.getPatientId(), Task.QUESTION_TIMESTAMP, question.getId(), String.valueOf(question.getTimestamp().toEpochSecond(ZoneOffset.UTC)));
    }

    public void sendPatient(Patient patient) {
        if (patient == null) {
            return;
        }

        this.send(Person.PATIENT, patient.getId(), Task.PATIENT_NAME, patient.getId(), patient.getName());
        this.send(Person.PATIENT, patient.getId(), Task.PATIENT_BIRTHDATE, patient.getId(), String.valueOf(patient.getBirthdate().toEpochDay()));
        this.send(Person.PATIENT, patient.getId(), Task.PATIENT_ALLERGIES, patient.getId(), String.valueOf(patient.getAllergies() == null ? new HashSet<>() : patient.getAllergies()));
    }

    public void sendIds(Person person, String personId, Task task, String taskId, Set<String> ids) {
        this.send(person, personId, task, taskId, String.valueOf(ids));
    }

    public void sendId(Person person, String personId, Task task, String taskId, String id) {
        this.send(person, personId, task, taskId, id);
    }

}
