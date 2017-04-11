package com.crossover.trial.journals.rest;

import com.crossover.trial.journals.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
@RestController
public class NotificationRestController
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private SubscriptionService subscriptionService;

    @Autowired
    public NotificationRestController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }


    @RequestMapping("/notifyNewJournals")
    public void sendEmail(){
        try {
            subscriptionService.notifyNewJournals();
        } catch (MessagingException e) {
            logger.error(String.format("There has been an error with the email sending: %s", e.getMessage()));
        }
    }
}
