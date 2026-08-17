CREATE INDEX idx_style_plans_user_created_id
    ON style_plans (user_id, created_at DESC, id DESC);

CREATE INDEX idx_recommendations_user_generated_id
    ON recommendations (user_id, generated_at DESC, id DESC);
