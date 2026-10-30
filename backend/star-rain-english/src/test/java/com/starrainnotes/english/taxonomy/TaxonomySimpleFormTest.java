package com.starrainnotes.english.taxonomy;

import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.mapper.TaxonomyMapper;
import com.starrainnotes.english.taxonomy.service.impl.TaxonomyServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaxonomySimpleFormTest {
    private final TaxonomyMapper mapper = mock(TaxonomyMapper.class);
    private final TaxonomyServiceImpl service = new TaxonomyServiceImpl(mapper);
    private Node node(long id, String code) {
        Node n = new Node(); n.setId(id); n.setCode(code); n.setDimension("VOCABULARY"); n.setLibraryModule("vocabulary");
        n.setName("生活"); n.setNameEn("Life"); n.setSlug("life"); n.setEnabled(true); return n;
    }
    @Test void generatesStableMetadataForNewTopicUsingOnlyNamesAndParent() {
        Node parent = node(1L, "T01"), used = node(2L, "T01-01"); used.setParentId(1L);
        when(mapper.byId(1L)).thenReturn(parent); when(mapper.list(true)).thenReturn(List.of(parent, used));
        Node request = new Node(); request.setDimension("VOCABULARY"); request.setParentId(1L); request.setName("健康"); request.setNameEn("Health");
        doAnswer(call -> { ((Node) call.getArgument(0)).setId(3L); return 1; }).when(mapper).insert(any());
        service.create(request);
        assertThat(request.getCode()).isEqualTo("T01-02"); assertThat(request.getSlug()).startsWith("term-");
        verify(mapper).lockCatalog(); verify(mapper).insert(request);
    }
    @Test void aNameEditPreservesExistingLinksCodeParentAndOrder() {
        Node old = node(2L, "T01-01"); old.setParentId(1L); old.setSortOrder(20);
        when(mapper.byId(2L)).thenReturn(old); when(mapper.byId(1L)).thenReturn(node(1L, "T01"));
        Node request = new Node(); request.setName("新名称"); request.setNameEn("New name");
        service.update(2L, request);
        assertThat(request.getCode()).isEqualTo("T01-01"); assertThat(request.getSlug()).isEqualTo("life");
        assertThat(request.getParentId()).isEqualTo(1L); assertThat(request.getSortOrder()).isEqualTo(20);
    }
}
