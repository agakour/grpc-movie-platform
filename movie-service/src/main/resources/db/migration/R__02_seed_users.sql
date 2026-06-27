INSERT INTO app_user (username, password_hash, role) VALUES
    ('admin', '$2a$10$bFlu6je4tvalvuQ1QCx/TO5uBIcMTtuM1FvznsW3AMv3fpQYmrz02', 'ADMIN'),
    ('user',  '$2a$10$vhNQ8z4De5/Grc0oft7BYu2MLVy94769t8CKFyVfERSQZBP4NxFNS', 'USER')
ON CONFLICT (username) DO NOTHING;
