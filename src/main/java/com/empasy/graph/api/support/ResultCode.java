package com.empasy.graph.api.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS("00", "Success"),
    AUTH_FAIL("02", "Auth Fail"),
    INVALID_PARAMETER("03", "Invalid Parameter"),
    NO_DATA("05", "No Data"),
    DATA_ERROR("06", "Data Error"),
    REQUEST_EXCEEDED("20", "Number of Requests Exceeded"),
    PARTIAL_ERROR("21", "Failed to process data (Partial)"),
    ALL_ERROR("22", "Failed to process data (All)"),
    DB_ERROR("99", "DB Error"),
    ETC_ERROR("99", "Etc Error"),
    UPDATE_ITEM_ERROR("99", "Update Item Error"),
    NO_EMAIL("100", "User does not have email"),
    NO_SMS("101", "User does not have mobile number"),
    INCORRECT_OTP("102", "OTP is incorrect"),
    EXPIRED_OTP("103", "OTP was expired"),
    SEND_OTP_ERROR("104", "Failed to send OTP");

    private final String code;
    private final String message;
}
