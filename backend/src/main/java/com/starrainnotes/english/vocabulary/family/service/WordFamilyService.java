package com.starrainnotes.english.vocabulary.family.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyMemberRequest;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyMemberView;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyRequest;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyView;
import com.starrainnotes.english.vocabulary.family.infrastructure.WordFamilyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class WordFamilyService {
    private final WordFamilyRepository repository;

    public WordFamilyService(WordFamilyRepository repository) {
        this.repository = repository;
    }

    public List<WordFamilyView> list(String query) {
        List<WordFamilyView> result = new ArrayList<>();
        for (Long id : repository.ids(query)) result.add(get(id));
        return result;
    }

    public WordFamilyView get(Long id) {
        WordFamilyView view = repository.get(id);
        if (view == null) throw missing();
        return view;
    }

    public WordFamilyView publicGet(String slug) {
        Long id = repository.idBySlug(slug);
        if (id == null) throw missing();
        return get(id);
    }

    @Transactional
    public WordFamilyView create(WordFamilyRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), repository::slugExists);
        List<Long> wordIds = validatedWordIds(request.wordIds());
        long id;
        try {
            id = repository.insert(request.headWord().trim(), slug, clean(request.description()));
        } catch (DuplicateKeyException ex) {
            throw conflict();
        }
        repository.replaceLinks(id, wordIds);
        return get(id);
    }

    @Transactional
    public WordFamilyView update(Long id, WordFamilyRequest request) {
        WordFamilyView current = get(id);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), current.slug());
        List<Long> wordIds = validatedWordIds(request.wordIds());
        try {
            repository.update(id, request.headWord().trim(), slug, clean(request.description()));
        } catch (DuplicateKeyException ex) {
            throw conflict();
        }
        repository.replaceLinks(id, wordIds);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        repository.delete(id);
    }

    @Transactional
    public WordFamilyMemberView addMember(Long familyId, WordFamilyMemberRequest request) {
        get(familyId);
        validate(request);
        int order = request.sortOrder() == null
                ? repository.nextMemberOrder(familyId) : request.sortOrder();
        Long id = repository.insertMember(familyId, request.spelling().trim(),
                clean(request.partOfSpeech()), clean(request.phoneticUs()),
                clean(request.translation()), clean(request.cefrLevel()),
                clean(request.exampleSentence()), clean(request.exampleTranslation()), order);
        return requireMember(id);
    }

    @Transactional
    public WordFamilyMemberView updateMember(Long familyId, Long memberId, WordFamilyMemberRequest request) {
        requireOwned(familyId, memberId);
        validate(request);
        repository.updateMember(memberId, request.spelling().trim(),
                clean(request.partOfSpeech()), clean(request.phoneticUs()),
                clean(request.translation()), clean(request.cefrLevel()),
                clean(request.exampleSentence()), clean(request.exampleTranslation()),
                request.sortOrder());
        return requireMember(memberId);
    }

    @Transactional
    public void deleteMember(Long familyId, Long memberId) {
        requireOwned(familyId, memberId);
        repository.deleteMember(memberId);
    }

    @Transactional
    public void move(Long familyId, Long memberId, int target) {
        requireOwned(familyId, memberId);
        List<Long> ids = repository.memberIds(familyId);
        ids.remove(memberId);
        ids.add(Math.min(Math.max(target, 0), ids.size()), memberId);
        for (int i = 0; i < ids.size(); i++) {
            repository.setMemberOrder(ids.get(i), (i + 1) * 10);
        }
    }

    private List<Long> validatedWordIds(List<Long> requested) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(requested == null ? List.of() : requested));
        for (Long id : ids) {
            if (!repository.wordExists(id)) invalid("Linked vocabulary word does not exist.");
        }
        return ids;
    }

    private void validate(WordFamilyMemberRequest request) {
        String cefr = clean(request.cefrLevel());
        if (cefr != null && !repository.cefrExists(cefr)) invalid("Invalid CEFR level.");
        if (request.sortOrder() != null && request.sortOrder() < 0) invalid("Invalid sort order.");
    }

    private void requireOwned(Long familyId, Long memberId) {
        if (!repository.ownsMember(familyId, memberId)) throw memberMissing();
    }

    private WordFamilyMemberView requireMember(Long id) {
        WordFamilyMemberView member = repository.member(id);
        if (member == null) throw memberMissing();
        return member;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_WORD_FAMILY_NOT_FOUND",
                "Word family not found", "The word family does not exist.");
    }

    private ApiException memberMissing() {
        return new ApiException(HttpStatus.NOT_FOUND, "VOCABULARY_FAMILY_MEMBER_NOT_FOUND",
                "Family member not found", "The word-family member does not exist.");
    }

    private ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "VOCABULARY_WORD_FAMILY_SLUG_CONFLICT",
                "Slug already exists", "Choose another family slug.");
    }

    private void invalid(String detail) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VOCABULARY_WORD_FAMILY_INVALID",
                "Invalid word family", detail);
    }
}
