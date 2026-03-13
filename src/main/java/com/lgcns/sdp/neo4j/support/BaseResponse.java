package com.lgcns.sdp.neo4j.support;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor // This is legacy. Do not use Default Constructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseResponse<T> extends BaseResponseDto {
    @Schema(title = "결과데이터", description = "결과데이터", example = "{'key':'value'}")
    private T data;

    @Builder
    public BaseResponse(String resultCode, String resultMessage, T data) {
        super.setResultCode(resultCode);
        super.setResultMessage(resultMessage);
        this.data = data;
    }

    public static <T> BaseResponse<T> success() {
        return BaseResponse.<T>builder()
                .resultCode(ResultCode.SUCCESS.getCode())
                .resultMessage(ResultCode.SUCCESS.getMessage())
                .data(null)
                .build();
    }

    public static <T> BaseResponse<T> success(@NotNull T data) {
        if (data == null) {
            throw new NullPointerException("data is null");
        }

        return BaseResponse.<T>builder()
                .resultCode(ResultCode.SUCCESS.getCode())
                .resultMessage(ResultCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> of(ResultCode resultCode) {
        return BaseResponse.<T>builder()
                .resultCode(resultCode.getCode())
                .resultMessage(resultCode.getMessage())
                .data(null)
                .build();
    }

    public static <T> BaseResponse<T> of(ResultCode resultCode, @NotNull T data) {
        if (data == null) {
            throw new NullPointerException("data is null");
        }

        return BaseResponse.<T>builder()
                .resultCode(resultCode.getCode())
                .resultMessage(resultCode.getMessage())
                .data(data)
                .build();
    }

    @Deprecated
    @Override
    public void setResultCode(String resultCode) {
        super.setResultCode(resultCode);
    }

    @Deprecated
    @Override
    public void setResultMessage(String resultMessage) {
        super.setResultMessage(resultMessage);
    }

}
