package io.edurt.datacap.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.sql.node.element.TableElement;
import io.edurt.datacap.sql.node.option.TableOption;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class CreateTableStatement
        extends SQLStatement
{
    private final String tableName;
    private final boolean temporary;
    private final boolean ifNotExists;
    private final List<TableElement> columns;
    private final List<TableOption> options;

    public CreateTableStatement(
            String tableName,
            boolean temporary,
            boolean ifNotExists,
            List<TableElement> columns,
            List<TableOption> options
    )
    {
        super(StatementType.CREATE);
        this.tableName = tableName;
        this.temporary = temporary;
        this.ifNotExists = ifNotExists;
        this.columns = columns;
        this.options = options;
    }
}
