package com.insurance.approval.adapter;

import com.insurance.approval.client.api.ApprovalFlowFacade;
import com.insurance.approval.client.dto.ApprovalDTO;
import com.insurance.approval.client.dto.ReviewApprovalCmd;
import com.insurance.approval.client.dto.SubmitApprovalCmd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalFlowFacade approvalFlowFacade;

    public ApprovalController(ApprovalFlowFacade approvalFlowFacade) {
        this.approvalFlowFacade = approvalFlowFacade;
    }

    @Operation(summary = "提交审批", description = "提交审批请求")
    @PostMapping
    public ApprovalDTO submitApproval(@Valid @RequestBody SubmitApprovalCmd cmd) {
        return approvalFlowFacade.submitApproval(cmd);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public ApprovalDTO approve(@PathVariable Long id, @Valid @RequestBody ReviewApprovalCmd cmd) {
        return approvalFlowFacade.approve(id, cmd);
    }

    @Operation(summary = "审批驳回")
    @PostMapping("/{id}/reject")
    public ApprovalDTO reject(@PathVariable Long id, @Valid @RequestBody ReviewApprovalCmd cmd) {
        return approvalFlowFacade.reject(id, cmd);
    }

    @Operation(summary = "查询审批详情")
    @GetMapping("/{id}")
    public ApprovalDTO getApproval(@PathVariable Long id) {
        return approvalFlowFacade.getApproval(id);
    }

    @Operation(summary = "查询审批列表")
    @GetMapping
    public List<ApprovalDTO> listApprovals(
            @Parameter(description = "目标类型") @RequestParam(required = false) String targetType,
            @Parameter(description = "目标ID") @RequestParam(required = false) String targetId,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return approvalFlowFacade.listApprovals(targetType, targetId, status);
    }
}
