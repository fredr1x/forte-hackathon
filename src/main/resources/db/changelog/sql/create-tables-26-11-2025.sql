CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    telegram_id BIGINT UNIQUE,
    role VARCHAR(50),
    email VARCHAR(255),
    jira_username VARCHAR(255),
    jira_api_token VARCHAR(255),
    created_at TIMESTAMP,
    last_login TIMESTAMP,
    team_id BIGINT
);

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    pm_id BIGINT,
    jira_project_key VARCHAR(255),
    jira_url VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE meetings (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT,
    file_name VARCHAR(255),
    file_url VARCHAR(512),
    transcription TEXT,
    summary TEXT,
    uploaded_at TIMESTAMP,
    processed_at TIMESTAMP,
    processing_status VARCHAR(50)
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    jira_key VARCHAR(255) NOT NULL UNIQUE,
    summary VARCHAR(255) NOT NULL,
    description TEXT,
    assignee_id BIGINT,
    team_id BIGINT,
    status VARCHAR(50),
    priority VARCHAR(50),
    deadline TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    jira_url VARCHAR(255),
    meeting_id BIGINT
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_team_id FOREIGN KEY (team_id)
        REFERENCES teams(id);

ALTER TABLE teams
    ADD CONSTRAINT fk_teams_pm_id FOREIGN KEY (pm_id)
        REFERENCES users(id);

ALTER TABLE meetings
    ADD CONSTRAINT fk_meetings_team_id FOREIGN KEY (team_id)
        REFERENCES teams(id);

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee_id FOREIGN KEY (assignee_id)
        REFERENCES users(id);

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_team_id FOREIGN KEY (team_id)
        REFERENCES teams(id);

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_meeting_id FOREIGN KEY (meeting_id)
        REFERENCES meetings(id);

CREATE INDEX idx_users_team_id ON users(team_id);
CREATE INDEX idx_tasks_team_id ON tasks(team_id);
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_tasks_meeting_id ON tasks(meeting_id);
CREATE INDEX idx_meetings_team_id ON meetings(team_id);
