package com.camploop.model.enums;

public enum RequestStatus {
    PENDING,    // buyer asked, seller hasn't acted
    SOLD,       // seller marked the item sold to THIS buyer
    DECLINED,   // seller declined this buyer
    CANCELLED,  // buyer withdrew the request
    CLOSED      // item was sold to someone else / became unavailable
}
