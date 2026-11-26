package io.github.yilers.web.field;

import io.github.yilers.api.field.*;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

/** Jackson 3字段权限扩展；仅改变写出的JSON，不清空或覆盖VO原值。 */
@Configuration(proxyBeanMethods = false)
public class FieldPermissionJacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer fieldPermissionCustomizer(FieldPermissionService permissions) {
        return builder -> {
            var module = new SimpleModule("field-permission");
            module.setSerializerModifier(new ValueSerializerModifier() {
                @Override
                public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                        BeanDescription.Supplier bean, List<BeanPropertyWriter> properties) {
                    return properties.stream().map(property -> {
                        var annotation = property.getAnnotation(FieldPermission.class);
                        return annotation == null ? property : new ProtectedWriter(property, annotation, permissions);
                    }).toList();
                }
            });
            builder.addModule(module);
        };
    }

    private static class ProtectedWriter extends BeanPropertyWriter {
        private final FieldPermission annotation;
        private final String key;
        private final FieldPermissionService permissions;

        ProtectedWriter(BeanPropertyWriter original, FieldPermission annotation, FieldPermissionService permissions) {
            super(original);
            this.annotation = annotation;
            this.key = FieldDefinition.from(annotation).key();
            this.permissions = permissions;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
            var access = permissions.access(key);
            if (access.viewMode() == FieldViewMode.FULL) {
                super.serializeAsProperty(bean, generator, context);
            } else if (access.viewMode() == FieldViewMode.MASKED) {
                generator.writeName(getName());
                String value = annotation.mask().mask(get(bean));
                if (value == null) {
                    generator.writeNull();
                } else {
                    generator.writeString(value);
                }
            }
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
            var access = permissions.access(key);
            if (access.viewMode() == FieldViewMode.FULL) {
                super.serializeAsElement(bean, generator, context);
            } else if (access.viewMode() == FieldViewMode.MASKED) {
                String value = annotation.mask().mask(get(bean));
                if (value == null) {
                    generator.writeNull();
                } else {
                    generator.writeString(value);
                }
            } else {
                generator.writeNull();
            }
        }
    }
}
