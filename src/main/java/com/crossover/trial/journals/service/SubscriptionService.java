package com.crossover.trial.journals.service;

import com.crossover.trial.journals.model.Category;
import com.crossover.trial.journals.model.Journal;
import com.crossover.trial.journals.model.User;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.MessagingException;
import java.util.List;
import java.util.Set;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
public interface SubscriptionService {

    void overrideJavaMailSender(JavaMailSender javaMailSender);

    Set<User> findUsersSubscribedTo(Long categoryId);

    void notifySubscribedUsers(Journal journal) throws MessagingException;

    void notifyNewJournals() throws MessagingException;
}
