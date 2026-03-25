package com.empasy.graph.api.support;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

public class BaseResponseDto {
    @Schema(
            title = "결과코드",
            description = "결과코드",
            example = "00"
    )
    private String resultCode;
    @Schema(
            title = "결과메시지",
            description = "결과메시지",
            example = "Success"
    )
    private String resultMessage;

    @Generated
    public String getResultCode() {
        return this.resultCode;
    }

    @Generated
    public String getResultMessage() {
        return this.resultMessage;
    }

    @Generated
    public void setResultCode(final String resultCode) {
        this.resultCode = resultCode;
    }

    @Generated
    public void setResultMessage(final String resultMessage) {
        this.resultMessage = resultMessage;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BaseResponseDto)) {
            return false;
        } else {
            BaseResponseDto other = (BaseResponseDto) o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$resultCode = this.getResultCode();
                Object other$resultCode = other.getResultCode();
                if (this$resultCode == null) {
                    if (other$resultCode != null) {
                        return false;
                    }
                } else if (!this$resultCode.equals(other$resultCode)) {
                    return false;
                }

                Object this$resultMessage = this.getResultMessage();
                Object other$resultMessage = other.getResultMessage();
                if (this$resultMessage == null) {
                    if (other$resultMessage != null) {
                        return false;
                    }
                } else if (!this$resultMessage.equals(other$resultMessage)) {
                    return false;
                }

                return true;
            }
        }
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof BaseResponseDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $resultCode = this.getResultCode();
        result = result * 59 + ($resultCode == null ? 43 : $resultCode.hashCode());
        Object $resultMessage = this.getResultMessage();
        result = result * 59 + ($resultMessage == null ? 43 : $resultMessage.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        String var10000 = this.getResultCode();
        return "BaseResponseDto(resultCode=" + var10000 + ", resultMessage=" + this.getResultMessage() + ")";
    }

}