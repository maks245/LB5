DELETE FROM user_roles;
-- Потім видаляємо дані з основних таблиць
DELETE FROM users;
DELETE FROM roles;

-- Скидання послідовностей (ID) для H2
-- Примітка: Імена послідовностей в H2 зазвичай мають вигляд TABLE_NAME_SEQ
-- Якщо ID ваших таблиць генеруються через @GeneratedValue(strategy = GenerationType.IDENTITY),
-- то Hibernate автоматично створює послідовності.
-- У H2 вони часто називаються HIBERNATE_SEQUENCE (для всіх таблиць, якщо немає @SequenceGenerator)
-- або TABLE_NAME_SEQ (якщо @GeneratedValue(strategy = GenerationType.SEQUENCE) з певним іменем)
-- АБО просто автоматично скидаються при TRUNCATE або якщо IDENTITY column використовує GENERATED ALWAYS AS IDENTITY.

-- Найпростіший спосіб для H2 з @GeneratedValue(strategy = GenerationType.IDENTITY)
-- Це не завжди потрібно, бо H2 in-memory база даних скидається з кожним запуском.
-- Але якщо потрібно скинути лічильник після DELETE, то так:
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
ALTER TABLE roles ALTER COLUMN id RESTART WITH 1;