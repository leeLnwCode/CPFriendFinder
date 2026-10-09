-- Reference interest catalogue only; no test accounts, conversations or personal data.
INSERT INTO interests(id,name,is_active,created_at) VALUES ('2b7e336c-46a0-5c72-8e7b-e18c28128398','Gaming',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('f678655a-b99a-55e7-8659-bed8bc59b9ad','Music',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('6cc9f818-5ded-5fcd-b26a-46a9f3b12672','Coding',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('f3e10744-9c81-589b-9b95-5faac04c3009','Sports',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('fa25c18f-e005-5021-a604-a32e9555176b','Movies',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('3b45dc75-35b2-53d9-bd94-789be0721125','Reading',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('4fd015dc-52c7-56ad-864a-ac27e3b1d2de','Travel',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
INSERT INTO interests(id,name,is_active,created_at) VALUES ('ab3a0045-0ac5-5844-9ed2-548c5436d5cf','Art',true,CURRENT_TIMESTAMP) ON CONFLICT(name) DO NOTHING;
