-- First admin user for Rosswood Management System
-- Username: admin
-- Password: Rosswood@2024
-- Generated with BcryptUtil.bcryptHash("Rosswood@2024") cost=10
INSERT INTO app_user (id, username, email, password, firstname, lastname, isadmin, status)
VALUES (
           1,
           'admin',
           'admin@rosswood.com',
           '$2a$10$OjvRwGdYJ3lpzQvqkuqP9O.rimi4sKSK1L64qi9mmxrEC116K5Bo.',
           'System',
           'Admin',
           true,
           'REGISTERED'
       );

-- Give the existing admin user all roles
UPDATE app_user SET roles = 'sales,production,inventory' WHERE isadmin = true;