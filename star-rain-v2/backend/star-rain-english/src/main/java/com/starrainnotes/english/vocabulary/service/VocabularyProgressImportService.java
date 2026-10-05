package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyLocalProgressRequest;

/*
 * 游客本机进度导入。
 *
 * 未登录时学习进度只存在浏览器（IndexedDB），登录后由这个入口合并到账号；
 * 合并只增不减，不会用小号的本机进度覆盖账号上已有的更高进度。
 */
public interface VocabularyProgressImportService {

    void importLocal(long accountId, VocabularyLocalProgressRequest request);
}
