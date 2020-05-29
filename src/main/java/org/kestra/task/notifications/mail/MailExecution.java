package org.kestra.task.notifications.mail;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.IOUtils;
import org.kestra.core.models.annotations.Documentation;
import org.kestra.core.models.annotations.Example;
import org.kestra.core.models.executions.Execution;
import org.kestra.core.models.flows.State;
import org.kestra.core.models.tasks.VoidOutput;
import org.kestra.core.runners.RunContext;
import org.kestra.core.serializers.JacksonMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Documentation(
    description = "Task to send a mail with execution information",
    body = "Main execution information are provided in the sent mail (id, namespace, flow, state, duration, start date ...)."
)
@Example(
    title = "Send a mail notification on failed flow",
    full = true,
    code = {
        "id: mail",
        "namespace: org.kestra.tests",
        "",
        "listeners:",
        "  - conditions:",
        "      - type: org.kestra.core.models.listeners.types.ExecutionStatusCondition",
        "        in:",
        "          - FAILED",
        "    tasks:",
        "      - id: mail",
        "        type: org.kestra.task.notifications.mail.MailExecution",
        "        to: to@mail.com",
        "        from: from@mail.com",
        "        subject: This is the subject",
        "        host: nohost-mail.site",
        "        port: 465",
        "        username: user",
        "        password: pass",
        "        sessionTimeout: 1000",
        "        transportStrategy: SMTPS",
        "",
        "",
        "tasks:",
        "  - id: ok",
        "    type: org.kestra.core.tasks.debugs.Return",
        "    format: \"{{task.id}} > {{taskrun.startDate}}\""
    }
)
public class MailExecution extends MailSend {
    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String htmlTextTemplate = IOUtils.toString(
            Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("mail-template.hbs.html")),
            Charsets.UTF_8
        );

        @SuppressWarnings("unchecked")
        Execution execution = JacksonMapper.toMap((Map<String, Object>) runContext.getVariables().get("execution"), Execution.class);

        Map<String, Object> renderMap = new HashMap<>();
        renderMap.put("duration", execution.getState().humanDuration());
        renderMap.put("startDate", execution.getState().getStartDate());
        // FIXME
        renderMap.put("link", "https://todo.com");

        execution
            .findFirstByState(State.Type.FAILED)
            .ifPresentOrElse(
                taskRun -> renderMap.put("firstFailed", taskRun),
                () -> renderMap.put("firstFailed", false)
            );

        this.htmlTextContent = runContext.render(htmlTextTemplate, renderMap);

        return super.run(runContext);
    }
}
