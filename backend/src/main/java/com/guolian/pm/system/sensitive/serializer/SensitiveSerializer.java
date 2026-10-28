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
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

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
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        SensitivePolicyService policy = SpringUtil.getBean(SensitivePolicyService.class);
        if (policy == null) {
            gen.writeString("***");
            return;
        }
        if (policy.isHidden(group)) {
            gen.writeOmittedField(fieldName);
            return;
        }
        gen.writeString(policy.mask(group, fieldName, value));
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
