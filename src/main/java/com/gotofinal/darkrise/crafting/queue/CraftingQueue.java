package com.gotofinal.darkrise.crafting.queue;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.UUID;

public class CraftingQueue {

    @Getter
    @Setter
    private UUID owner;

    @Getter
    private LinkedList<QueueItem> queue = new LinkedList<>();

    public CraftingQueue(Player owner) {
        setOwner(owner.getUniqueId());
    }
}
