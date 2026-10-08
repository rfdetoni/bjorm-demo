package com.github.rfdetoni.bjorm.demo.api;

import com.github.rfdetoni.bjorm.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ProblemDetail> notFound(NoSuchElementException error) {
        return response(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(OptimisticLockException.class)
    ResponseEntity<ProblemDetail> conflict(OptimisticLockException error) {
        return response(HttpStatus.CONFLICT, "Produto alterado ou removido por outro usuário. Recarregue a lista.");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> badRequest(Exception error) {
        return response(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).distinct()
            .reduce((a, b) -> a + "; " + b).orElse("Dados inválidos");
        return response(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatus code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code, detail);
        return ResponseEntity.status(code).body(problem);
    }
}
