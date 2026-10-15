package com.starrainnotes.account.service;

import com.starrainnotes.account.vo.LoginReservation;

/** Atomically reserves short lived source and identity budgets before authentication work. */
public interface AccountThrottleService {
    /** Username and email aliases of an existing account share one budget. */
    LoginReservation reserveLogin(String ip, String identifier);

    /** Successful authentication releases its own reservations, never other pending attempts. */
    void loginSucceeded(LoginReservation reservation);

    /** Counts every reset request, including unknown and disabled accounts. */
    void reserveReset(String ip, String email);
}
