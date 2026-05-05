package com.rosswood.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "app_user")
public class User extends PanacheEntity implements RenardeUserWithPassword {

    @Column(unique = true)
    public String userName;
    public String password;

    @Column(name = "roles")
    public String roles;

    @Override
    public boolean registered(){
        return true;
    }

    @Override
    public Set<String> roles() {
        if (roles == null || roles.isBlank()) return Collections.emptySet();
        Set<String> set = new HashSet<>();
        for (String r : roles.split(",")) {
            String trimmed = r.trim();
            if (!trimmed.isEmpty()) set.add(trimmed);
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public String userId() {
        return userName;
    }

    @Override
    public String password() {
        return password;
    }

    //
    // Helpers

    public static User findByUserName(String username) {
        return find("LOWER(userName) = ?1", username.toLowerCase()).firstResult();
    }
}
