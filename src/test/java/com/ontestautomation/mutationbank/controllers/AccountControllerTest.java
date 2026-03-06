package com.ontestautomation.mutationbank.controllers;

import com.ontestautomation.mutationbank.dto.AccountCreateUpdate;
import com.ontestautomation.mutationbank.models.AccountType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerTest {

    @LocalServerPort
    private int port;

    private RequestSpecification requestSpec;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setBasePath("/account")
                .build();
    }

    private long createAccount(AccountType type, double balance) {
        AccountCreateUpdate payload = new AccountCreateUpdate(type, balance);
        return given(requestSpec)
                .body(payload)
                .post()
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    // --- POST /account ---

    @Test
    void createCheckingAccount_returns201WithAccountDetails() {
        AccountCreateUpdate payload = new AccountCreateUpdate(AccountType.CHECKING, 500.0);

        given(requestSpec)
                .body(payload)
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("type", equalTo("CHECKING"))
                .body("balance", equalTo(500.0f));
    }

    @Test
    void createSavingsAccount_returns201WithAccountDetails() {
        AccountCreateUpdate payload = new AccountCreateUpdate(AccountType.SAVINGS, 1500.0);

        given(requestSpec)
                .body(payload)
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("type", equalTo("SAVINGS"))
                .body("balance", equalTo(1500.0f));
    }

    // --- GET /account/{id} ---

    @Test
    void getAccount_existingId_returns200WithAccountDetails() {
        long id = createAccount(AccountType.CHECKING, 200.0);

        given(requestSpec)
                .get("/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("type", equalTo("CHECKING"))
                .body("balance", equalTo(200.0f));
    }

    @Test
    void getAccount_nonExistingId_returns404() {
        given(requestSpec)
                .get("/{id}", Long.MAX_VALUE)
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }

    // --- GET /account ---

    @Test
    void getAllAccounts_whenAccountsExist_returns200WithList() {
        createAccount(AccountType.CHECKING, 100.0);

        given(requestSpec)
                .get()
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    // --- DELETE /account/{id} ---

    @Test
    void deleteAccount_existingId_returns204() {
        long id = createAccount(AccountType.CHECKING, 100.0);

        given(requestSpec)
                .delete("/{id}", id)
                .then()
                .statusCode(204);
    }

    @Test
    void deleteAccount_thenGetReturns404() {
        long id = createAccount(AccountType.CHECKING, 100.0);

        given(requestSpec).delete("/{id}", id);

        given(requestSpec)
                .get("/{id}", id)
                .then()
                .statusCode(404);
    }

    // --- POST /account/{id}/deposit/{amount} ---

    @Test
    void deposit_positiveAmount_updatesBalance() {
        long id = createAccount(AccountType.CHECKING, 500.0);

        given(requestSpec)
                .post("/{id}/deposit/{amount}", id, 200.0)
                .then()
                .statusCode(200)
                .body("balance", equalTo(700.0f));
    }

    @Test
    void deposit_zeroAmount_returns400() {
        long id = createAccount(AccountType.CHECKING, 500.0);

        given(requestSpec)
                .post("/{id}/deposit/{amount}", id, 0.0)
                .then()
                .statusCode(400)
                .body("message", containsString("Amount must be greater than 0"));
    }

    @Test
    void deposit_negativeAmount_returns400() {
        long id = createAccount(AccountType.CHECKING, 500.0);

        given(requestSpec)
                .post("/{id}/deposit/{amount}", id, -50.0)
                .then()
                .statusCode(400)
                .body("message", containsString("Amount must be greater than 0"));
    }

    @Test
    void deposit_nonExistingAccount_returns404() {
        given(requestSpec)
                .post("/{id}/deposit/{amount}", Long.MAX_VALUE, 100.0)
                .then()
                .statusCode(404);
    }

    // --- POST /account/{id}/withdraw/{amount} ---

    @Test
    void withdraw_positiveAmount_fromCheckingAccount_updatesBalance() {
        long id = createAccount(AccountType.CHECKING, 500.0);

        given(requestSpec)
                .post("/{id}/withdraw/{amount}", id, 200.0)
                .then()
                .statusCode(200)
                .body("balance", equalTo(300.0f));
    }

    @Test
    void withdraw_positiveAmount_fromSavingsAccount_withSufficientFunds_updatesBalance() {
        long id = createAccount(AccountType.SAVINGS, 500.0);

        given(requestSpec)
                .post("/{id}/withdraw/{amount}", id, 200.0)
                .then()
                .statusCode(200)
                .body("balance", equalTo(300.0f));
    }

    @Test
    void withdraw_fromCheckingAccount_allowsOverdraft() {
        long id = createAccount(AccountType.CHECKING, 100.0);

        given(requestSpec)
                .post("/{id}/withdraw/{amount}", id, 300.0)
                .then()
                .statusCode(200)
                .body("balance", equalTo(-200.0f));
    }

    @Test
    void withdraw_fromSavingsAccount_withInsufficientFunds_returns400() {
        long id = createAccount(AccountType.SAVINGS, 100.0);

        given(requestSpec)
                .post("/{id}/withdraw/{amount}", id, 300.0)
                .then()
                .statusCode(400)
                .body("message", containsString("Insufficient funds"));
    }

    @Test
    void withdraw_zeroAmount_returns400() {
        long id = createAccount(AccountType.SAVINGS, 500.0);

        given(requestSpec)
                .post("/{id}/withdraw/{amount}", id, 0.0)
                .then()
                .statusCode(400)
                .body("message", containsString("Amount must be greater than 0"));
    }

    @Test
    void withdraw_negativeAmount_returns400() {
        long id = createAccount(AccountType.SAVINGS, 500.0);

        given(requestSpec)
                .post("/{id}/withdraw/{amount}", id, -100.0)
                .then()
                .statusCode(400)
                .body("message", containsString("Amount must be greater than 0"));
    }

    @Test
    void withdraw_nonExistingAccount_returns404() {
        given(requestSpec)
                .post("/{id}/withdraw/{amount}", Long.MAX_VALUE, 100.0)
                .then()
                .statusCode(404);
    }

    // --- POST /account/{id}/interest ---

    @Test
    void addInterest_savingsAccountBalanceBelow1000_applies1PercentInterest() {
        long id = createAccount(AccountType.SAVINGS, 500.0);

        given(requestSpec)
                .post("/{id}/interest", id)
                .then()
                .statusCode(200)
                .body("balance", equalTo(505.0f));
    }

    @Test
    void addInterest_savingsAccountBalanceBetween1000And5000_applies2PercentInterest() {
        long id = createAccount(AccountType.SAVINGS, 2000.0);

        given(requestSpec)
                .post("/{id}/interest", id)
                .then()
                .statusCode(200)
                .body("balance", equalTo(2040.0f));
    }

    @Test
    void addInterest_savingsAccountBalanceAtOrAbove5000_applies3PercentInterest() {
        long id = createAccount(AccountType.SAVINGS, 5000.0);

        given(requestSpec)
                .post("/{id}/interest", id)
                .then()
                .statusCode(200)
                .body("balance", equalTo(5150.0f));
    }

    @Test
    void addInterest_checkingAccount_returns400() {
        long id = createAccount(AccountType.CHECKING, 1000.0);

        given(requestSpec)
                .post("/{id}/interest", id)
                .then()
                .statusCode(400)
                .body("message", containsString("Cannot add interest to checking account"));
    }

    @Test
    void addInterest_nonExistingAccount_returns404() {
        given(requestSpec)
                .post("/{id}/interest", Long.MAX_VALUE)
                .then()
                .statusCode(404);
    }
}
