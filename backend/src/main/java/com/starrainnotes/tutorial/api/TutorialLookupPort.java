package com.starrainnotes.tutorial.api;

import java.util.Collection;
import java.util.List;

public interface TutorialLookupPort {
    record Ref(long id, String title, String slug, String publishStatus) {
    }

    boolean containsAll(Collection<Long> ids);

    List<Ref> findAll(Collection<Long> ids);
}
