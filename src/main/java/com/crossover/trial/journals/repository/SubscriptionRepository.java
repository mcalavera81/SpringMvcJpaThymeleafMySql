package com.crossover.trial.journals.repository;

import com.crossover.trial.journals.model.Journal;
import com.crossover.trial.journals.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by mcalavera81 on 09/04/2017.
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByCategoryId(Long categoryId);
}
