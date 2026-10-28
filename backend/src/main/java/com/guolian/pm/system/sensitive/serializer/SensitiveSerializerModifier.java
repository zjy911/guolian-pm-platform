package com.guolian.pm.system.sensitive.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.guolian.pm.common.util.SpringUtil;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import com.guolian.pm.system.sensitive.service.SensitivePolicyService;

import java.util.ArrayList;
import java.util.List;

/**
 * 让 @Sensitive 字段在 HIDDEN 档位下从 JSON 中彻底消失（不用空串或 null 占位）。
 * 通过替换 BeanPropertyWriter 实现，避免把策略固化进序列化器缓存。
 */
public class SensitiveSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> result = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            Sensitive sensitive = writer.getAnnotation(Sensitive.class);
            if (sensitive == null) {
                result.add(writer);
            } else {
                result.add(new HiddenAwareWriter(writer, sensitive.group()));
            }
        }
        return result;
    }

    private static final class HiddenAwareWriter extends BeanPropertyWriter {

        private final String group;

        private HiddenAwareWriter(BeanPropertyWriter base, String group) {
            super(base);
            this.group = group;
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            SensitivePolicyService policy = SpringUtil.getBean(SensitivePolicyService.class);
            if (policy != null && policy.isHidden(group)) {
                return;
            }
            super.serializeAsField(bean, gen, prov);
        }
    }
}
