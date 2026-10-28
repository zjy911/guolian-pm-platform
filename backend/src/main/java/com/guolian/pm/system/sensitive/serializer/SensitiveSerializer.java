package com.guolian.pm.system.sensitive.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.guolian.pm.common.util.SpringUtil;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import com.guolian.pm.system.sensitive.service.SensitivePolicyService;

import java.io.IOException;

/**
 * 脱敏序列化器：按当前角色档位与字段注册算法输出。
 * HIDDEN 时字段直接从 JSON 中移除（不用空串或 null 占位）。
 *
 * <p>注意：{@code @Sensitive} 不只标在 String 字段上（金额、成本、利润率是 BigDecimal），
 * 因此按 Object 处理——只有文本字段参与字符级脱敏，其余类型原样输出，
 * 避免把数值强转成 String（曾导致 BigDecimal cannot be cast to String 的 500）。</p>
 */
public class SensitiveSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    private final String group;
    private final String fieldName;

    public SensitiveSerializer() {
        this(null, null);
    }

    public SensitiveSerializer(String group, String fieldName) {
        this.group = group;
        this.fieldName = fieldName;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        SensitivePolicyService policy = SpringUtil.getBean(SensitivePolicyService.class);
        // HIDDEN 档位：整个字段不输出
        if (policy != null && group != null && policy.isHidden(group)) {
            gen.writeOmittedField(fieldName);
            return;
        }
        // 非文本字段（金额、比率、日期等）不做字符级脱敏，按原类型输出
        if (!(value instanceof CharSequence)) {
            serializers.defaultSerializeValue(value, gen);
            return;
        }
        String text = value.toString();
        if (policy == null) {
            gen.writeString("***");
            return;
        }
        gen.writeString(policy.mask(group, fieldName, text));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        Sensitive annotation = property == null ? null : property.getAnnotation(Sensitive.class);
        if (annotation == null && property != null) {
            annotation = property.getContextAnnotation(Sensitive.class);
        }
        if (annotation == null) {
            return prov.findValueSerializer(String.class);
        }
        return new SensitiveSerializer(annotation.group(), property.getName());
    }
}
