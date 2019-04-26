package com.door43.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class EmailReporterTest {

    @Test
    public void generatePayloadWithUserEmail() throws Exception {
        EmailReporter reporter = new EmailReporter("token", "helpdeskemail");
        String payload = reporter.generatePayload("name", "email", "hello\nworld", "General Feedback");
        assertEquals("{\"personalizations\":[{\"subject\":\"tS Android: General Feedback\",\"to\":[{\"name\":\"Help Desk\",\"email\":\"helpdeskemail\"}]}],\"reply_to\":{\"name\":\"name\",\"email\":\"email\"},\"from\":{\"name\":\"name\",\"email\":\"email\"},\"content\":[{\"type\":\"text/plain\",\"value\":\"hello\\nworld\"},{\"type\":\"text/html\",\"value\":\"hello<br>world\"}]}", payload);
    }

    @Test
    public void generatePayloadWithoutUserEmail() throws Exception {
        EmailReporter reporter = new EmailReporter("token", "helpdeskemail");
        String payload = reporter.generatePayload("name", null, "message", "General Feedback");
        assertEquals("{\"personalizations\":[{\"subject\":\"tS Android: General Feedback\",\"to\":[{\"name\":\"Help Desk\",\"email\":\"helpdeskemail\"}]}],\"reply_to\":{\"name\":\"Help Desk\",\"email\":\"helpdeskemail\"},\"from\":{\"name\":\"Help Desk\",\"email\":\"helpdeskemail\"},\"content\":[{\"type\":\"text/plain\",\"value\":\"message\"},{\"type\":\"text/html\",\"value\":\"message\"}]}", payload);
    }
}
