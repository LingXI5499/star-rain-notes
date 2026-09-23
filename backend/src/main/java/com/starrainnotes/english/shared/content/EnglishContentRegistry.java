package com.starrainnotes.english.shared.content;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class EnglishContentRegistry {
    private final Map<EnglishContentType,EnglishContentDescriptorProvider> providers;

    public EnglishContentRegistry(List<EnglishContentDescriptorProvider> providers) {
        Map<EnglishContentType,EnglishContentDescriptorProvider> selected=new EnumMap<>(EnglishContentType.class);
        for(EnglishContentDescriptorProvider provider:providers) {
            if(selected.putIfAbsent(provider.type(),provider)!=null)
                throw new IllegalStateException("Duplicate English content provider: "+provider.type());
        }
        this.providers=Map.copyOf(selected);
    }

    public ContentDescriptor require(EnglishContentType type,long id) {
        EnglishContentDescriptorProvider provider=providers.get(type);
        if(provider==null) throw new IllegalArgumentException("Unsupported English content type: "+type);
        return provider.require(id);
    }

    public ContentDescriptor requirePublished(EnglishContentType type,long id) {
        ContentDescriptor content=require(type,id);
        if(!content.published()) throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_CONTENT_NOT_PUBLISHED",
                "Content unavailable","The selected English content is not published.");
        return content;
    }

    public ContentCatalogSlice catalog(EnglishContentType type, ContentCatalogFilter filter, int limit) {
        EnglishContentDescriptorProvider provider = providers.get(type);
        if (provider == null) throw new IllegalArgumentException("Unsupported English content type: " + type);
        return provider.catalog(filter, limit);
    }

    public long publishedCount(EnglishContentType type) {
        EnglishContentDescriptorProvider provider = providers.get(type);
        if (provider == null) throw new IllegalArgumentException("Unsupported English content type: " + type);
        return provider.publishedCount();
    }
}
