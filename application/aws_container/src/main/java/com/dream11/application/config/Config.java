package com.dream11.application.config;

import com.dream11.application.Application;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

public interface Config {
  Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  default void validate() {
    Set<ConstraintViolation<Object>> violations = validator.validate(this);
    if (!violations.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.INVALID_CONFIG,
          this.getClass().getSimpleName(),
          violations.stream()
              .map(v -> String.format("%s %s", v.getPropertyPath(), v.getMessage()))
              .collect(Collectors.joining(",")));
    }
    Application.getObjectMapper().valueToTree(this).findParents("data").forEach(JsonNode::asText);
  }
}
