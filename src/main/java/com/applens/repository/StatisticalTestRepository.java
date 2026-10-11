package com.applens.repository;

import com.applens.entity.StatisticalTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatisticalTestRepository extends JpaRepository<StatisticalTest, Long> {
    
    List<StatisticalTest> findAllByOrderByComputedAtDesc();

    void deleteByTestName(String testName);
}
