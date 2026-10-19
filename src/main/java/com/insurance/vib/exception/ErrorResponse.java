package com.insurance.vib.exception;

import java.util.List;

public record ErrorResponse(int status, String message, List<String> errors) {
}
