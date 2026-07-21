package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.TemplateFacade;
import com.insurance.ruleengine.client.dto.RuleTemplateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private final TemplateFacade templateFacade;

    public TemplateController(TemplateFacade templateFacade) {
        this.templateFacade = templateFacade;
    }

    @Operation(summary = "列出所有模板", description = "返回按 sortOrder 排序的模板列表")
    @GetMapping
    public List<RuleTemplateDTO> listAll() {
        return templateFacade.findAll();
    }

    @Operation(summary = "按分类筛选", description = "按 category 参数筛选模板")
    @GetMapping(params = "category")
    public List<RuleTemplateDTO> listByCategory(
            @Parameter(description = "分类: UNDERWRITING / RISK_CONTROL / PRICING / CLAIM") @RequestParam String category) {
        return templateFacade.findByCategory(category);
    }

    @Operation(summary = "按编码查询", description = "根据 templateCode 查询单个模板")
    @GetMapping("/{templateCode}")
    public ResponseEntity<RuleTemplateDTO> getByCode(
            @Parameter(description = "模板编码") @PathVariable String templateCode) {
        return templateFacade.findByCode(templateCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "创建模板", description = "管理员创建新模板")
    @ApiResponse(responseCode = "201", description = "创建成功")
    @PostMapping
    public ResponseEntity<RuleTemplateDTO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模板信息", required = true,
                    content = @Content(schema = @Schema(implementation = RuleTemplateDTO.class))) @RequestBody RuleTemplateDTO template) {
        RuleTemplateDTO created = templateFacade.create(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "更新模板", description = "按 templateCode 更新模板")
    @PutMapping("/{templateCode}")
    public ResponseEntity<RuleTemplateDTO> update(
            @Parameter(description = "模板编码") @PathVariable String templateCode,
            @RequestBody RuleTemplateDTO template) {
        return ResponseEntity.ok(templateFacade.update(templateCode, template));
    }

    @Operation(summary = "删除模板", description = "按 templateCode 删除模板")
    @DeleteMapping("/{templateCode}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "模板编码") @PathVariable String templateCode) {
        templateFacade.delete(templateCode);
        return ResponseEntity.noContent().build();
    }
}
