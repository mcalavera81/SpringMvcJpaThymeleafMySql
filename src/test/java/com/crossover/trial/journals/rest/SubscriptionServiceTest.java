package com.crossover.trial.journals.rest;

import com.crossover.trial.journals.Application;
import com.crossover.trial.journals.model.Category;
import com.crossover.trial.journals.model.Journal;
import com.crossover.trial.journals.model.Publisher;
import com.crossover.trial.journals.model.User;
import com.crossover.trial.journals.repository.CategoryRepository;
import com.crossover.trial.journals.repository.PublisherRepository;
import com.crossover.trial.journals.repository.SubscriptionRepository;
import com.crossover.trial.journals.repository.UserRepository;
import com.crossover.trial.journals.service.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Transactional
public class SubscriptionServiceTest extends BaseTest{

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private JournalService journalService;

    @Autowired
    private JavaMailSender javaMailSender;


    @Mock
    private JavaMailSender javaMailSenderMock;

    @Mock
    private MimeMessage mimeMessage;

    private User publisher1, publisher2, user1, user2;
    private Category category1, category2, category3, category4;


    @Before
    public void initAll(){
        publisher1 = userService.getUserByLoginName("publisher1").get();
        publisher2 = userService.getUserByLoginName("publisher2").get();
        user1 = userService.getUserByLoginName("user1").get();
        user2 = userService.getUserByLoginName("user2").get();


        category1= categoryRepository.findOne(1L);
        category2= categoryRepository.findOne(2L);
        category3= categoryRepository.findOne(3L);
        category4= categoryRepository.findOne(4L);

        MockitoAnnotations.initMocks(this);

        subscriptionService.overrideJavaMailSender(javaMailSenderMock);

    }
    @Test
    public void browseSubscribedUser() {


        userService.subscribe(user1, category1.getId());
        userService.subscribe(user2, category1.getId());
        userService.subscribe(publisher1, category1.getId());

        userService.subscribe(user1, category2.getId());
        userService.subscribe(user2, category2.getId());

        userService.subscribe(publisher1, category3.getId());

        Set<User> usersSubscribedTo;
        usersSubscribedTo = subscriptionService.findUsersSubscribedTo(category1.getId());
        Assert.assertTrue(usersSubscribedTo.containsAll(Arrays.asList(user1, user2, publisher1)));

        usersSubscribedTo = subscriptionService.findUsersSubscribedTo(category2.getId());
        Assert.assertTrue(usersSubscribedTo.containsAll(Arrays.asList(user1, user2)));

        usersSubscribedTo = subscriptionService.findUsersSubscribedTo(category3.getId());
        Assert.assertTrue(usersSubscribedTo.containsAll(Arrays.asList(publisher1)));

        usersSubscribedTo = subscriptionService.findUsersSubscribedTo(category3.getId());
        Assert.assertTrue(usersSubscribedTo.containsAll(Arrays.asList(publisher1)));

        usersSubscribedTo = subscriptionService.findUsersSubscribedTo(category4.getId());
        Assert.assertTrue(usersSubscribedTo.size()==0);

    }


    @Test
    public void testNotifyNewJournals() throws MessagingException {

        User user = getUser("publisher2");
        Publisher publisher = publisherRepository.findByUser(user).get();

        Journal journal = new Journal();
        journal.setName("Journal");
        journal.setUuid("SOME_EXTERNAL_ID");

        long categoryId =3L;
        Category category = categoryRepository.findOne(categoryId);

        try {
            journalService.publish(publisher, journal, category.getId());
        } catch (ServiceException e) {
            fail(e.getMessage());
        }

        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);

        Assert.assertTrue(!journalService.findNewJournals().isEmpty());
        Mockito.when(javaMailSenderMock.createMimeMessage()).thenReturn(mimeMessage);

        subscriptionService.notifyNewJournals();

        Assert.assertTrue(journalService.findNewJournals().isEmpty());
    }
}
