package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.ArchiveRuleCmd;
import com.insurance.ruleengine.client.dto.ConvertRuleRequest;
import com.insurance.ruleengine.client.dto.ConvertRuleResponse;
import com.insurance.ruleengine.client.dto.CreateRuleCmd;
import com.insurance.ruleengine.infrastructure.drools.DrlConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.ruleengine.client.dto.CreateRuleVersionCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleCmd;
import com.insurance.ruleengine.client.dto.PublishRuleCmd;
import com.insurance.ruleengine.client.dto.ReviewApprovalCmd;
import com.insurance.ruleengine.client.dto.RollbackRuleCmd;
import com.insurance.ruleengine.client.dto.RuleAuditLogDTO;
import com.insurance.ruleengine.client.dto.RuleDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionLogDTO;
import com.insurance.ruleengine.client.dto.RuleExecutionResultDTO;
import com.insurance.ruleengine.client.dto.RuleVersionDTO;
import com.insurance.ruleengine.client.dto.SubmitApprovalCmd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {
    private final RuleEngineFacade ruleEngineFacade;
    private final DrlConverter drlConverter;
    private final ObjectMapper objectMapper;

    public RuleController(RuleEngineFacade ruleEngineFacade,
                          DrlConverter drlConverter, ObjectMapper objectMapper) {
        this.ruleEngineFacade = ruleEngineFacade;
        this.drlConverter = drlConverter;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "创建规则", description = "创建规则定义")
    @PostMapping
    public RuleDTO createRule(@Valid @RequestBody CreateRuleCmd cmd) {
        return ruleEngineFacade.createRule(cmd);
    }

    @Operation(summary = "创建版本", description = "为指定规则创建新版本（草稿）")
    @PostMapping("/{ruleCode}/versions")
    public RuleDTO createVersion(@PathVariable String ruleCode, @Valid @RequestBody CreateRuleVersionCmd cmd) {
        return ruleEngineFacade.createVersion(ruleCode, cmd);
    }

    @Operation(summary = "测试规则", description = "执行规则版本并返回决策结果")
    @PostMapping("/{ruleCode}/test")
    public RuleExecutionResultDTO test(@PathVariable String ruleCode, @Valid @RequestBody ExecuteRuleCmd cmd) {
        cmd.setRuleCode(ruleCode);
        return ruleEngineFacade.testRule(ruleCode, cmd);
    }

    @Operation(summary = "提交审批", description = "提交规则版本进入审批流程")
    @PostMapping("/{ruleCode}/versions/{version}/submit-approval")
    public RuleDTO submitApproval(@PathVariable String ruleCode, @PathVariable Integer version,
                                  @Valid @RequestBody SubmitApprovalCmd cmd) {
        return ruleEngineFacade.submitApproval(ruleCode, version, cmd);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{ruleCode}/versions/{version}/approve")
    public RuleDTO approveApproval(@PathVariable String ruleCode, @PathVariable Integer version,
                                   @Valid @RequestBody ReviewApprovalCmd cmd) {
        return ruleEngineFacade.approveApproval(ruleCode, version, cmd);
    }

    @Operation(summary = "审批驳回")
    @PostMapping("/{ruleCode}/versions/{version}/reject")
    public RuleDTO rejectApproval(@PathVariable String ruleCode, @PathVariable Integer version,
                                  @Valid @RequestBody ReviewApprovalCmd cmd) {
        return ruleEngineFacade.rejectApproval(ruleCode, version, cmd);
    }

    @Operation(summary = "审批回调", description = "由审批服务调用的回调接口")
    @PostMapping("/{ruleCode}/versions/{version}/approval-callback")
    public RuleDTO approvalCallback(@PathVariable String ruleCode, @PathVariable Integer version,
                                    @RequestBody ApprovalCallbackDTO callback) {
        return ruleEngineFacade.handleApprovalCallback(
                ruleCode, version, callback.getStatus(),
                callback.getReviewedBy(), callback.getReason());
    }

    @Operation(summary = "发布规则", description = "发布规则版本（支持灰度百分比）")
    @PostMapping("/{ruleCode}/publish")
    public RuleDTO publish(@PathVariable String ruleCode, @Valid @RequestBody PublishRuleCmd cmd) {
        return ruleEngineFacade.publish(ruleCode, cmd);
    }

    @Operation(summary = "回滚规则", description = "回滚到指定版本")
    @PostMapping("/{ruleCode}/rollback")
    public RuleDTO rollback(@PathVariable String ruleCode, @Valid @RequestBody RollbackRuleCmd cmd) {
        return ruleEngineFacade.rollback(ruleCode, cmd);
    }

    @Operation(summary = "执行规则", description = "执行已发布的规则")
    @PostMapping("/execute")
    public RuleExecutionResultDTO execute(@Valid @RequestBody ExecuteRuleCmd cmd) {
        return ruleEngineFacade.execute(cmd);
    }

    @Operation(summary = "查询规则列表")
    @GetMapping
    public List<RuleDTO> listRules(@RequestParam(required = false) String category,
                                   @RequestParam(required = false) String businessLine,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String keyword) {
        return ruleEngineFacade.listRules(category, businessLine, status, keyword);
    }

    @Operation(summary = "查询规则详情")
    @GetMapping("/{ruleCode}")
    public RuleDTO getRule(@PathVariable String ruleCode) {
        return ruleEngineFacade.getRule(ruleCode);
    }

    @Operation(summary = "查询版本列表")
    @GetMapping("/{ruleCode}/versions")
    public List<RuleVersionDTO> listVersions(@PathVariable String ruleCode) {
        return ruleEngineFacade.listVersions(ruleCode);
    }

    @Operation(summary = "查询执行日志")
    @GetMapping("/{ruleCode}/executions")
    public List<RuleExecutionLogDTO> listExecutions(@PathVariable String ruleCode) {
        return ruleEngineFacade.listExecutions(ruleCode);
    }

    @Operation(summary = "查询审计日志")
    @GetMapping("/{ruleCode}/audits")
    public List<RuleAuditLogDTO> listAudits(@PathVariable String ruleCode) {
        return ruleEngineFacade.listAudits(ruleCode);
    }

    @Operation(summary = "归档规则")
    @PostMapping("/{ruleCode}/archive")
    public RuleDTO archive(@PathVariable String ruleCode, @Valid @RequestBody ArchiveRuleCmd cmd) {
        return ruleEngineFacade.archive(ruleCode, cmd);
    }

    @Operation(summary = "DRL 与可视化双向转换")
    @PostMapping("/convert")
    public ConvertRuleResponse convert(@RequestBody ConvertRuleRequest request) {
        ConvertRuleResponse response = new ConvertRuleResponse();
        if (request.getVisualModel() != null) {
            response.setDrl(drlConverter.visualModelToDrl(objectMapper.valueToTree(request.getVisualModel())));
        } else if (request.getDrl() != null && !request.getDrl().isBlank()) {
            response.setVisualModel(drlConverter.drlToVisualModel(request.getDrl()));
        } else {
            throw new IllegalArgumentException("Either visualModel or drl must be supplied");
        }
        return response;
    }

    public static class ApprovalCallbackDTO {
        private String status;
        private String reviewedBy;
        private String reason;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
