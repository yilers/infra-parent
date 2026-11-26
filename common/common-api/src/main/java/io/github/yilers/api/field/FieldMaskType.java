package io.github.yilers.api.field;

/** 可复用的字段脱敏方式；不适用时保守返回星号，不返回原值。 */
public enum FieldMaskType {
    NONE, PHONE, EMAIL;

    public String mask(Object value) {
        if (value == null) return null;
        String text = value.toString();
        if (this == PHONE && text.length() >= 7) {
            return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
        }
        if (this == EMAIL && text.indexOf('@') > 0) {
            return text.substring(0, 1) + "***" + text.substring(text.indexOf('@'));
        }
        return "***";
    }
}
