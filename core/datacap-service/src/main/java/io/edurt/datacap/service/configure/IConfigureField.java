package io.edurt.datacap.service.configure;

import com.fasterxml.jackson.annotation.JsonView;
import io.edurt.datacap.common.view.EntityView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class IConfigureField
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private IConfigureFieldName field; // The field name of the tag configuration

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private IConfigureFieldType type;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean required;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long min;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long max;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Object value;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean disabled;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private IConfigureFieldGroup group = IConfigureFieldGroup.configure;
}
