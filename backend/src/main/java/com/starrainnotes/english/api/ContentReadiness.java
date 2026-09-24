package com.starrainnotes.english.api;

import com.starrainnotes.english.shared.content.ContentDescriptor;
import java.util.List;

/** Public availability of an English content item and its required parent. */
public record ContentReadiness(ContentDescriptor content, boolean ready, List<String> issues) { }
