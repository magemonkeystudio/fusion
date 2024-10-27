package studio.magemonkey.fusion.api.events.services;

import lombok.Getter;

@Getter
public class EventServices {

    private final ProfessionService professionService;
    private final QueueService queueService;

    public EventServices() {
        professionService = new ProfessionService();
        queueService = new QueueService();
    }
}
