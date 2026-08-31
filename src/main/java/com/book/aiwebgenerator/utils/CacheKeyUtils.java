package com.book.aiwebgenerator.utils;


import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class CacheKeyUtils {

    /**
     * Generate a cache key based on an object (JSON + MD5)
     *
     * @param obj The object to generate the key for
     * @return The MD5 hashed cache key
     */
    public static String generateKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // Convert to JSON first, then apply MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}