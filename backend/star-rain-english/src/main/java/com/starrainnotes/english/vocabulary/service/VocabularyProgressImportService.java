/*
 * VocabularyProgressImportService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：VocabularyLocalProgressRequestDTO。
 * 阅读入口：importLocal（importLocal处理入口）。
 */

package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyLocalProgressRequestDTO;

/*
 * 游客本机进度导入。
 *
 * 未登录时学习进度只存在浏览器（IndexedDB），登录后由这个入口合并到账号；
 * 合并只增不减，不会用小号的本机进度覆盖账号上已有的更高进度。
 */
/**
 * 词汇进度Import业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface VocabularyProgressImportService {

    /**
     * importLocal处理入口。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyLocalProgressRequestDTO</code>
     */
    void importLocal(long accountId, VocabularyLocalProgressRequestDTO request);
}
