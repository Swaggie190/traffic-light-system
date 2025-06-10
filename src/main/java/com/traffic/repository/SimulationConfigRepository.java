package com.traffic.repository;

import com.traffic.model.SimulationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationConfigRepository extends JpaRepository<SimulationConfig, Long> {
    
    Optional<SimulationConfig> findBySimulationId(String simulationId);
    
    List<SimulationConfig> findByIsActiveTrue();
    
    Optional<SimulationConfig> findByName(String name);
    
    @Query("SELECT sc FROM SimulationConfig sc WHERE sc.isActive = true ORDER BY sc.createdAt DESC LIMIT 1")
    Optional<SimulationConfig> findLatestActive();
    
    void deleteBySimulationId(String simulationId);
}