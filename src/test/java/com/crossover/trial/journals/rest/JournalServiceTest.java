package com.crossover.trial.journals.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.crossover.trial.journals.Application;
import com.crossover.trial.journals.model.Category;
import com.crossover.trial.journals.model.Journal;
import com.crossover.trial.journals.model.Publisher;
import com.crossover.trial.journals.model.User;
import com.crossover.trial.journals.repository.CategoryRepository;
import com.crossover.trial.journals.repository.PublisherRepository;
import com.crossover.trial.journals.service.JournalService;
import com.crossover.trial.journals.service.ServiceException;
import com.crossover.trial.journals.service.UserService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Transactional
public class JournalServiceTest {

	private final static String NEW_JOURNAL_NAME = "New Journal";
	private final static String SOME_EXTERNAL_ID = "SOME_EXTERNAL_ID";

	@Autowired
	private JournalService journalService;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private PublisherRepository publisherRepository;

	@Test
	public void browseSubscribedUser() {
		List<Journal> journals = journalService.listAll(getUser("user1"));
		assertNotNull(journals);
		assertEquals(1, journals.size());

		assertEquals(new Long(1), journals.get(0).getId());
		assertEquals("Medicine", journals.get(0).getName());
		assertEquals(new Long(1), journals.get(0).getPublisher().getId());
		assertNotNull(journals.get(0).getPublishDate());
	}

	@Test
	public void browseUnSubscribedUser() {
		List<Journal> journals = journalService.listAll(getUser("user2"));
		assertEquals(0, journals.size());
	}

	@Test
	public void listPublisher() {
		User user = getUser("publisher1");
		Optional<Publisher> p = publisherRepository.findByUser(user);
		List<Journal> journals = journalService.publisherList(p.get());
		assertEquals(2, journals.size());

		assertEquals(new Long(1), journals.get(0).getId());
		assertEquals(new Long(2), journals.get(1).getId());

		assertEquals("Medicine", journals.get(0).getName());
		assertEquals("Test Journal", journals.get(1).getName());
		journals.stream().forEach(j -> assertNotNull(j.getPublishDate()));
		journals.stream().forEach(j -> assertEquals(new Long(1), j.getPublisher().getId()));

	}

	@Test(expected = ServiceException.class)
	public void publishFail() throws ServiceException {
		User user = getUser("publisher2");
		Optional<Publisher> p = publisherRepository.findByUser(user);

		Journal journal = new Journal();
		journal.setName("New Journal");

		journalService.publish(p.get(), journal, 1L);
	}

	@Test(expected = ServiceException.class)
	public void publishFail2() throws ServiceException {
		User user = getUser("publisher2");
		Optional<Publisher> p = publisherRepository.findByUser(user);

		Journal journal = new Journal();
		journal.setName("New Journal");

		journalService.publish(p.get(), journal, 150L);
	}

	@Test()
	public void publishSuccess() {
		User user = getUser("publisher2");
		Publisher publisher = publisherRepository.findByUser(user).get();

		Journal journal = new Journal();
		journal.setName(NEW_JOURNAL_NAME);
		journal.setUuid(SOME_EXTERNAL_ID);

		List<Journal> journalsBefore = journalService.publisherList(publisher);

		long categoryId =3L;
		Category category = categoryRepository.findOne(categoryId);

		try {
			journalService.publish(publisher, journal, category.getId());
		} catch (ServiceException e) {
			fail(e.getMessage());
		}

		List<Journal> journalsAfter = journalService.publisherList(publisher);
		journalsAfter.stream().forEach(j -> assertNotNull(j.getPublishDate()));
		journalsAfter.stream().forEach(j -> assertEquals(new Long(2), j.getPublisher().getId()));

		assertEquals(journalsAfter.size(), journalsBefore.size()+1);

		journalsAfter.removeAll(journalsBefore);

		assertEquals(category.getName(), journalsAfter.get(0).getCategory().getName());
		assertEquals(NEW_JOURNAL_NAME, journalsAfter.get(0).getName());
		assertEquals(SOME_EXTERNAL_ID, journalsAfter.get(0).getUuid());
		assertEquals("N", journalsAfter.get(0).getNotified());

	}

	@Test(expected = ServiceException.class)
	public void unPublishFail() {
		User user = getUser("publisher1");
		Optional<Publisher> p = publisherRepository.findByUser(user);
		journalService.unPublish(p.get(), 4L);
	}

	@Test(expected = ServiceException.class)
	public void unPublishFail2() {
		User user = getUser("publisher1");
		Optional<Publisher> p = publisherRepository.findByUser(user);
		journalService.unPublish(p.get(), 100L);
	}

	@Test
	public void unPublishSuccess() {
		User user = getUser("publisher2");
		Publisher publisher = publisherRepository.findByUser(user).get();

		Journal journal = new Journal();
		journal.setName(NEW_JOURNAL_NAME);
		journal.setUuid(SOME_EXTERNAL_ID);


		long categoryId =3L;
		Category category = categoryRepository.findOne(categoryId);

		try {
			journalService.publish(publisher, journal, category.getId());
		} catch (ServiceException e) {
			fail(e.getMessage());
		}

		List<Journal> journals;
		journals = journalService.publisherList(publisher);
		int sizeBefore = journals.size();
		journalService.unPublish(publisher, journals.get(0).getId());
		int sizeAfter = journalService.publisherList(publisher).size();
		Assert.assertEquals(sizeBefore, sizeAfter +1);

	}

	@Test
	public void findByNotified() {
		Assert.assertTrue(
			journalService.findNewJournals().stream().map(Journal::getName).collect(Collectors.toList())
					.containsAll(Arrays.asList("Test Journal", "Medicine"))
		);
	}

	@Test
	public void markJournalsAsNotified() {
		Assert.assertEquals(2,journalService.findNewJournals().size());
		Assert.assertEquals(2, journalService.markNewJournalsAsNotified());
		Assert.assertEquals(0,journalService.findNewJournals().size());
	}


	protected User getUser(String name) {
		Optional<User> user = userService.getUserByLoginName(name);
		if (!user.isPresent()) {
			fail("user1 doesn't exist");
		}
		return user.get();
	}

}
