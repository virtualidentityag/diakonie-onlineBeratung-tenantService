TRUNCATE TABLE TENANT;
INSERT INTO TENANT (`id`, `name`, `subdomain`, `licensing_allowed_users`, `content_impressum`,`content_privacy`, `create_date`, `update_date`,`settings`, `is_video_call_allowed`, `show_asker_profile`)
                  VALUES (1, 'Happylife Gmbh', 'happylife', 5, '{"de" : "Impressum"}', '{"de" : "Privacy"}', '2021-12-28', '2021-12-29', null, true, true);

