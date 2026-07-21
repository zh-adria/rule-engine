package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.CustomFieldDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/custom-fields")
@Tag(name = "自定义字段", description = "规则条件自定义字段管理")
public class CustomFieldController {

    private final RuleEngineFacade ruleEngineFacade;

    public CustomFieldController(RuleEngineFacade ruleEngineFacade) {
        this.ruleEngineFacade = ruleEngineFacade;
    }

    @Operation(summary = "查询自定义字段列表", description = "按业务线筛选")
    @GetMapping
    public List<CustomFieldDTO> listCustomFields(
            @Parameter(description = "业务线，不传则返回全部") @RequestParam(required = false) String businessLine) {
        if (businessLine != null && !businessLine.isBlank()) {
            return ruleEngineFacade.listCustomFields(businessLine);
        }
        return ruleEngineFacade.listAllCustomFields();
    }

    @Operation(summary = "创建自定义字段")
    @PostMapping
    public CustomFieldDTO createCustomField(@Valid @RequestBody CustomFieldDTO dto) {
        return ruleEngineFacade.createCustomField(dto);
    }

    @Operation(summary = "删除自定义字段")
    @DeleteMapping("/{id}")
    public void deleteCustomField(@PathVariable Long id) {
        ruleEngineFacade.deleteCustomField(id);
    }
}
