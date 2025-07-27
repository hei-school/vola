package school.hei.vola.endpoint.event.consumer.model;

import school.hei.vola.PojaGenerated;
import school.hei.vola.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
