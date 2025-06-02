
INSERT INTO users (username, email, password) VALUES ('testuser', 'test@example.com', '$2a$10$l2N7vIk2X191mQuIDzAP0eGeAgaTX8znBtymC14uAvHeNe5oq/MKK');


INSERT INTO user_roles (user_id, role_id)
VALUES (SCOPE_IDENTITY(), 1);