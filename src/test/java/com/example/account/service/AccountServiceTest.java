package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock // 의존성 있는 변수 가짜 생성
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks  // 위의 가짜 생성 목 주입 accountRepository -> accountService
    private AccountService accountService;

    // 계좌 생성 테스트

    @Test
    void createAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012")
                        .build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());
        // accountNumber가 1씩 증가하는지 확인
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }


    // 계좌 첫 생성 테스트
    @Test
    void createFirstAccount() {
        //given
        AccountUser user = AccountUser.builder()
                .id(18L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());  // OPTIONAL이 비었음
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());
        // accountNumber가 1씩 증가하는지 확인
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(18L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    // user가 없는 경우 (create)
    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(18L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class, () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }


    // 계좌 해지 테스트
    @Test
    @DisplayName("유저 당 최대 계좌는 10개")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder()
                .id(18L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class, () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());

    }

    @Test
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    // user가 없는 경우 (delete)
    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccountFailed_UserNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(18L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    // 계좌가 없는 경우 (delete)
    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccountFailed_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 해지 실패")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser pororo = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();
        AccountUser dory = AccountUser.builder()
                .id(13L)
                .name("Dory")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pororo));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(dory)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 해지 - 계좌 해지 실패")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser pororo = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pororo));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pororo)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 잔액 남음 - 계좌 해지 실패")
    void deleteAccountFailed_balanceNotEmpty() {
        //given
        AccountUser pororo = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pororo));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pororo)
                        .balance(1000L)
                        .accountNumber("1000000012")
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    void successGetAccountByUserId() {
        //given
        AccountUser pororo = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountNumber("1111111111")
                        .accountUser(pororo)
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountNumber("2222222222")
                        .accountUser(pororo)
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountNumber("3333333333")
                        .accountUser(pororo)
                        .balance(3000L)
                        .build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pororo));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);
        //then
        assertEquals(accountDtos.size(), 3);
        assertEquals(accountDtos.get(0).getAccountNumber(), "1111111111");
        assertEquals(accountDtos.get(0).getBalance(), 1000L);
        assertEquals(accountDtos.get(1).getAccountNumber(), "2222222222");
        assertEquals(accountDtos.get(1).getBalance(), 2000L);
        assertEquals(accountDtos.get(2).getAccountNumber(), "3333333333");
        assertEquals(accountDtos.get(2).getBalance(), 3000L);
    }

    @Test
    void failedToGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(100L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

}