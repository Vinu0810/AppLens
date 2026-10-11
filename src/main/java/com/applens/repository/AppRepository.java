package com.applens.repository;

import com.applens.entity.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppRepository extends JpaRepository<App, Long> {

    Optional<App> findByAppId(String appId);
    Page<App> findByCategory(String category, Pageable pageable);
    Page<App> findByStore(String store, Pageable pageable);
    Page<App> findByAppType(String appType, Pageable pageable);
    Page<App> findByCategoryAndStore(String category, String store, Pageable pageable);
    Page<App> findByCategoryAndStoreAndAppType(String category, String store, String appType, Pageable pageable);

    @Query("SELECT AVG(a.rating) FROM App a WHERE a.rating IS NOT NULL")
    Double findAverageRating();

    @Query("SELECT a.store, COUNT(a) FROM App a GROUP BY a.store")
    List<Object[]> countByStore();

    @Query("SELECT a.appType, COUNT(a) FROM App a GROUP BY a.appType")
    List<Object[]> countByAppType();

    @Query("SELECT COUNT(DISTINCT a.category) FROM App a WHERE a.category IS NOT NULL")
    Long countDistinctCategories();

    @Query("SELECT a.category, AVG(a.rating), COUNT(a) FROM App a WHERE a.category IS NOT NULL AND a.rating IS NOT NULL GROUP BY a.category ORDER BY AVG(a.rating) DESC")
    List<Object[]> findAvgRatingByCategory();

    @Query("SELECT a.appType, AVG(a.rating), COUNT(a) FROM App a WHERE a.appType IS NOT NULL AND a.rating IS NOT NULL GROUP BY a.appType")
    List<Object[]> findAvgRatingByAppType();

    @Query("SELECT a.category, SUM(a.installCount), COUNT(a) FROM App a WHERE a.category IS NOT NULL AND a.installCount IS NOT NULL GROUP BY a.category ORDER BY SUM(a.installCount) DESC")
    List<Object[]> findTopCategoriesByInstalls();

    @Query("SELECT a.contentRating, COUNT(a), AVG(a.rating) FROM App a WHERE a.contentRating IS NOT NULL GROUP BY a.contentRating ORDER BY COUNT(a) DESC")
    List<Object[]> findContentRatingStats();

    @Query("SELECT a.store, AVG(a.rating), COUNT(a) FROM App a WHERE a.rating IS NOT NULL GROUP BY a.store")
    List<Object[]> findAvgRatingByStore();

    @Query("SELECT a.rating FROM App a WHERE a.appType = :appType AND a.rating IS NOT NULL")
    List<BigDecimal> findRatingsByAppType(@Param("appType") String appType);

    @Query("SELECT a.rating FROM App a WHERE a.store = :store AND a.rating IS NOT NULL")
    List<BigDecimal> findRatingsByStore(@Param("store") String store);

    @Query("SELECT a.category, AVG(a.rating), COUNT(a), SUM(a.reviewCount), AVG(a.price) FROM App a WHERE a.category IS NOT NULL GROUP BY a.category ORDER BY COUNT(a) DESC")
    List<Object[]> findCategoryBreakdown();

    @Query("SELECT a FROM App a WHERE LOWER(a.name) LIKE LOWER(CONCAT(\'%\', :search, \'%\')) OR LOWER(a.category) LIKE LOWER(CONCAT(\'%\', :search, \'%\'))")
    Page<App> searchApps(@Param("search") String search, Pageable pageable);
}