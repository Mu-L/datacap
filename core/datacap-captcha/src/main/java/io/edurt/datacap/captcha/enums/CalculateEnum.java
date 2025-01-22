package io.edurt.datacap.captcha.enums;

public enum CalculateEnum
{
    ADD("+", "加"),
    SUBTRACT("-", "减"),
    MULTIPLY("*", "乘"),
    DIVIDE("÷", "除"),
    EQUAL("=", "等");

    private final String valueEn;
    private final String valueZh;

    CalculateEnum(String valueEn, String valueZh)
    {
        this.valueEn = valueEn;
        this.valueZh = valueZh;
    }

    public String getValueEn()
    {
        return valueEn;
    }

    public String getValueZh()
    {
        return valueZh;
    }
}
