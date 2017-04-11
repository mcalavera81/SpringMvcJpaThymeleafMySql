package com.crossover.trial.journals.rest;

import com.crossover.trial.journals.Application;
import com.crossover.trial.journals.model.Publisher;
import com.crossover.trial.journals.model.User;
import com.crossover.trial.journals.repository.PublisherRepository;
import com.crossover.trial.journals.service.JournalService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@Transactional
public class PublisherControllerTest extends BaseTest{
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private JournalService journalService;

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

    }

    @Test
    @WithUserDetails("publisher1")
    public void testHandleFileUpload() throws Exception {

        User publisher1 = getUser("publisher1");
        Publisher publisher = publisherRepository.findByUser(publisher1).get();

        Assert.assertEquals(2,journalService.publisherList(publisher).size());

        MockMultipartFile file = new MockMultipartFile("file", "filename.txt",
                "application/pdf", "some pdf".getBytes());

        MvcResult mvcResult = mockMvc.perform(fileUpload("/publisher/publish").file(file)
                .param("name", "filename.txt")
                .param("category", "1"))
                .andExpect(status().is3xxRedirection())
                .andReturn();


        Assert.assertEquals(3,journalService.publisherList(publisher).size());



    }

    @WithUserDetails("publisher1")
    @Test
    public void testHandleFileUploadEmpty() throws Exception {

        User publisher1 = getUser("publisher1");
        Publisher publisher = publisherRepository.findByUser(publisher1).get();

        int sizeBefore= journalService.publisherList(publisher).size();

        MockMultipartFile file = new MockMultipartFile("file", "filename.txt",
                "application/pdf", (byte[])null);

        MvcResult mvcResult = mockMvc.perform(fileUpload("/publisher/publish").file(file)
                .param("name", "filename.txt")
                .param("category", "1"))
                .andExpect(status().is3xxRedirection())
                .andReturn();


        Assert.assertEquals(sizeBefore,journalService.publisherList(publisher).size());

    }
}
