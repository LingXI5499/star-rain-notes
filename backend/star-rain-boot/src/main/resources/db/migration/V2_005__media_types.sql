-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-media 媒体类型扩展
--
-- 背景：V2_004 只登记了 IMAGE/DOCUMENT/AUDIO/OTHER 四类。
-- 本轮补齐用户明确要求的媒体能力：视频与压缩包，
-- 并把 file_extension 的语义说明与大小口径写进列注释。
--
-- 说明：
-- 1. media_type 是 VARCHAR(30)，新增枚举值不需要改列类型，本迁移只更新注释；
-- 2. 不建立任何物理外键，与 V2_004 保持一致；
-- 3. 本迁移不改动已有数据，可安全重复执行（幂等）。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 媒体大类的取值口径
--    IMAGE    图片：png/jpg/jpeg/webp/gif/bmp/tiff/avif
--    DOCUMENT 文档：pdf/md/markdown/txt/doc/docx/xls/xlsx/ppt/pptx
--    AUDIO    音频：mp3/m4a/aac/ogg/oga/wav/flac
--    VIDEO    视频：mp4/m4v/mov/webm/mkv/avi/ogv
--    ARCHIVE  压缩包：zip/rar/7z/tar/gz/tgz
--    OTHER    预留，当前不开放上传
-- ---------------------------------------------------------------------
ALTER TABLE sr_media_asset
    MODIFY COLUMN media_type VARCHAR(30) NOT NULL
    COMMENT 'IMAGE/DOCUMENT/AUDIO/VIDEO/ARCHIVE/OTHER';

ALTER TABLE sr_media_asset
    MODIFY COLUMN file_extension VARCHAR(20) NULL
    COMMENT '规范化扩展名，不含点；与 media_type 一起决定前端的分类展示方式';

ALTER TABLE sr_media_asset
    MODIFY COLUMN size_bytes BIGINT NOT NULL
    COMMENT '文件大小，字节；各媒体大类的上限由应用配置决定，不写死在数据库';

-- ---------------------------------------------------------------------
-- 2. 视频与压缩包属于大文件，按 media_type 的既有索引查询即可，
--    这里只补充一条说明，避免后续误加冗余索引。
--    既有索引：idx_sr_media_asset_type_status (media_type, status)
-- ---------------------------------------------------------------------
