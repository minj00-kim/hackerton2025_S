package com.hackerton.hackerton2025.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException() {
        super("이미 이 게시물에 리뷰를 남겼습니다.");
    }
}