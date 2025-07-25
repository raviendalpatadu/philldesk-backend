package com.philldesk.philldeskbackend.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CustomerProfileUpdateDTO {
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    // Note: username and email are typically not updatable for security reasons
}
