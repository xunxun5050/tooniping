package com.webtoonhub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationVerifyRequest(
    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해 주세요.")
    String email,
    @NotBlank(message = "인증번호를 입력해 주세요.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.")
    String code
) {
}
