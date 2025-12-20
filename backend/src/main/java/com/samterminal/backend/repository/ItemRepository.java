package com.samterminal.backend.repository;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByUser(AppUser user);
}
