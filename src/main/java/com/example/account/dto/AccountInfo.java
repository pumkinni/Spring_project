package com.example.account.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// client 와 controller 간의 응답
public class AccountInfo {
    private String accountNumber;
    private Long balance;

}
