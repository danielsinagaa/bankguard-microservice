UPDATE users
SET password_hash = '$2a$10$C0yBBbs26WtQZvXhLAqvtOYL39PswkPjbgvorq8iIJn99Zr/tMbzu'
WHERE username = 'admin';

UPDATE users
SET password_hash = '$2a$10$ngT4yEjyaRREk3Atgfok7uMQCrSLg760lSXFsGOoK/U18Vl6.NTci'
WHERE username = 'backoffice';

UPDATE users
SET password_hash = '$2a$10$RGkUKRVlldzieo.UOq9og.Sf7Wbt1TCZXzPIijCzG20.4GIxY4yOe'
WHERE username = 'analyst';
