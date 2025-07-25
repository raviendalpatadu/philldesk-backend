package com.philldesk.philldeskbackend.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UserUpdateDTO {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private Boolean isActive;
    private Long roleId; // Use roleId instead of full Role object
    private String password; // Optional for password updates
}
