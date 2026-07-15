package com.mdt.collab.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 标注序列化器（architecture.md §3：标注对象序列化为 Protobuf/JSON 后支持回放与持久化）。
 * 先用 JSON 落地；生产可平滑切 Protobuf（同一 AnnotationOp 结构）。
 */
public class AnnotationSerializer {

    private static final ObjectMapper OM = new ObjectMapper();

    /** 标注操作（白板一笔 / 一个图形 / 一段文字） */
    public static class AnnotationOp {
        public String id;               // 元素 ID（前端生成，UUID）
        public String type;            // pen | rect | arrow | text
        public List<List<Double>> points; // 归一化坐标 [[x,y],...]
        public String color;           // #RRGGBB
        public String author;          // 作者用户标识（脱敏后仅用户名）
        public long t;                 // 时间戳（ms）
        public int version;            // 乐观锁版本号（服务端回填）

        public AnnotationOp() {}
        public AnnotationOp(String id, String type, List<List<Double>> points, String color, String author, long t, int version) {
            this.id = id; this.type = type; this.points = points; this.color = color;
            this.author = author; this.t = t; this.version = version;
        }
    }

    public static String serialize(AnnotationOp op) {
        try { return OM.writeValueAsString(op); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("标注序列化失败", e); }
    }

    public static AnnotationOp deserialize(String json) {
        try { return OM.readValue(json, AnnotationOp.class); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("标注反序列化失败", e); }
    }
}
