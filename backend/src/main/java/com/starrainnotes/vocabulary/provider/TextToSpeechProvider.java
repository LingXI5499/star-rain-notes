package com.starrainnotes.vocabulary.provider;

import java.util.Optional;

/** Replaceable text-to-speech source. The default application uses browser speech synthesis. */
public interface TextToSpeechProvider {
    Optional<byte[]> synthesize(String text, String voice);
}
