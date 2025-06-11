-- Sample simulation configuration for testing
INSERT INTO simulation_configs (
    simulation_id, name, scenario,
    lambda_north, lambda_south, lambda_east, lambda_west,
    mu_north, mu_south, mu_east, mu_west,
    sigma_north, sigma_south, sigma_east, sigma_west,
    min_green_time, max_green_time, yellow_time, red_clearance_time,
    pedestrian_weight, switching_threshold,
    vehicle_performance_weight, pedestrian_performance_weight,
    created_at, is_active
) VALUES (
    'demo-sim-001', 'Demo Balanced Traffic', 'BALANCED',
    2.0, 2.0, 2.0, 2.0,
    1.0, 1.0, 1.0, 1.0,
    3.0, 3.0, 3.0, 3.0,
    15, 60, 3, 2,
    0.3, 2.0,
    0.7, 0.3,
    CURRENT_TIMESTAMP, true
);

-- Initial traffic state for demo simulation
INSERT INTO traffic_states (
    simulation_id, time_step, timestamp,
    vehicles_north, vehicles_south, vehicles_east, vehicles_west,
    pedestrians_north, pedestrians_south, pedestrians_east, pedestrians_west,
    current_phase, current_green_time, calculated_green_time,
    phase1_density, phase2_density
) VALUES (
    'demo-sim-001', 0, CURRENT_TIMESTAMP,
    5, 7, 3, 4,
    2, 3, 1, 2,
    'PHASE_1', 0, 30,
    0.4, 0.25
);