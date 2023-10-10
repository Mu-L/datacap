package io.edurt.datacap.service.repository.metadata;

import io.edurt.datacap.service.entity.metadata.ColumnEntity;
import io.edurt.datacap.service.entity.metadata.TableEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ColumnRepository
        extends PagingAndSortingRepository<ColumnEntity, Long>
{
    @Query("SELECT c " +
            "FROM ColumnEntity c " +
            "WHERE c.table = ?1 " +
            "ORDER BY c.position ASC ")
    List<ColumnEntity> findAllByTable(TableEntity table);

    @Query(value = "SELECT new ColumnEntity(c.id, c.name, c.createTime) " +
            "FROM ColumnEntity c " +
            "WHERE c.table = :table")
    List<ColumnEntity> findSimpleAllByTable(@Param(value = "table") TableEntity table);
}
