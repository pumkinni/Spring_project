package com.example.account.domain;


import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Account {

    @Id
    @GeneratedValue
    private long id;
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

}
