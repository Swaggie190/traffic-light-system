-- Drop tables if they exist (for clean restart)
DROP TABLE IF EXISTS performance_metrics;
DROP TABLE IF EXISTS traffic_states;
DROP TABLE IF EXISTS simulation_configs;

-- Create simulation_configs table
CREATE TABLE simulation_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    scenario VARCHAR(50) NOT NULL,
    lambda_north DOUBLE NOT NULL,
    lambda_south DOUBLE NOT NULL,
    lambda_east DOUBLE NOT NULL,
    lambda_west DOUBLE NOT NULL,
    mu_north DOUBLE NOT NULL,
    mu_south DOUBLE NOT NULL,
    mu_east DOUBLE NOT NULL,
    mu_west DOUBLE NOT NULL,
    sigma_north DOUBLE NOT NULL,
    sigma_south DOUBLE NOT NULL,
    sigma_east DOUBLE NOT NULL,
    sigma_west DOUBLE NOT NULL,
    min_green_time INTEGER NOT NULL,
    max_green_time INTEGER NOT NULL,
    yellow_time INTEGER NOT NULL,
    red_clearance_time INTEGER NOT NULL,
    pedestrian_weight DOUBLE NOT NULL,
    switching_threshold DOUBLE NOT NULL,
    vehicle_performance_weight DOUBLE NOT NULL,
    pedestrian_performance_weight DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Create traffic_states table
CREATE TABLE traffic_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id VARCHAR(255) NOT NULL,
    time_step BIGINT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    vehicles_north INTEGER NOT NULL DEFAULT 0,
    vehicles_south INTEGER NOT NULL DEFAULT 0,
    vehicles_east INTEGER NOT NULL DEFAULT 0,
    vehicles_west INTEGER NOT NULL DEFAULT 0,
    pedestrians_north INTEGER NOT NULL DEFAULT 0,
    pedestrians_south INTEGER NOT NULL DEFAULT 0,
    pedestrians_east INTEGER NOT NULL DEFAULT 0,
    pedestrians_west INTEGER NOT NULL DEFAULT 0,
    current_phase VARCHAR(20) NOT NULL DEFAULT 'PHASE_1',
    current_green_time INTEGER NOT NULL DEFAULT 0,
    calculated_green_time INTEGER NOT NULL DEFAULT 30,
    phase1_density DOUBLE NOT NULL DEFAULT 0.0,
    phase2_density DOUBLE NOT NULL DEFAULT 0.0,
    FOREIGN KEY (simulation_id) REFERENCES simulation_configs(simulation_id)
);

-- Create performance_metrics table
CREATE TABLE performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id VARCHAR(255) NOT NULL,
    total_time_steps BIGINT NOT NULL,
    average_vehicle_waiting_time DOUBLE NOT NULL,
    average_pedestrian_waiting_time DOUBLE NOT NULL,
    combined_performance_index DOUBLE NOT NULL,
    total_vehicles_processed BIGINT NOT NULL,
    total_pedestrians_processed BIGINT NOT NULL,
    phase1_total_time BIGINT NOT NULL,
    phase2_total_time BIGINT NOT NULL,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (simulation_id) REFERENCES simulation_configs(simulation_id)
);

-- Create indexes for better performance
CREATE INDEX idx_simulation_configs_simulation_id ON simulation_configs(simulation_id);
CREATE INDEX idx_traffic_states_simulation_id ON traffic_states(simulation_id);
CREATE INDEX idx_traffic_states_time_step ON traffic_states(time_step);
CREATE INDEX idx_performance_metrics_simulation_id ON performance_metrics(simulation_id);