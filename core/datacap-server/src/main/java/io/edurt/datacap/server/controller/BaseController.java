package io.edurt.datacap.server.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.common.utils.CodeUtils;
import io.edurt.datacap.common.view.EntityView;
import io.edurt.datacap.service.annotation.DynamicJsonView;
import io.edurt.datacap.service.body.FilterBody;
import io.edurt.datacap.service.entity.BaseEntity;
import io.edurt.datacap.service.entity.PageEntity;
import io.edurt.datacap.service.repository.BaseRepository;
import io.edurt.datacap.service.service.BaseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;

public abstract class BaseController<T extends BaseEntity>
        implements Serializable
{
    private final BaseRepository<T, Long> repository;
    private final BaseService<T> service;

    protected BaseController(BaseRepository<T, Long> repository, BaseService<T> service)
    {
        this.repository = repository;
        this.service = service;
    }

    /**
     * Get data based on pagination
     */
    @PostMapping(value = "list")
    @DynamicJsonView
    public CommonResponse<PageEntity<T>> list(@RequestBody FilterBody filter)
    {
        return service.getAll(repository, filter);
    }

    /**
     * Create new resource
     */
    @PostMapping
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<T> create(@RequestBody T configure)
    {
        configure.setCode(CodeUtils.generateCode(false));
        return service.saveOrUpdate(repository, configure);
    }

    /**
     * Update existing resource
     */
    @PutMapping
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<T> update(@RequestBody T configure)
    {
        return repository.findByCode(configure.getCode())
                .map(entity -> {
                    configure.setId(entity.getId());
                    configure.setCode(entity.getCode());
                    return service.saveOrUpdate(repository, configure);
                })
                .orElseGet(() -> CommonResponse.failure("Resource [ " + configure.getCode() + " ] not found"));
    }

    @Deprecated
    @DeleteMapping
    public CommonResponse<String> delete(@RequestParam(value = "code") String code)
    {
        return service.deleteByCode(repository, code);
    }

    @DeleteMapping(value = "{code}")
    public CommonResponse<String> deleteForPath(@PathVariable(value = "code") String code)
    {
        return service.deleteByCode(repository, code);
    }

    /**
     * Retrieves information for a specific path.
     *
     * @param id the identifier of the path
     * @return the information for the specified path
     */
    @GetMapping(value = "{id}")
    public CommonResponse<T> getInfoForPath(@PathVariable(value = "id") Long id)
    {
        return service.getById(repository, id);
    }

    @GetMapping(value = "info/{code}")
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<T> getByCode(@PathVariable(value = "code") String code)
    {
        return service.getByCode(repository, code);
    }
}
