package com.crossover.trial.journals.service;

import com.crossover.trial.journals.model.Category;
import com.crossover.trial.journals.model.Journal;
import com.crossover.trial.journals.model.Subscription;
import com.crossover.trial.journals.model.User;
import com.crossover.trial.journals.repository.SubscriptionRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final Logger logger = Logger.getLogger(this.getClass());

    private SubscriptionRepository subscriptionRepository;

    private JournalService journalService;

    private UserService userService;

    private JavaMailSender javaMailSender;

    @Override
    public void overrideJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Value("${email.subject.subscription}")
    private String subjectSubscription;

    @Value("${email.subject.newJournals}")
    private String subjectNewJournals;

    @Autowired
    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            JournalService journalService,
            UserService userService,
            JavaMailSender javaMailSender
            ){
        this.subscriptionRepository = subscriptionRepository;
        this.javaMailSender = javaMailSender;
        this.journalService = journalService;
        this.userService = userService;
    }



    @Override
    public Set<User> findUsersSubscribedTo(Long categoryId) {

        List<Subscription> subscriptions = subscriptionRepository.findByCategoryId(categoryId);
        return subscriptions.stream().map(subs-> subs.getUser()).collect(Collectors.toSet());

    }

    @Override
    @Async
    public void notifySubscribedUsers(Journal journal) throws MessagingException  {
        Set<User> usersSubscribedTo = findUsersSubscribedTo(journal.getCategory().getId());
        String[] emails = getEmails(usersSubscribedTo);
        if(emails.length>0){
            sendEmail(emails,subjectSubscription, journal.toString());
            logger.info(String.format("Send notification to subscribed users for journal: %s", journal.getId()));
        }
    }

    @Override
    @Scheduled(cron = "0 15 3 * * ?")
    public void notifyNewJournals()  {

        List<Journal> newJournals = journalService.findNewJournals();

        if(newJournals.size() > 0){
            List<User> allUsers = userService.findAll();
            String[] emails = getEmails(allUsers);
            String digest = createDigest(newJournals);

            try {
                sendEmail(emails, subjectNewJournals, digest);
            } catch (MessagingException e) {
                logger.info(String.format("Error sending email: %s", e.getMessage()));
            }
            journalService.markNewJournalsAsNotified();
            logger.info(String.format("Send notification to all users for all new journals"));
        }

    }

    private String[] getEmails(Collection<User> collection){
        String[] emails = collection.stream().map(User::getEmail).toArray(size -> new String[size]);
        return emails;
    }


    private String createDigest(List<Journal> journals){
        return journals.stream().map(journal -> journal.getCategory()+":"+journal.getName())
        .collect(Collectors.joining("\n"));
    }

    private void sendEmail(String[] destinations, String subject, String body) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "utf-8");

        helper.setFrom("info@crossover");
        helper.setBcc(destinations);
        helper.setSubject(subject);
        helper.setText(body);

        javaMailSender.send(msg);

    }

}
