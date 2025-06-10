package com.traffic.repository;

import com.traffic.model.PerformanceMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceMetricsRepository extends JpaRepository<PerformanceMetrics, Long> {
    
    Optional<PerformanceMetrics> findBySimulationId(String simulationId);
    
    List<PerformanceMetrics> findAllByOrderByCalculatedAtDesc();
    
    void deleteBySimulationId(String simulationId);
    
    @Query("SELECT pm FROM PerformanceMetrics pm ORDER BY pm.combinedPerformanceIndex ASC LIMIT :limit")
    List<PerformanceMetrics> findTopPerformingSimulations(@Param("limit") int limit);
}