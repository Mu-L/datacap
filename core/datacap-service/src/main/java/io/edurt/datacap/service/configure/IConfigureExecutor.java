package io.edurt.datacap.service.configure;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.common.view.EntityView;
import io.edurt.datacap.executor.common.RunProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class IConfigureExecutor
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String executor;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private RunProtocol protocol = RunProtocol.NONE;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private IConfigurePipelineType type;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<IConfigureExecutorField> fields;
}
