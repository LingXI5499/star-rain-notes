package com.starrainnotes.seo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Bounded, single-flight cache. Invalidated in-flight loads must reload before returning. */
final class BoundedSeoCache<V> {
    private record Entry<V>(V value, long expiresAt) {}
    private final Map<String, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private final Object[] flights = new Object[32];
    private final int capacity;
    private final long ttl;
    private final LongSupplier clock;
    private long generation;

    BoundedSeoCache(int capacity, long ttl, LongSupplier clock) {
        this.capacity = capacity;
        this.ttl = ttl;
        this.clock = clock;
        for (int i = 0; i < flights.length; i++) flights[i] = new Object();
    }

    V get(String key, Supplier<V> loader) {
        synchronized (flights[Math.floorMod(key.hashCode(), flights.length)]) {
            while (true) {
                long started;
                synchronized (this) {
                    long now = clock.getAsLong();
                    entries.values().removeIf(entry -> entry.expiresAt() <= now);
                    Entry<V> entry = entries.get(key);
                    if (entry != null) return entry.value();
                    started = generation;
                }
                V value = loader.get();
                synchronized (this) {
                    if (started != generation) continue;
                    // Missing content is deliberately not retained.
                    if (value != null) {
                        entries.put(key, new Entry<>(value, clock.getAsLong() + ttl));
                        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
                    }
                    return value;
                }
            }
        }
    }

    synchronized void clear() { generation++; entries.clear(); }
}
