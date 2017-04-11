package com.crossover.trial.journals.rest;

import com.crossover.trial.journals.Application;
import com.crossover.trial.journals.model.*;
import com.crossover.trial.journals.repository.*;
import com.crossover.trial.journals.service.JournalService;
import com.crossover.trial.journals.service.ServiceException;
import com.crossover.trial.journals.service.UserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@Transactional
public class JournalRestServiceTest extends BaseTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private Category category1, category2, category3;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private JournalService journalService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        category1= categoryRepository.findOne(1L);
        category2= categoryRepository.findOne(2L);
        category3= categoryRepository.findOne(3L);


    }


    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));



    @Test
    @WithUserDetails("user2")
    public void testBrowse() throws Exception {

        journalRepository.deleteAll();

        publishJournal("journal1", 1L);
        publishJournal("journal2", 1L);
        publishJournal("journal3", 2L);
        publishJournal("journal4", 3L);

        User user2 = getUser("user2");
        List<String> journals = journalService.listAll(user2)
                .stream().map(Journal::getName).collect(Collectors.toList());

        MvcResult mvcResult = mockMvc.perform(get("/rest/journals"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(journals.size())))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(journals.toArray())))
                .andReturn();

    }


    @Test
    @WithUserDetails("user2")
    public void testUsersubscriptions() throws Exception {
        journalRepository.deleteAll();

        publishJournal("journal1", 1L);
        publishJournal("journal2", 1L);
        publishJournal("journal3", 2L);
        publishJournal("journal4", 3L);

        User user2 = getUser("user2");

        userService.subscribe(user2, category1.getId());
        userService.subscribe(user2, category3.getId());

        MvcResult mvcResult = mockMvc.perform(get("/rest/journals/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].active", is(false)))
                .andExpect(jsonPath("$[2].id", is(3)))
                .andExpect(jsonPath("$[2].active", is(true)))
                .andExpect(jsonPath("$[3].id", is(4)))
                .andExpect(jsonPath("$[3].active", is(false)))
                .andExpect(jsonPath("$[4].id", is(5)))
                .andExpect(jsonPath("$[4].active", is(false)))
                .andReturn();

    }

    @Test
    @WithUserDetails("user2")
    public void testSubscribe() throws Exception {
        User user2;
        user2 = getUser("user2");

        int subscriptions = user2.getSubscriptions().size();

        mockMvc.perform(post("/rest/journals/subscribe/5"))
                .andExpect(status().isOk());

        user2 = getUser("user2");
        Assert.assertEquals(subscriptions+1, user2.getSubscriptions().size());


    }

    @WithUserDetails("publisher1")
    @Test
    public void testPublishedList() throws Exception {

        Publisher publisher = publisherRepository.findByUser(getUser("publisher1")).get();
        List<String> names = journalService.publisherList(publisher)
                .stream().map(Journal::getName).collect(Collectors.toList());


        MvcResult mvcResult = mockMvc.perform(get("/rest/journals/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(names.size())))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(names.toArray())))
                .andReturn();



    }

    @WithUserDetails("publisher1")
    @Test
    public void testUnPublish() throws Exception {
        User publisher1 = getUser("publisher1");
        Publisher publisher = publisherRepository.findByUser(publisher1).get();


        Assert.assertEquals(2,journalService.publisherList(publisher).size());

        mockMvc.perform(delete("/rest/journals//unPublish/1"))
                .andExpect(status().isOk());

        Assert.assertEquals(1, journalService.publisherList(publisher).size());

    }

    private void publishJournal(String name, Long categoryId){
        User user = getUser("publisher1");
        Optional<Publisher> p = publisherRepository.findByUser(user);

        Journal journal = new Journal();
        journal.setName(name);
        journal.setUuid("UUID");
        try {
            journalService.publish(p.get(), journal, categoryId);
        } catch (ServiceException e) {
            fail(e.getMessage());
        }
    }


}
