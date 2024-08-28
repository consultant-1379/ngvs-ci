package com.ericsson.bss.util

class Email {

    private static final String PROJECT_DEFAULT_SUBJECT = '$PROJECT_DEFAULT_SUBJECT'

    private String recipient
    private String subject
    private String content
    private String failureTriggerSubject
    private String fixedTriggerSubject
    private String unstableTriggerSubject
    private String abortedTriggerSubject
    private String alwaysTriggerSubject
    private String statusChangedTriggerSubject

    private Email(Builder builder) {
        this.recipient = builder.recipient
        this.subject = builder.subject
        this.content = builder.content
        this.failureTriggerSubject = builder.failureTriggerSubject
        this.fixedTriggerSubject = builder.fixedTriggerSubject
        this.unstableTriggerSubject = builder.unstableTriggerSubject
        this.abortedTriggerSubject = builder.abortedTriggerSubject
        this.alwaysTriggerSubject = builder.alwaysTriggerSubject
        this.statusChangedTriggerSubject = builder.statusChangedTriggerSubject
    }

    public static Builder newBuilder() {
        return new Builder()
    }

    public String getRecipient() {
        return recipient
    }

    public String getSubject() {
        return subject
    }

    public String getContent() {
        return content
    }

    public String getFailureTriggerSubject() {
        return failureTriggerSubject
    }

    public String getFixedTriggerSubject() {
        return fixedTriggerSubject
    }

    public String getUnstableTriggerSubject() {
        return unstableTriggerSubject
    }

    public String getAbortedTriggerSubject() {
        return abortedTriggerSubject
    }

    public String getAlwaysTriggerSubject() {
        return alwaysTriggerSubject
    }

    public String getStatusChangedTriggerSubject() {
        return statusChangedTriggerSubject
    }

    private static class Builder {
        private String recipient
        private String subject
        private String content
        private String failureTriggerSubject
        private String fixedTriggerSubject
        private String unstableTriggerSubject
        private String abortedTriggerSubject
        private String alwaysTriggerSubject
        private String statusChangedTriggerSubject

        private Builder() {
        }

        public Email build() {
            return new Email(this)
        }

        public Builder withRecipient(String recipient) {
            this.recipient = recipient
            return this
        }

        public Builder withSubject(String subject) {
            this.subject = subject
            return this
        }

        public Builder withContent(String content) {
            this.content = content
            return this
        }

        public Builder withFailureTrigger(String triggerSubject = PROJECT_DEFAULT_SUBJECT) {
            this.failureTriggerSubject = triggerSubject
            return this
        }

        public Builder withFixedTrigger(String triggerSubject = PROJECT_DEFAULT_SUBJECT) {
            this.fixedTriggerSubject = triggerSubject
            return this
        }

        public Builder withUnstableTrigger(String triggerSubject = PROJECT_DEFAULT_SUBJECT) {
            this.unstableTriggerSubject = triggerSubject
            return this
        }

        public Builder withAbortedTrigger(String triggerSubject = PROJECT_DEFAULT_SUBJECT) {
            this.abortedTriggerSubject = triggerSubject
            return this
        }

        public Builder withAlwaysTrigger(String triggerSubject = PROJECT_DEFAULT_SUBJECT) {
            this.alwaysTriggerSubject = triggerSubject
            return this
        }

        public Builder withStatusChangedTrigger(String triggerSubject = PROJECT_DEFAULT_SUBJECT) {
            this.statusChangedTriggerSubject = triggerSubject
            return this
        }
    }
}
