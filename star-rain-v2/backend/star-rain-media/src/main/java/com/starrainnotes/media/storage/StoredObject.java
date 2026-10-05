package com.starrainnotes.media.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 存储结果 —— 存储抽象的出参，与 StorageWriteCommand 同属 MediaStorage 的契约。
 *
 * storageKey 是相对 Key（形如 2026/10/<uuid>.png），禁止是服务器绝对路径。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredObject {

    private String storageProvider;
    private String storageKey;
    private long sizeBytes;
}
