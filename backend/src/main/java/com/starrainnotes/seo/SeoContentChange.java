package com.starrainnotes.seo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SeoContentChange {
    String kind();
    String pathPrefix();
    /** Zero-based position of the content id in the annotated method. */
    int idParameter() default 0;
}
