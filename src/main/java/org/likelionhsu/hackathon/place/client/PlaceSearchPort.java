package org.likelionhsu.hackathon.place.client;

import java.util.List;

public interface PlaceSearchPort {

    List<ExternalPlace> search(PlaceSearchCommand command);
}
