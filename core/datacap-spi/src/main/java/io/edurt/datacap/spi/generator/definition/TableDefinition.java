package io.edurt.datacap.spi.generator.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.spi.model.Pagination;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.compress.utils.Sets;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableDefinition
        extends BaseDefinition
{
    private String database;
    private String name;
    private String comment;
    private String engine;
    private long autoIncrement;
    private String format;
    private long count;
    private Set<ColumnDefinition> columns = Sets.newHashSet();
    private Set<ColumnDefinition> orders = Sets.newHashSet();
    private Set<ColumnDefinition> filters = Sets.newHashSet();
    private Pagination pagination;
    private List<JsonNode> rows = Lists.newArrayList();

    private TableDefinition(String database, String name)
    {
        this.database = database;
        this.name = name;
    }

    public static TableDefinition create(String database, String name)
    {
        return new TableDefinition(database, name);
    }

    public TableDefinition comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public TableDefinition engine(String engine)
    {
        this.engine = engine;
        return this;
    }

    public TableDefinition autoIncrement(long autoIncrement)
    {
        this.autoIncrement = autoIncrement;
        return this;
    }

    public TableDefinition columns(Set<ColumnDefinition> columns)
    {
        this.columns = columns;
        return this;
    }

    public TableDefinition format(String format)
    {
        this.format = format;
        return this;
    }

    public TableDefinition count(long count)
    {
        this.count = count;
        return this;
    }

    public TableDefinition orders(Set<ColumnDefinition> orders)
    {
        this.orders = orders;
        return this;
    }

    public TableDefinition filters(Set<ColumnDefinition> filters)
    {
        this.filters = filters;
        return this;
    }

    public TableDefinition pagination(Pagination pagination)
    {
        this.pagination = pagination;
        return this;
    }

    public TableDefinition values(List<JsonNode> rows)
    {
        this.rows = rows;
        return this;
    }
}
