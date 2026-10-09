package com.starrainnotes.english.knowledge.utils;
import com.starrainnotes.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;
public final class EnglishBodyValidator {
    private static final Pattern TEXT=Pattern.compile("[\\p{L}\\p{N}]");
    private EnglishBodyValidator() { }
    public static boolean hasMeaningfulText(String markdown) {
        if(markdown==null) return false;
        String text=markdown.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>","")
            .replaceAll("(?s)<[^>]*>","").replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)","")
            .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)","$1")
            .replaceAll("(?m)^\\s*\\[[^]]+]:.*$","").replaceAll("&(?:nbsp|#160|#x[aA]0);", " ");
        return TEXT.matcher(text).find();
    }
    public static void requireBody(String body) { if(!hasMeaningfulText(body)) throw new ApiException("ENGLISH_BODY_REQUIRED","完成或发布前请填写英文正文",400); }
    public static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((text==null?"":text).getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable",e); }
    }
    public static String title(String text) { return text==null || text.isBlank()?"Untitled Article":text.trim(); }
    public static String blank(String text) { return text==null?"":text; }
    public static void length(String value,int maximum) { if(value!=null && value.length()>maximum) throw new ApiException("ENGLISH_DOCUMENT_INVALID","内容超过允许长度",400); }
}
