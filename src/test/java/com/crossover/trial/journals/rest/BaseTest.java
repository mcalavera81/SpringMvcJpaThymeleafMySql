package com.crossover.trial.journals.rest;

import com.crossover.trial.journals.model.User;
import com.crossover.trial.journals.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.Assert.fail;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
public class BaseTest {


    @Autowired
    protected UserService userService;


    protected User getUser(String name) {
        Optional<User> user = userService.getUserByLoginName(name);
        if (!user.isPresent()) {
            fail(String.format("user %s doesn't exist", name));
        }
        return user.get();
    }
}
