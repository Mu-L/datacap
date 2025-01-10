package io.edurt.datacap.spi.generator.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.edurt.datacap.spi.generator.DataType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColumnDefinition
        extends BaseDefinition
{
    private String name;
    private DataType type;
    private boolean nullable;
    private Object defaultValue;
    private String comment;
}
