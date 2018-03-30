package ru.sbt.jschool.session3.problem1;

import java.util.*;

/**
 */
public class AccountServiceImpl implements AccountService {
    private FraudMonitoring fraudMonitoring;

    private Map<Long, Account> accountHashMap = new HashMap<>();
    private Map<Long, Payment> paymentMap = new HashMap<>();
    public AccountServiceImpl(FraudMonitoring fraudMonitoring) {
        this.fraudMonitoring = fraudMonitoring;
    }

    @Override public Result create(long clientID, long accountID, float initialBalance, Currency currency) {
        if (fraudMonitoring.check(clientID)) {
            return Result.FRAUD;
        }
        if (find(accountID) != null) {
            return Result.ALREADY_EXISTS;
        }
        Account account = new Account(clientID, accountID, currency, initialBalance);
        accountHashMap.put(accountID, account);
        return Result.OK;
    }

    @Override public List<Account> findForClient(long clientID) {
        List<Account> findAccounts = new LinkedList<>();
        for (Account value : accountHashMap.values()) {
            if (value.getClientID() == clientID) {
                findAccounts.add(value);
            }
        }
        return findAccounts;
    }

    @Override public Account find(long accountID) {
        return accountHashMap.get(accountID);
    }

    @Override public Result doPayment(Payment payment) {
        Account payerAccount = accountHashMap.get(payment.getPayerAccountID());
        Account recipientAccount = accountHashMap.get(payment.getRecipientAccountID());
        if (paymentMap.get(payment.getOperationID()) != null) {
            return Result.ALREADY_EXISTS;
        }
        if (payerAccount == null || payerAccount.getClientID() != payment.getPayerID()) {
            return Result.PAYER_NOT_FOUND;
        } if (recipientAccount == null || recipientAccount.getClientID() != payment.getRecipientID()) {
            return Result.RECIPIENT_NOT_FOUND;
        }
        if (payment.getAmount() > payerAccount.getBalance()) {
            return Result.INSUFFICIENT_FUNDS;
        }
        if (fraudMonitoring.check(payerAccount.getClientID()) ||
                fraudMonitoring.check(recipientAccount.getClientID())) {
            return Result.FRAUD;
        }
        paymentMap.put(payment.getOperationID(), payment);
        if (payerAccount.getCurrency() != recipientAccount.getCurrency()) {
            float newBalance = payerAccount.getCurrency().to(payerAccount.getBalance(), recipientAccount.getCurrency());
            float newAmount = payerAccount.getCurrency().to(payment.getAmount(), recipientAccount.getCurrency());
            payment.setAmount(newAmount);
            newBalance -= newAmount;
            payerAccount.setBalance(recipientAccount.getCurrency().to(newBalance, payerAccount.getCurrency()));
        } else {
            payerAccount.setBalance(payerAccount.getBalance() - payment.getAmount());
        }
        recipientAccount.setBalance(recipientAccount.getBalance() + payment.getAmount());
        return Result.OK;
    }
}
