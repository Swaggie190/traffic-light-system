package com.traffic.repository;

import com.traffic.model.TrafficState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrafficStateRepository extends JpaRepository<TrafficState, Long> {
    
    List<TrafficState> findBySimulationIdOrderByTimeStepAsc(String simulationId);
    
    Optional<TrafficState> findBySimulationIdAndTimeStep(String simulationId, Long timeStep);
    
    @Query("SELECT ts FROM TrafficState ts WHERE ts.simulationId = :simulationId ORDER BY ts.timeStep DESC LIMIT 1")
    Optional<TrafficState> findLatestBySimulationId(@Param("simulationId") String simulationId);
    
    @Query("SELECT ts FROM TrafficState ts WHERE ts.simulationId = :simulationId AND ts.timeStep >= :fromStep AND ts.timeStep <= :toStep ORDER BY ts.timeStep ASC")
    List<TrafficState> findBySimulationIdAndTimeStepRange(
        @Param("simulationId") String simulationId,
        @Param("fromStep") Long fromStep,
        @Param("toStep") Long toStep
    );
    
    void deleteBySimulationId(String simulationId);
    
    @Query("SELECT COUNT(ts) FROM TrafficState ts WHERE ts.simulationId = :simulationId")
    Long countBySimulationId(@Param("simulationId") String simulationId);
}



