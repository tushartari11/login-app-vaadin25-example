package com.zetcom.mpria.config.domain;

import java.util.List;

/** Raised by the publish gate. Carries every problem, because fixing them one round trip at a time is not a workflow. */
public class ConfigurationValidationException extends RuntimeException {

    private final transient List<ConfigurationError> errors;

    public ConfigurationValidationException(List<ConfigurationError> errors) {
        super(summarise(errors));
        this.errors = List.copyOf(errors);
    }

    public List<ConfigurationError> errors() {
        return errors;
    }

    private static String summarise(List<ConfigurationError> errors) {
        return "the configuration is not publishable: " + errors.size() + " problem(s); "
                + errors.stream().map(ConfigurationError::describe).toList();
    }
}
